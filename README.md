# Architecture Design Overview: GP Practice Document Submission API

## Document Details

**Document author**: Mark Evans

**Document version**: 1.0

**Status**: Draft

**Approved by**: ...

**Date approved**: ...

**Review date**: 2026/10/22

**Template version**: 1.2

### Revision History

| Date       | Version | Author   | Revision Summary |
| ---------- | :-----: | -------- | ---------------- |
| 2025-10-22 |  v1.0   |  Mark Evans | Initial draft |

## 1. Introduction and Goals

This document describes the architecture for the GP Practice Document Submission API — a service that accepts FHIR R4 Bundles (type=transaction) containing clinical documents (DocumentReference + Binary + Patient + Encounter + Practitioner + Organisation) and delivers them to GP practice systems.

### 1.1 Requirements Overview

- Accept FHIR R4 Bundles (transaction)
- Validate syntactic and semantic correctness of FHIR resources
- Ensure the associated Binary resource is stored reliably
- Authenticate and authorize senders (API Gateway)
- Provide clear OperationOutcome responses for success/failure

### 1.2 Quality Goals

- Security: Confidentiality, integrity and availability of PHI
- Performance: Typical bundle processing in <2s for validation path (non-blocking storage)
- Scalability: Horizontal scaling on stateless API containers
- Observability: Tracing, metrics and structured logging for each submission

### 1.3 Stakeholders

| Role/Name | Contact | Expectations |
| --------- | ------- | ------------ |
| GP Practices |  | Receive validated clinical documents reliably |
| Hospital EHRs | | Ability to submit discharge summaries |
| Platform Operators |  | Secure, observable and resilient service |

## 2. Architecture Constraints

- Must accept FHIR R4 (JSON) Bundles as input.
- Must run in a typical cloud environment (managed Kubernetes or PaaS).
- Must store binary clinical documents in an object store (S3-compatible).
- Must integrate with existing identity provider (jwt).

## 3. Context and Scope

The system boundary is the GP Practice Document Submission API. It receives FHIR Bundles from external senders (Hospital systems), validates them, persists metadata, stores binary files in object storage, and forwards or makes available the clinical documents to the GP Practice System.

### 3.1 Business Context

- Replace ad-hoc file transfer with a validated, auditable API for clinical document submission.
- Provide consistent OperationOutcome responses aligned with NHS FHIR practices.

### 3.2 Technical Context

- External senders: Hospital EHRs, middleware
- API Gateway: TLS termination, authentication, rate-limiting
- GP Docs API: Spring Boot application (controller + services)
- Backend: Postgres for metadata,  object store for Binary resources


## 4. Solution Strategy

- Use a layered Spring Boot application with Controller -> Service -> Repository pattern.
- Use HAPI FHIR libraries for parsing and validating FHIR R4 resources (enabled in FhirConfig.java).
- Keep API stateless so it can scale horizontally; persist state to Postgres and object store.
- Push logs and metrics to central observability stack (Prometheus/Grafana + ELK)

## 5. Building Block View

### 5.1 C4 Level 1 View

![C4 Level 1](assets/c4-1.png "C4 Level 1")

### 5.2 C4 Level 2 View

![C4 Level 2](assets/c4-2.png "C4 Level 2")

### 5.3 C4 Level 3 View

![C4 Level 3](assets/c4-3.png "C4 Level 3")

## 6. Runtime View

Important runtime scenarios are captured as sequence diagrams below.

### Sequence: Submit Document Bundle

![submit](assets/seq-1.png "Submit Document")

### Sequence: Error Handling (invalid bundle)

![error](assets/seq-2.png "Invalid Document")

## 7. Deployment view

Generic cloud deployment:

- API Gateway (managed) — TLS termination and OAuth2/OIDC authentication.
- Kubernetes cluster (or container service) running stateless GP Docs API pods behind an autoscaling group.
- Postgres (managed) for metadata and audit records.
- Object storage for Binary resources.
- Optional message broker (e.g., SNS/SQS or Kafka) for async delivery to GP systems.
- Observability stack: Prometheus, Grafana, ELK/Cloud Logging.

## 8. Crosscutting Concepts

### 8.1 Design Patterns

- Layered architecture (Controller -> Service -> Repository)
- Validation-as-a-service (FhirValidationService)

### 8.2 Security

- Transport: TLS 1.2+ enforced at API Gateway.
- Authentication/Authorization: OAuth2/OIDC; clients obtain tokens; scopes limit access.
- Input validation: strict FHIR validation and size limits for Binary resources.
- Data protection: store PHI encrypted at rest (DB and object store) and encrypted in transit.
- Audit: immutable audit trail for submissions (write-ahead logs/audit table).
- Secrets: use cloud secret manager for DB credentials and encryption keys.

### 8.3 Scalability

- Stateless API nodes behind load balancer enable horizontal scaling.
- Postgres scaled vertically or via read replicas for read-heavy workloads.
- Object store offloads large binary sizes and scales independently.

### 8.4 Resilience

- Retries with exponential backoff for transient errors (e.g., storage timeouts).
- Circuit breakers on downstream calls if integrated with external GP endpoints.
- Dead-letter queue for failed async deliveries.

### 8.5 Observability

- Structured logging (JSON) with correlation IDs per submission.
- Metrics: request rate, validation success/failure, processing latency.
- Tracing (W3C TraceContext) to follow submission across services.

### 8.6 Regulatory & Compliance

- Treat submitted data as sensitive health data; follow regional regulations (e.g., UK Data Protection Act / GDPR).
- Data retention policies for audit logs and binaries must be defined.

## 9. Architecture Decisions

- Use HAPI FHIR for validation and parsing (as implemented in FhirConfig.java).
- Keep API stateless; use cloud-managed DB and object storage.

### 9.1 New ADRs

| ID | Impact |
| -- | ------ |
| ADR-001 | Use HAPI FHIR for validation and resource handling |

## 10. Quality Requirements

- Availability: 99.9% SLA for core submission paths.
- Latency: median validation latency < 2s.
- Throughput: scale to handle bursts of submissions from multiple hospital sources.

## 11. Risks and Technical Debt

### 11.1 Risks

| ID | Impact | Mitigation / Plan | Owner |
| -- | ------ | ----------------- | ----- |
| R-001 | High | Sensitive PHI exposure if TLS/auth misconfigured | Platform Ops |
| R-002 | Medium | Malformed FHIR bundles cause processing errors | Improve validation & clearer OperationOutcome messages |

### 11.2 Technical Debt

| ID | Impact | Mitigation / Plan | Owner |
| -- | ------ | ----------------- | ----- |
| TD-001 | Medium | No async delivery pipeline implemented | Introduce message broker for retries and durability |

## 12. Glossary

| Term | Definition |
| ---- | ---------- |
| FHIR | Fast Healthcare Interoperability Resources (R4) |
| Bundle | FHIR Bundle resource (transaction) |
| DocumentReference | FHIR resource pointing to clinical document |
| Binary | FHIR resource containing binary payload (PDF) |

