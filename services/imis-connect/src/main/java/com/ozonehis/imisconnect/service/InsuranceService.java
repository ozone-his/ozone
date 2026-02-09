package com.ozonehis.imisconnect.service;

import com.ozonehis.imisconnect.dto.ClaimResponse;
import com.ozonehis.imisconnect.dto.EligibilityRequest;
import com.ozonehis.imisconnect.dto.EligibilityResponse;
import com.ozonehis.imisconnect.service.fhir.FhirService;
import com.ozonehis.imisconnect.service.graphql.GraphQLService;
import org.hl7.fhir.r4.model.Claim;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InsuranceService {

    private final FhirService fhirService;
    private final GraphQLService graphQLService;

    @Autowired
    public InsuranceService(FhirService fhirService, GraphQLService graphQLService) {
        this.fhirService = fhirService;
        this.graphQLService = graphQLService;
    }

    public EligibilityResponse checkEligibility(EligibilityRequest request) {
        try {
            return fhirService.checkEligibility(request);
        } catch (Exception e) {
            return graphQLService.checkEligibility(request);
        }
    }

    public ClaimResponse submitClaim(Claim claim) {
        try {
            return fhirService.submitClaim(claim);
        } catch (Exception e) {
            return graphQLService.submitClaim(claim);
        }
    }

    public ClaimResponse getClaimStatus(String claimId) {
        try {
            return fhirService.getClaimStatus(claimId);
        } catch (Exception e) {
            return graphQLService.getClaimStatus(claimId);
        }
    }
}
