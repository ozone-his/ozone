import React, { useEffect, useState } from "react";
import { useCurrentPatient } from "@openmrs/esm-framework";
import { config } from "./config";

export default function OrdersPriceHint({ orderContext }: { orderContext: any }) {
  const { currentPatient } = useCurrentPatient();
  const [price, setPrice] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const productCode = orderContext?.concept?.display || orderContext?.concept?.name; // Simplification

  useEffect(() => {
    if (productCode && currentPatient?.id) {
      setLoading(true);
      // Fetch estimated price from Odoo via IMIS-Connect
      fetch(`${config.imisConnectBaseUrl}/pricing/estimate?code=${productCode}&patientId=${currentPatient.id}`)
        .then(res => res.json())
        .then(data => {
          setPrice(data.price ? `${data.price} ${data.currency}` : "Not covered");
          setLoading(false);
        })
        .catch(() => {
          setPrice(null);
          setLoading(false);
        });
    }
  }, [productCode, currentPatient?.id]);

  if (!productCode || loading || !price) return null;

  return (
    <div style={{ fontSize: "0.85rem", color: price === "Not covered" ? "orange" : "green", marginTop: "0.25rem" }}>
      <strong>Est. Price:</strong> {price}
    </div>
  );
}
