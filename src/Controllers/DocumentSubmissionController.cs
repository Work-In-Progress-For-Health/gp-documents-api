using Hl7.Fhir.Model;
using Hl7.Fhir.Serialization;
using Hl7.Fhir.Validation;
using Microsoft.AspNetCore.Mvc;
using Uk.HealthTechWales.GpPractice.Models;
using Uk.HealthTechWales.GpPractice.Services;

namespace Uk.HealthTechWales.GpPractice.Controllers;

[ApiController]
[Route("api/v1/gp-practice/{gpPracticeId}/documents")]
public class DocumentSubmissionController : ControllerBase
{
    private readonly ILogger<DocumentSubmissionController> _logger;
    private readonly IGpPracticeService _gpPracticeService;
    private readonly Validator _fhirValidator;
    private readonly IDocumentProcessingService _documentProcessingService;
    private readonly FhirJsonParser _fhirParser;

    private static readonly string[] RequiredResources =
    {
        "DocumentReference", "Binary", "Patient", "Encounter", "Practitioner", "Organization"
    };

    public DocumentSubmissionController(
        IGpPracticeService gpPracticeService,
        Validator fhirValidator,
        IDocumentProcessingService documentProcessingService,
        ILogger<DocumentSubmissionController> logger)
    {
        _logger = logger;
        _gpPracticeService = gpPracticeService;
        _fhirValidator = fhirValidator;
        _documentProcessingService = documentProcessingService;
        _fhirParser = new FhirJsonParser();
    }

    [HttpPost]
    [Consumes("application/fhir+json")]
    [Produces("application/fhir+json")]
    public async Task<ActionResult<FhirOperationOutcome>> SubmitDocumentBundle(
        [FromRoute] string gpPracticeId,
        [FromBody] string bundleJson)
    {
        // Validate GP Practice exists
        var gpExists = await _gpPracticeService.IsValidPracticeAsync(gpPracticeId);
        if (!gpExists)
        {
            return InvalidPractice($"Invalid GP practice ID: {gpPracticeId}");
        }

        // Parse the incoming JSON to a FHIR Bundle
        Bundle bundle;
        try
        {
            bundle = _fhirParser.Parse<Bundle>(bundleJson);
        }
        catch (Exception ex)
        {
            return BadRequest($"Invalid FHIR Bundle JSON: {ex.Message}");
        }

        if (bundle?.Type == null || bundle.Entry == null || !bundle.Entry.Any())
        {
            return BadRequest("Invalid or missing FHIR Bundle structure.");
        }

        // Extract Binary resource
        var binaryResource = bundle.Entry
            .Select(e => e.Resource)
            .OfType<Binary>()
            .FirstOrDefault();

        if (binaryResource == null)
        {
            return BadRequest("Missing Binary resource in bundle.");
        }

        if (binaryResource.Data == null || binaryResource.Data.Length == 0)
        {
            return BadRequest("Binary resource contains no data.");
        }

        // Convert binary data to base64 string
        var base64Data = Convert.ToBase64String(binaryResource.Data);

        // Validate NHS Number is present and verified
        var nhsNumberValidation = ValidateNHSNumber(bundle);
        if (nhsNumberValidation != null)
        {
            return BadRequest(nhsNumberValidation);
        }

        // Validate the bundle
        var validationResult = _fhirValidator.Validate(bundle);
        if (!validationResult.IsSuccessful)
        {
            var outcome = validationResult.ToOperationOutcome();
            return StatusCode(400, FhirOperationOutcome.FromOperationOutcome(outcome));
        }

        // Check for required resources
        var resourceTypes = bundle.Entry
            .Where(e => e.Resource != null)
            .Select(e => e.Resource.TypeName)
            .ToList();

        foreach (var required in RequiredResources)
        {
            if (!resourceTypes.Contains(required))
            {
                return BadRequest($"Missing mandatory FHIR resource: {required}");
            }
        }

        // Process document based on configured mode
        var result = await _documentProcessingService.ProcessDocumentAsync(
            gpPracticeId, bundle, bundleJson, binaryResource, base64Data);

        if (!result.Success)
        {
            if (result.MalwareDetected)
            {
                return MalwareDetected(result.ErrorMessage!);
            }

            return BadRequest(result.ErrorMessage!);
        }

        var successOutcome = FhirOperationOutcome.Success(
            $"Bundle successfully processed and document accepted for GP practice {gpPracticeId}");

        return StatusCode(201, successOutcome);
    }

    private ActionResult<FhirOperationOutcome> BadRequest(string message)
    {
        var outcome = FhirOperationOutcome.Error(message);
        return StatusCode(400, outcome);
    }

    private ActionResult<FhirOperationOutcome> InvalidPractice(string message)
    {
        var outcome = FhirOperationOutcome.Error(message);
        return StatusCode(404, outcome);
    }

    private ActionResult<FhirOperationOutcome> MalwareDetected(string message)
    {
        var outcome = FhirOperationOutcome.Error(message);
        return StatusCode(422, outcome);
    }

    private string? ValidateNHSNumber(Bundle bundle)
    {
        // Find Patient resource
        var patientResource = bundle.Entry
            .Select(e => e.Resource)
            .OfType<Patient>()
            .FirstOrDefault();

        if (patientResource == null)
        {
            return "Patient resource not found in bundle";
        }

        // Check for NHS Number identifier
        var nhsNumberIdentifier = patientResource.Identifier
            .FirstOrDefault(id => id.System == "https://fhir.nhs.uk/Id/nhs-number");

        if (nhsNumberIdentifier == null)
        {
            return "NHS Number identifier not found in Patient resource";
        }

        // Check for NHS Number value
        if (string.IsNullOrEmpty(nhsNumberIdentifier.Value))
        {
            return "NHS Number value is missing";
        }

        // Check for verification status extension
        var verificationExtension = nhsNumberIdentifier.Extension
            .FirstOrDefault(ext => ext.Url == "https://fhir.hl7.org.uk/StructureDefinition/Extension-UKCore-NHSNumberVerificationStatus");

        if (verificationExtension == null)
        {
            return "NHS Number verification status extension not found";
        }

        // Check that extension has a CodeableConcept value
        if (verificationExtension.Value is not CodeableConcept codeableConcept)
        {
            return "NHS Number verification status extension does not contain a CodeableConcept";
        }

        // Check for coding
        if (codeableConcept.Coding == null || !codeableConcept.Coding.Any())
        {
            return "NHS Number verification status does not contain coding";
        }

        // Check that at least one coding has the correct system and code
        var hasValidVerification = codeableConcept.Coding.Any(coding =>
            coding.System == "https://fhir.hl7.org.uk/CodeSystem/UKCore-NHSNumberVerificationStatus" &&
            (coding.Code == "01" || coding.Code == "number-present-and-verified")
        );

        if (!hasValidVerification)
        {
            return "NHS Number is not verified. Verification status must be '01' or 'number-present-and-verified'";
        }

        return null; // Validation passed
    }
}
