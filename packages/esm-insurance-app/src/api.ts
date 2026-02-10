import { config } from "./config";
import { restBaseUrl } from "@openmrs/esm-framework";

export type EligibilityResponse = {
  valid: boolean;
  band: "A" | "B" | "C" | "D" | null;
  plan?: string;
  allowedBands?: string[];
  expiry?: string;
  message?: string;
};

export async function validateEligibility(insureeId: string, facilityUuid: string): Promise<EligibilityResponse> {
  const res = await fetch(`${config.imisConnectBaseUrl}/eligibility`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ insureeId, facilityUuid })
  });
  if (!res.ok) throw new Error(`Eligibility request failed: ${res.statusText}`);
  return await res.json();
}

// Minimal OpenMRS REST calls (use session auth / token set by O3)
export async function setPatientBand(patientUuid: string, band: "A"|"B"|"C"|"D") {
  // Create or update person attribute (patient is a Person in OpenMRS)
  const payload = { attributeType: config.patientBandAttributeTypeUuid, value: band };
  // Try upsert: list attributes and replace if exists
  const atts = await (await fetch(`${restBaseUrl}/person/${patientUuid}/attribute?v=full`)).json();
  const existing = atts?.results?.find((a:any) => a.attributeType?.uuid === config.patientBandAttributeTypeUuid);
  if (existing) {
    await fetch(`${restBaseUrl}/person/${patientUuid}/attribute/${existing.uuid}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ value: band })
    });
  } else {
    await fetch(`${restBaseUrl}/person/${patientUuid}/attribute`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
  }
}
