import { defineConfigSchema, getAsyncLifecycle } from "@openmrs/esm-framework";

export const importTranslation = async () => ({});
export const activate = () => true;

export const PatientBandChip = getAsyncLifecycle(
  () => import("./PatientBandChip"),
  { featureName: "oznhmo-patient-band-chip", moduleName: "@oznhmo/esm-insurance-app" }
);

export const EligibilityPanel = getAsyncLifecycle(
  () => import("./EligibilityPanel"),
  { featureName: "oznhmo-eligibility-panel", moduleName: "@oznhmo/esm-insurance-app" }
);

export const InsuranceTab = getAsyncLifecycle(
  () => import("./InsuranceTab"),
  { featureName: "oznhmo-insurance-tab", moduleName: "@oznhmo/esm-insurance-app" }
);

export const OrdersPriceHint = getAsyncLifecycle(
  () => import("./OrdersPriceHint"),
  { featureName: "oznhmo-orders-price-hint", moduleName: "@oznhmo/esm-insurance-app" }
);

defineConfigSchema("@oznhmo/esm-insurance-app", {
  imisConnectBaseUrl: { _type: "string", _default: "http://localhost:8085/api/v1" },
  patientBandAttributeTypeUuid: { _type: "string", _default: "123e4567-e89b-12d3-a456-426614174000" }
});
