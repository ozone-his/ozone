export const config = {
  imisConnectBaseUrl: process.env.IMIS_CONNECT_BASE_URL || "http://localhost:8085/api/v1",
  // OpenMRS REST base is discovered by esm-framework at runtime
  patientBandAttributeTypeUuid: process.env.PATIENT_BAND_ATTR_UUID || "123e4567-e89b-12d3-a456-426614174000"
};
