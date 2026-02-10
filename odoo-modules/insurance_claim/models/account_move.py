from odoo import api, fields, models, _
from odoo.exceptions import UserError
from datetime import date

class AccountMove(models.Model):
    _inherit = "account.move"

    insurance_plan = fields.Char(readonly=True)
    insurance_band = fields.Selection([("A","A"),("B","B"),("C","C"),("D","D")], readonly=True)
    facility_uuid = fields.Char(readonly=False)
    claim_id = fields.Char(readonly=True)
    claim_status = fields.Selection([
        ("entered","Entered"),("checked","Checked"),("processed","Processed"),("valuated","Valuated"),("rejected","Rejected")
    ], readonly=True)

    @api.model
    def create(self, vals):
        if not vals.get("facility_uuid"):
            # Try derive from current user's company/location mapping
            # Assuming a custom field x_facility_uuid on res.company
            facility = self.env.user.company_id.x_facility_uuid if hasattr(self.env.user.company_id, 'x_facility_uuid') else False
            if facility:
                vals["facility_uuid"] = facility
        return super().create(vals)

    def action_post(self):
        for inv in self:
            if not inv.facility_uuid:
                raise UserError(_("Facility UUID is required. Please set it before posting the invoice."))
        return super().action_post()

    def action_validate_coverage(self):
        client = self.env["insurance.claim.client"]
        for inv in self:
            if not inv.partner_id or not inv.partner_id.ref:
                raise UserError(_("Patient must have an Insuree/HMO ID in 'Customer Reference'."))
            if not inv.facility_uuid:
                raise UserError(_("Facility UUID is required on the invoice."))

            res = client.validate_coverage(inv.partner_id.ref, inv.facility_uuid)
            inv.write({
                "insurance_plan": res.get("plan"),
                "insurance_band": res.get("band")
            })
        return True

    def action_submit_claim(self):
        client = self.env["insurance.claim.client"]
        for inv in self:
            if inv.claim_id:
                raise UserError(_("Claim already submitted: %s") % inv.claim_id)
            if not inv.insurance_band:
                raise UserError(_("Insurance band unknown. Please Validate Coverage first."))

            items = []
            for line in inv.invoice_line_ids:
                if not line.product_id or not line.product_id.default_code:
                    raise UserError(_("Service code missing for product on line '%s'") % line.name)
                items.append({
                    "sequence": line.sequence or 1,
                    "productOrService": {"coding": [{"code": line.product_id.default_code}]},
                    "unitPrice": {"value": line.price_unit, "currency": inv.currency_id.name},
                    "quantity": {"value": line.quantity}
                })

            claim = {
              "resourceType": "Claim",
              "status": "active",
              "use": "claim",
              "type": {"coding":[{"system":"http://terminology.hl7.org/CodeSystem/claim-type","code":"professional"}]},
              "patient": {"reference": f"Patient/{inv.partner_id.id}"},  # map to openIMIS patient externally if needed
              "created": date.today().isoformat(),
              "provider": {"reference": f"Organization/{inv.facility_uuid}"},
              "priority": {"coding":[{"system":"http://terminology.hl7.org/CodeSystem/processpriority","code":"normal"}]},
              "insurance": [{"sequence":1,"focal":True,"coverage":{"reference":"Coverage/placeholder"}}],
              "item": items,
              "total": {"value": sum(l.price_subtotal for l in inv.invoice_line_ids), "currency": inv.currency_id.name}
            }

            res = client.submit_claim(claim)
            inv.write({"claim_id": res.get("claimId"), "claim_status": res.get("status")})
        return True

    def action_track_claim(self):
        client = self.env["insurance.claim.client"]
        for inv in self:
            if not inv.claim_id:
                raise UserError(_("No claim to track. Submit Claim first."))
            res = client.get_claim_status(inv.claim_id)
            inv.write({"claim_status": res.get("status")})
        return True


class AccountMoveLine(models.Model):
    _inherit = "account.move.line"

    service_code = fields.Char(related="product_id.default_code", store=False)
    coverage_hint = fields.Char(compute="_compute_coverage_hint", store=False)

    def _compute_coverage_hint(self):
        for line in self:
            inv = line.move_id
            band = inv.insurance_band
            line.coverage_hint = f"Band {band or '-'}"

    @api.onchange("product_id")
    def _onchange_product_id_set_band_price(self):
        for line in self:
            inv = line.move_id
            if not line.product_id or not inv:
                continue
            if not line.product_id.default_code:
                continue
            if not inv.insurance_band:
                continue

            price = self._resolve_tariff_price(
                service_code=line.product_id.default_code,
                band=inv.insurance_band,
                facility_uuid=inv.facility_uuid
            )
            if price is not None:
                line.price_unit = price
            elif inv.insurance_band in ("A", "B", "C"):
                raise UserError(_(
                    "No tariff configured for service '%s' (Band %s @ Facility %s)."
                ) % (line.product_id.default_code, inv.insurance_band, inv.facility_uuid))

    @api.depends("product_id", "move_id.insurance_band", "move_id.facility_uuid")
    def _compute_price_unit(self):
        super(AccountMoveLine, self)._compute_price_unit()
        for line in self:
            inv = line.move_id
            if not line.product_id or not line.product_id.default_code or not inv.insurance_band:
                continue
            price = self._resolve_tariff_price(
                service_code=line.product_id.default_code,
                band=inv.insurance_band,
                facility_uuid=inv.facility_uuid
            )
            if price is not None:
                line.price_unit = price

    def _resolve_tariff_price(self, service_code: str, band: str, facility_uuid: str):
        if band == "D":
            rec = self.env["tariff.band.d"].sudo().search([("service_code", "=", service_code)], limit=1)
            return rec.price_ngn if rec else None
        else:  # A/B/C
            if not facility_uuid:
                raise UserError(_("Facility UUID is required on the invoice to resolve A/B/C tariffs."))
            rec = self.env["tariff.abc.facility"].sudo().search([
                ("service_code", "=", service_code),
                ("band", "=", band),
                ("facility_uuid", "=", facility_uuid)
            ], limit=1)
            return rec.price_ngn if rec else None
