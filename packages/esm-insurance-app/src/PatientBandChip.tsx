import React, { useEffect, useState } from "react";
import { useCurrentPatient, restBaseUrl } from "@openmrs/esm-framework";

const colorMap: Record<string,string> = { A:"#2f80ed", B:"#27ae60", C:"#f2c94c", D:"#9b51e0" };

export default function PatientBandChip() {
  const { currentPatient } = useCurrentPatient();
  const [band, setBand] = useState<string | null>(null);

  const refresh = async () => {
    if (!currentPatient?.id) return;
    // Fetch patient attributes and extract the band
    const res = await fetch(`${restBaseUrl}/person/${currentPatient.id}/attribute?v=full`);
    const json = await res.json();
    const a = json?.results?.find((x:any)=> x?.attributeType?.display?.toLowerCase().includes("band"));
    setBand(a?.value || null);
  };

  useEffect(() => {
    refresh();
    const handler = (e: Event) => refresh();
    window.addEventListener("oznhmo:band-updated", handler);
    return () => window.removeEventListener("oznhmo:band-updated", handler);
  }, [currentPatient?.id]);

  if (!band) return null;

  return (
    <span style={{
      backgroundColor: colorMap[band] || "#ccc",
      color: "#fff", borderRadius: "12px", padding: "0.15rem 0.5rem",
      fontWeight: 600, marginLeft: "0.25rem"
    }}
      title={`Patient Band: ${band}${band==="D" ? " (Uniform tariff)" : ""}`}
    >
      Band {band}
    </span>
  );
}
