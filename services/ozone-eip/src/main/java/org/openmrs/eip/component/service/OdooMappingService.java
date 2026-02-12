package org.openmrs.eip.component.service;

import org.apache.camel.Header;
import org.springframework.stereotype.Service;

@Service("odooMappingService")
public class OdooMappingService {

    /**
     * Resolves the Odoo Company ID based on the OpenMRS Location's Band attribute.
     * Logic: Location -> Attribute[Hospital Band] -> Odoo Company Mapping Table
     */
    public Integer resolveHospitalCompany(@Header("locationUuid") String locationUuid) {
        return HospitalBandMapper.getCompanyIdByLocation(locationUuid);
    }

    /**
     * Determines which Odoo Product ID to use for the initial consultation.
     * Logic: VisitType -> Odoo Product Mapping
     */
    public String getConsultationProduct(@Header("visitType") String visitType) {
        if ("Emergency".equalsIgnoreCase(visitType)) {
            return "e4832123-emergency-uuid"; // Mapped to odoo_products.csv
        }
        return "8ef596f4-6014-4113-90d1-0f40d1e38936"; // Default General Consultation
    }
}
