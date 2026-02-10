import React, { useEffect, useState } from "react";
import { useCurrentPatient } from "@openmrs/esm-framework";
import { config } from "./config";

export default function InsuranceTab() {
  const { currentPatient } = useCurrentPatient();
  const [claims, setClaims] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (currentPatient?.id) {
      setLoading(true);
      fetch(`${config.imisConnectBaseUrl}/claims?patientId=${currentPatient.id}`)
        .then(res => res.json())
        .then(data => {
          setClaims(data || []);
          setLoading(false);
        })
        .catch(() => setLoading(false));
    }
  }, [currentPatient?.id]);

  return (
    <div style={{ padding: "1rem" }}>
      <h3>Insurance Claims</h3>
      {loading ? <p>Loading claims...</p> : (
        <table style={{ width: "100%", borderCollapse: "collapse" }}>
          <thead>
            <tr style={{ borderBottom: "2px solid #eee", textAlign: "left" }}>
              <th>Claim ID</th>
              <th>Status</th>
              <th>Date</th>
              <th>Amount</th>
            </tr>
          </thead>
          <tbody>
            {claims.map((c, i) => (
              <tr key={i} style={{ borderBottom: "1px solid #eee" }}>
                <td>{c.claimId}</td>
                <td><span style={{
                  padding: "0.2rem 0.5rem", borderRadius: "4px",
                  backgroundColor: "#e0e0e0", fontSize: "0.85rem"
                }}>{c.status}</span></td>
                <td>{c.date}</td>
                <td>{c.amount} {c.currency}</td>
              </tr>
            ))}
            {claims.length === 0 && <tr><td colSpan={4}>No claims found for this patient.</td></tr>}
          </tbody>
        </table>
      )}
    </div>
  );
}
