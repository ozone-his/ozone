package com.ozonehis.imisconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityResponse {

    private boolean valid;

    private String plan;

    private String band;

    private List<String> allowedBands;

    private LocalDate expiryDate;
}
