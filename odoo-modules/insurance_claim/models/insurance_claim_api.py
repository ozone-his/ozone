import requests
from odoo import api, fields, models, _
from odoo.exceptions import UserError

class InsuranceConfig(models.TransientModel):
    _name = "insurance.config"
    _description = "IMIS-Connect Configuration (transient holder)"
    # In practice, store in ir.config_parameter
    # Keys: imis_connect_base_url, imis_connect_token (if needed)

def _imis_base_url(env):
    ICP = env["ir.config_parameter"].sudo()
    return ICP.get_param("imis_connect_base_url", "http://localhost:8085/api/v1")

class InsuranceClaimClient(models.AbstractModel):
    _name = "insurance.claim.client"
    _description = "Client to IMIS-Connect"

    def validate_coverage(self, insuree_id, facility_uuid):
        url = f"{_imis_base_url(self.env)}/eligibility"
        r = requests.post(url, json={"insureeId": insuree_id, "facilityUuid": facility_uuid}, timeout=20)
        if not r.ok:
            raise UserError(_("Eligibility check failed: %s") % r.text)
        return r.json()

    def submit_claim(self, claim_payload):
        url = f"{_imis_base_url(self.env)}/claims"
        r = requests.post(url, json=claim_payload, timeout=30)
        if not r.ok:
            raise UserError(_("Claim submission failed: %s") % r.text)
        return r.json()

    def get_claim_status(self, claim_id):
        url = f"{_imis_base_url(self.env)}/claims/{claim_id}"
        r = requests.get(url, timeout=20)
        if not r.ok:
            raise UserError(_("Claim status fetch failed: %s") % r.text)
        return r.json()
