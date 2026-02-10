package com.ozonehis.imisconnect.service.fhir;

import com.ozonehis.imisconnect.dto.ClaimResponse;
import com.ozonehis.imisconnect.dto.EligibilityRequest;
import com.ozonehis.imisconnect.dto.EligibilityResponse;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ResourceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class FhirService {

    private final IGenericClient fhirClient;
    private final FhirContext fhirContext;

    public FhirService(@Value("${openimis.baseUrl}") String openImisBaseUrl) {
        this.fhirContext = FhirContext.forR4();
        this.fhirClient = fhirContext.newRestfulGenericClient(openImisBaseUrl);
    }

    public EligibilityResponse checkEligibility(EligibilityRequest request) {
        try {
            org.hl7.fhir.r4.model.Bundle patientBundle = fhirClient.search()
                    .forResource(Patient.class)
                    .where(Patient.IDENTIFIER.exactly().identifier(request.getInsureeId()))
                    .returnBundle(org.hl7.fhir.r4.model.Bundle.class)
                    .execute();

            if (patientBundle.getEntry().isEmpty()) {
                throw new RuntimeException("Patient not found for ID: " + request.getInsureeId());
            }

            Patient patient = (Patient) patientBundle.getEntryFirstRep().getResource();

            org.hl7.fhir.r4.model.Bundle coverageBundle = fhirClient.search()
                    .forResource(Coverage.class)
                    .where(Coverage.BENEFICIARY.hasId(patient.getIdElement().getIdPart()))
                    .returnBundle(org.hl7.fhir.r4.model.Bundle.class)
                    .execute();

            if (coverageBundle.getEntry().isEmpty()) {
                throw new RuntimeException("Coverage not found for patient: " + request.getInsureeId());
            }

            Coverage coverage = (Coverage) coverageBundle.getEntryFirstRep().getResource();
            String band = resolveBand(coverage);

            return EligibilityResponse.builder()
                    .valid(true)
                    .plan(coverage.hasType() ? coverage.getType().getText() : "NHIA-Basic")
                    .band(band)
                    .allowedBands(getAllowedBands(band))
                    .expiryDate(LocalDate.now().plusYears(1))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("FHIR eligibility check failed: " + e.getMessage(), e);
        }
    }

    public ClaimResponse submitClaim(Claim claim) {
        log.info("Submitting FHIR claim for patient: {}", claim.getPatient().getReference());
        try {
            Claim response = (Claim) fhirClient.create()
                    .resource(claim)
                    .execute()
                    .getResource();

            log.info("Claim submitted successfully. IMIS ID: {}", response.getIdElement().getIdPart());
            return ClaimResponse.builder()
                    .claimId(response.getIdElement().getIdPart())
                    .status("submitted")
                    .message("Claim submitted successfully")
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("FHIR claim submission failed", e);
        }
    }

    public ClaimResponse getClaimStatus(String claimId) {
        log.debug("Fetching status for claim: {}", claimId);
        try {
            Claim claim = fhirClient.read()
                    .resource(Claim.class)
                    .withId(claimId)
                    .execute();

            return ClaimResponse.builder()
                    .claimId(claimId)
                    .status(claim.getStatus().toCode())
                    .message("Claim status retrieved")
                    .build();
        } catch (Exception e) {
            log.error("FHIR claim status check failed for ID {}: {}", claimId, e.getMessage());
            throw new RuntimeException("FHIR claim status check failed", e);
        }
    }

    private String resolveBand(Coverage coverage) {
        // Look for band extension or use type-based logic
        Extension bandExt = coverage.getExtensionByUrl("http://openimis.org/fhir/StructureDefinition/coverage-band");
        if (bandExt != null && bandExt.hasValue()) {
            return bandExt.getValue().toString();
        }

        // Fallback: Default to D if not specified, or use some logic
        return "D";
    }

    private List<String> getAllowedBands(String band) {
        return switch (band) {
            case "A" -> List.of("A", "B", "C", "D");
            case "B" -> List.of("B", "C", "D");
            case "C" -> List.of("C", "D");
            default -> List.of("D");
        };
    }
}
