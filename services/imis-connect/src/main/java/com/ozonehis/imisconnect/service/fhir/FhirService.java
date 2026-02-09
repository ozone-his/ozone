package com.ozonehis.imisconnect.service.fhir;

import com.ozonehis.imisconnect.dto.ClaimResponse;
import com.ozonehis.imisconnect.dto.EligibilityRequest;
import com.ozonehis.imisconnect.dto.EligibilityResponse;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ResourceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;

import java.time.LocalDate;
import java.util.List;

@Service
public class FhirService {

    private final IGenericClient fhirClient;
    private final FhirContext fhirContext;

    @Value("${openimis.baseUrl}")
    private String openImisBaseUrl;

    public FhirService() {
        this.fhirContext = FhirContext.forR4();
        this.fhirClient = fhirContext.newRestfulGenericClient(openImisBaseUrl);
    }

    public EligibilityResponse checkEligibility(EligibilityRequest request) {
        try {
            Patient patient = fhirClient.search()
                    .forResource(Patient.class)
                    .where(Patient.IDENTIFIER.exactly().identifier(request.getInsureeId()))
                    .returnBundle(org.hl7.fhir.r4.model.Bundle.class)
                    .execute()
                    .getEntryFirstRep()
                    .getResource()
                    .castToPatient(null);

            Coverage coverage = fhirClient.search()
                    .forResource(Coverage.class)
                    .where(Coverage.BENEFICIARY.hasId(patient.getIdElement().getIdPart()))
                    .returnBundle(org.hl7.fhir.r4.model.Bundle.class)
                    .execute()
                    .getEntryFirstRep()
                    .getResource()
                    .castToCoverage(null);

            return EligibilityResponse.builder()
                    .valid(true)
                    .plan(coverage.getPlan())
                    .band("D")
                    .allowedBands(List.of("D"))
                    .expiryDate(LocalDate.parse(coverage.getPeriod().getEnd().toString()))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("FHIR eligibility check failed", e);
        }
    }

    public ClaimResponse submitClaim(Claim claim) {
        try {
            Claim response = fhirClient.create()
                    .resource(claim)
                    .execute()
                    .getResource()
                    .castToClaim(null);

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
        try {
            Claim claim = fhirClient.read()
                    .resource(Claim.class)
                    .withId(claimId)
                    .execute();

            return ClaimResponse.builder()
                    .claimId(claimId)
                    .status(claim.getStatus().toString())
                    .message("Claim status retrieved")
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("FHIR claim status check failed", e);
        }
    }
}
