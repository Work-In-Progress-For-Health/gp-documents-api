# GP Documents API Exemplar
This is a Java spring boot application which implements the basic functionality to allow 3rd Parties to send documents to GPs for specific patients using gp practice codes.  The API is RESTful and based on FHIR R4.  A sample message for submission might contain several bundles of information, including Patient, Practioner, DocumentReference+Binary.
```json
{
  "resourceType": "Bundle",
  "type": "transaction",
  "entry": [
    {
      "fullUrl": "urn:uuid:9c2d9a2b-3b27-4e4f-9f10-c1a91e2b3a9f",
      "resource": {
        "resourceType": "Patient",
        "id": "9c2d9a2b-3b27-4e4f-9f10-c1a91e2b3a9f",
        "name": [
          {
            "family": "Smith",
            "given": ["John"]
          }
        ],
        "gender": "male",
        "birthDate": "1980-02-17",
        "identifier": [
          {
            "system": "https://fhir.nhs.uk/Id/nhs-number",
            "value": "3478526985",
            "extension": [
              {
                "url": "https://fhir.hl7.org.uk/StructureDefinition/Extension-UKCore-NHSNumberVerificationStatus",
                "valueCodeableConcept": {
                  "coding": [
                    {
                      "system": "https://fhir.hl7.org.uk/CodeSystem/UKCore-NHSNumberVerificationStatus",
                      "code": "number-present-and-verified",
                      "display": "Number present and verified"
                    }
                  ]
                }
              }
            ]
          }
        ],
        "address": [
          {
            "use": "home",
            "line": ["10 High Street"],
            "city": "Leeds",
            "postalCode": "LS1 4AB",
            "country": "GB"
          }
        ]
      },
      "request": {
        "method": "POST",
        "url": "Patient"
      }
    },
    {
      "fullUrl": "urn:uuid:edb28f5f-f312-4b91-9a89-64b20d9e1c77",
      "resource": {
        "resourceType": "Practitioner",
        "id": "edb28f5f-f312-4b91-9a89-64b20d9e1c77",
        "name": [
          {
            "prefix": ["Dr"],
            "family": "Jones",
            "given": ["Sarah"]
          }
        ]
      },
      "request": {
        "method": "POST",
        "url": "Practitioner"
      }
    },
    {
      "fullUrl": "urn:uuid:a203afcd-3a7d-4b27-bc0f-3c86efb6e0d2",
      "resource": {
        "resourceType": "Organization",
        "id": "a203afcd-3a7d-4b27-bc0f-3c86efb6e0d2",
        "name": "HealthTech GP Practice"
      },
      "request": {
        "method": "POST",
        "url": "Organization"
      }
    },
    {
      "fullUrl": "urn:uuid:7b6b4713-02a5-45da-bbd4-d59cf87a56ee",
      "resource": {
        "resourceType": "Encounter",
        "id": "7b6b4713-02a5-45da-bbd4-d59cf87a56ee",
        "status": "finished",
        "class": {
          "system": "http://terminology.hl7.org/CodeSystem/v3-ActCode",
          "code": "IMP",
          "display": "inpatient encounter"
        },
        "subject": {
          "reference": "urn:uuid:9c2d9a2b-3b27-4e4f-9f10-c1a91e2b3a9f"
        },
        "serviceProvider": {
          "reference": "urn:uuid:a203afcd-3a7d-4b27-bc0f-3c86efb6e0d2",
          "display": "GP Practice"
        },
        "serviceType": {
          "coding": [
            {
              "system": "https://fhir.hl7.org.uk/CodeSystem/UKCore-ServiceType",
              "code": "300",
              "display": "General Internal Medicine"
            }
          ]
        },
        "reasonCode": [
          {
            "text": "Clinical circumstance leading to admission..."
          }
        ],
        "period": {
          "start": "2025-10-15T10:33:00+00:00",
          "end": "2025-10-16T00:00:00+00:00"
        },
        "hospitalization": {
          "admitSource": {
            "coding": [
              {
                "system": "https://fhir.hl7.org.uk/CodeSystem/UKCore-AdmissionMethod",
                "code": "11",
                "display": "Emergency admission"
              }
            ]
          },
          "dischargeDisposition": {
            "text": "Unknown"
          },
          "destination": {
            "display": "Unknown"
          }
        },
        "location": [
          {
            "location": {
              "display": "Ward 00001"
            },
            "period": {
              "end": "2025-10-16T00:00:00+00:00"
            }
          }
        ]
      },
      "request": {
        "method": "POST",
        "url": "Encounter"
      }
    },
    {
      "fullUrl": "urn:uuid:5d5b9f92-97e3-4a24-bb56-7f8a6a4b5f8d",
      "resource": {
        "resourceType": "Binary",
        "id": "5d5b9f92-97e3-4a24-bb56-7f8a6a4b5f8d",
        "contentType": "application/pdf",
        "data": "SGVsbG8sIFdvcmxkIQ=="
      },
      "request": {
        "method": "POST",
        "url": "Binary"
      }
    },
    {
      "fullUrl": "urn:uuid:8faef11d-9c9a-4f5f-930b-f69e84c29cfb",
      "resource": {
        "resourceType": "DocumentReference",
        "id": "8faef11d-9c9a-4f5f-930b-f69e84c29cfb",
        "status": "current",
        "type": {
          "coding": [
            {
              "system": "http://snomed.info/sct",
              "code": "373942005",
              "display": "Discharge summary"
            }
          ],
          "text": "Discharge summary"
        },
        "subject": {
          "reference": "urn:uuid:9c2d9a2b-3b27-4e4f-9f10-c1a91e2b3a9f"
        },
        "author": [
          {
            "reference": "urn:uuid:edb28f5f-f312-4b91-9a89-64b20d9e1c77"
          }
        ],
        "custodian": {
          "reference": "urn:uuid:a203afcd-3a7d-4b27-bc0f-3c86efb6e0d2"
        },
        "content": [
          {
            "attachment": {
              "contentType": "application/pdf",
              "language": "en",
              "url": "urn:uuid:5d5b9f92-97e3-4a24-bb56-7f8a6a4b5f8d"
            }
          }
        ]
      },
      "request": {
        "method": "POST",
        "url": "DocumentReference"
      }
    }
  ]
}
```
