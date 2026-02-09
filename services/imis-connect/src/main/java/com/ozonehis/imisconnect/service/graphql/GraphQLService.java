package com.ozonehis.imisconnect.service.graphql;

import com.ozonehis.imisconnect.dto.ClaimResponse;
import com.ozonehis.imisconnect.dto.EligibilityRequest;
import com.ozonehis.imisconnect.dto.EligibilityResponse;
import org.hl7.fhir.r4.model.Claim;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class GraphQLService {

    public EligibilityResponse checkEligibility(EligibilityRequest request) {
        return EligibilityResponse.builder()
                .valid(true)
                .plan("NHIA-Basic")
                .band("D")
                .allowedBands(List.of("D"))
                .expiryDate(LocalDate.now().plusYears(1))
                .build();
    }

    public ClaimResponse submitClaim(Claim claim) {
        return ClaimResponse.builder()
                .claimId("IMIS-" + System.currentTimeMillis())
                .status("submitted")
                .message("Claim submitted successfully")
                .build();
    }

    public ClaimResponse getClaimStatus(String claimId) {
        return ClaimResponse.builder()
                .claimId(claimId)
                .status("processed")
                .message("Claim processed successfully")
                .build();
    }
}
