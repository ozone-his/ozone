package com.ozonehis.imisconnect.controller;

import com.ozonehis.imisconnect.dto.ClaimResponse;
import com.ozonehis.imisconnect.dto.EligibilityRequest;
import com.ozonehis.imisconnect.dto.EligibilityResponse;
import com.ozonehis.imisconnect.service.InsuranceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.hl7.fhir.r4.model.Claim;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Insurance API", description = "API for insurance eligibility and claim management")
public class InsuranceController {

    private final InsuranceService insuranceService;

    @Autowired
    public InsuranceController(InsuranceService insuranceService) {
        this.insuranceService = insuranceService;
    }

    @Operation(summary = "Check patient insurance eligibility",
            description = "Verifies patient insurance eligibility using insuree ID and facility UUID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Eligibility check successful"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "404", description = "Insuree not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/eligibility")
    public ResponseEntity<EligibilityResponse> checkEligibility(
            @Valid @RequestBody EligibilityRequest request) {
        EligibilityResponse response = insuranceService.checkEligibility(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Submit insurance claim",
            description = "Submits a FHIR Claim to openIMIS for adjudication")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Claim submission successful"),
            @ApiResponse(responseCode = "400", description = "Invalid claim data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/claims")
    public ResponseEntity<ClaimResponse> submitClaim(@RequestBody Claim claim) {
        ClaimResponse response = insuranceService.submitClaim(claim);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get claim status",
            description = "Retrieves the status of a submitted claim")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Claim status retrieved"),
            @ApiResponse(responseCode = "404", description = "Claim not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/claims/{claimId}")
    public ResponseEntity<ClaimResponse> getClaimStatus(@PathVariable String claimId) {
        ClaimResponse response = insuranceService.getClaimStatus(claimId);
        return ResponseEntity.ok(response);
    }
}
