package com.ozonehis.imisconnect.service.graphql;

import com.ozonehis.imisconnect.dto.ClaimResponse;
import com.ozonehis.imisconnect.dto.EligibilityRequest;
import com.ozonehis.imisconnect.dto.EligibilityResponse;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Claim;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GraphQLService {

    private final HttpGraphQlClient graphQlClient;

    public GraphQLService(@Value("${openimis.baseUrl}") String baseUrl) {
        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl + "/graphql")
                .build();
        this.graphQlClient = HttpGraphQlClient.builder(webClient).build();
    }

    public EligibilityResponse checkEligibility(EligibilityRequest request) {
        // In a real implementation, this would call openIMIS GraphQL API
        // For now, we return a mock that follows the Nigeria HMO band rules
        return EligibilityResponse.builder()
                .valid(true)
                .plan("NHIA-Basic")
                .band("D")
                .allowedBands(List.of("D"))
                .expiryDate(LocalDate.now().plusYears(1))
                .build();
    }

    public ClaimResponse submitClaim(Claim claim) {
        log.info("Submitting claim via GraphQL fallback");

        String query = """
            mutation SubmitClaim($claim: ClaimInput!) {
              submitClaim(input: $claim) {
                clientMutationId
                claim {
                  id
                  status
                }
              }
            }
        """;

        try {
            // Simplified example of calling openIMIS GraphQL
            Map<String, Object> response = graphQlClient.document(query)
                    .variable("claim", Map.of("patientId", claim.getPatient().getReference()))
                    .retrieve("submitClaim")
                    .toEntity(Map.class)
                    .block();

            return ClaimResponse.builder()
                    .claimId("IMIS-GQL-" + System.currentTimeMillis())
                    .status("submitted")
                    .message("Claim submitted successfully via GraphQL")
                    .build();
        } catch (Exception e) {
            log.error("GraphQL claim submission failed: {}", e.getMessage());
            throw new RuntimeException("GraphQL claim submission failed", e);
        }
    }

    public ClaimResponse getClaimStatus(String claimId) {
        return ClaimResponse.builder()
                .claimId(claimId)
                .status("processed")
                .message("Claim processed successfully")
                .build();
    }
}
