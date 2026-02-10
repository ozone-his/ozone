import React, { useState } from "react";
import { useCurrentPatient } from "@openmrs/esm-framework";
import { validateEligibility, setPatientBand } from "./api";

export default function EligibilityPanel() {
  const { currentPatient } = useCurrentPatient();
  const [insureeId, setInsureeId] = useState("");
  const [loading, setLoading] = useState(false);
  const [resp, setResp] = useState<any>(null);
  const patientUuid = currentPatient?.id ?? "";

  const facilityUuid = window.sessionStorage.getItem("currentFacilityUuid") || "FACILITY-UUID";

  const onValidate = async () => {
    setLoading(true);
    try {
      const result = await validateEligibility(insureeId, facilityUuid);
      setResp(result);
      if (result.valid && result.band) {
        await setPatientBand(patientUuid, result.band);
        window.dispatchEvent(new CustomEvent("oznhmo:band-updated", { detail: { band: result.band }}));
      }
    } catch (e:any) {
      setResp({ valid:false, message: e.message });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ padding: "0.75rem" }}>
      <h4>Insurance Eligibility</h4>
      <div>
        <label>Insuree/HMO ID</label>
        <input value={insureeId} onChange={(e)=>setInsureeId(e.target.value)} placeholder="Enter ID" />
        <button onClick={onValidate} disabled={!insureeId || loading}>{loading ? "Checking..." : "Validate"}</button>
      </div>

      {resp && (
        <div style={{ marginTop: "0.5rem", border: "1px solid #eee", padding: "0.5rem" }}>
          <p>Status: <strong style={{color: resp.valid ? "green" : "crimson"}}>{resp.valid ? "Valid" : "Invalid"}</strong></p>
          <p>Band: <strong>{resp.band || "-"}</strong>{resp.band === "D" ? " (Uniform tariff)" : ""}</p>
          <p>Plan: <strong>{resp.plan || "-"}</strong></p>
          <p>Expiry: <strong>{resp.expiry || "-"}</strong></p>
          {resp.allowedBands && <p>Allowed Bands: {resp.allowedBands.join(", ")}</p>}
          {resp.message && <p>{resp.message}</p>}
        </div>
      )}
    </div>
  );
}
