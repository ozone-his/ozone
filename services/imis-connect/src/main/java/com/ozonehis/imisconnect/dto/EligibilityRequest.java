package com.ozonehis.imisconnect.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EligibilityRequest {

    @NotBlank(message = "Insuree ID is required")
    private String insureeId;

    @NotBlank(message = "Facility UUID is required")
    private String facilityUuid;
}
