package uk.nhs.wales.gppractice.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FhirConfig {

    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4();
    }

    @Bean
    public FhirValidator fhirValidator(FhirContext fhirContext) {
        // Build validation support chain
        ValidationSupportChain supportChain = new ValidationSupportChain();
        supportChain.addValidationSupport(new DefaultProfileValidationSupport(fhirContext));
        supportChain.addValidationSupport(new InMemoryTerminologyServerValidationSupport(fhirContext));
        supportChain.addValidationSupport(new CommonCodeSystemsTerminologyService(fhirContext));

        // Create validator and register instance validator
        FhirValidator validator = fhirContext.newValidator();
        FhirInstanceValidator instanceValidator = new FhirInstanceValidator(supportChain);
        validator.registerValidatorModule(instanceValidator);

        return validator;
    }
}
