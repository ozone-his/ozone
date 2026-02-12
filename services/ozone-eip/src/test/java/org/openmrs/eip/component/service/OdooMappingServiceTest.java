package org.openmrs.eip.component.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OdooMappingServiceTest {

    private OdooMappingService service;

    @BeforeEach
    public void setUp() {
        service = new OdooMappingService();
    }

    @Test
    public void getConsultationProduct_shouldReturnEmergencyUuidForEmergencyVisit() {
        String result = service.getConsultationProduct("Emergency");
        assertEquals("e4832123-emergency-uuid", result);
    }

    @Test
    public void getConsultationProduct_shouldReturnDefaultUuidForOtherVisits() {
        String result = service.getConsultationProduct("General");
        assertEquals("8ef596f4-6014-4113-90d1-0f40d1e38936", result);
    }

    @Test
    public void resolveHospitalCompany_shouldReturnCompanyId() {
        Integer result = service.resolveHospitalCompany("some-location-uuid");
        assertEquals(1, result);
    }
}
