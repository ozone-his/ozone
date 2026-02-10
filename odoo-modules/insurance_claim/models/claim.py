from odoo import models, fields, api
import requests

class InsuranceClaim(models.Model):
    _name = 'insurance.claim'
    _description = 'Insurance Claim'
    
    name = fields.Char(
        string='Claim Number',
        readonly=True,
        default='New'
    )
    patient_id = fields.Char(
        string='Patient ID'
    )
    insuree_id = fields.Char(
        string='Insuree ID'
    )
    band = fields.Selection(
        string='Patient Band',
        selection=[('A', 'Band A'), ('B', 'Band B'), ('C', 'Band C'), ('D', 'Band D')]
    )
    facility_uuid = fields.Char(
        string='Facility UUID'
    )
    status = fields.Selection(
        string='Claim Status',
        selection=[
            ('draft', 'Draft'),
            ('submitted', 'Submitted'),
            ('processed', 'Processed'),
            ('approved', 'Approved'),
            ('rejected', 'Rejected')
        ],
        default='draft'
    )
    total_amount = fields.Float(
        string='Total Amount',
        readonly=True,
        digits=(16, 2)
    )
    claim_items = fields.One2many(
        'insurance.claim.item',
        'claim_id',
        string='Claim Items'
    )
    imis_claim_id = fields.Char(
        string='openIMIS Claim ID'
    )
    
    @api.model
    def create(self, vals):
        if vals.get('name', 'New') == 'New':
            vals['name'] = self.env['ir.sequence'].next_by_code('insurance.claim') or 'New'
        return super(InsuranceClaim, self).create(vals)
    
    def submit_claim(self):
        config_url = self.env['ir.config_parameter'].sudo().get_param('imis_connect.url', 'http://imis-connect:8080')
        url = f"{config_url}/api/v1/claims"
        headers = {'Content-Type': 'application/json'}
        
        for claim in self:
            claim_data = claim._prepare_claim_data()
            try:
                response = requests.post(url, json=claim_data, headers=headers)
                response.raise_for_status()
                
                result = response.json()
                claim.imis_claim_id = result.get('claimId')
                claim.status = 'submitted'
                
            except requests.exceptions.RequestException as e:
                raise Warning(f"Error submitting claim: {str(e)}")
    
    def check_eligibility(self):
        config_url = self.env['ir.config_parameter'].sudo().get_param('imis_connect.url', 'http://imis-connect:8080')
        url = f"{config_url}/api/v1/eligibility"
        headers = {'Content-Type': 'application/json'}

        for claim in self:
            if not claim.insuree_id or not claim.facility_uuid:
                raise Warning("Insuree ID and Facility UUID are required for eligibility check")

            data = {
                "insureeId": claim.insuree_id,
                "facilityUuid": claim.facility_uuid
            }

            try:
                response = requests.post(url, json=data, headers=headers)
                response.raise_for_status()

                result = response.json()
                if result.get('valid'):
                    claim.band = result.get('band')
                    # Log eligibility check in chatter
                    claim.message_post(body=f"Eligibility check: VALID. Band: {claim.band}. Plan: {result.get('plan')}")
                else:
                    claim.message_post(body="Eligibility check: INVALID")

            except requests.exceptions.RequestException as e:
                raise Warning(f"Error checking eligibility: {str(e)}")

    def get_claim_status(self):
        config_url = self.env['ir.config_parameter'].sudo().get_param('imis_connect.url', 'http://imis-connect:8080')
        url = f"{config_url}/api/v1/claims/{{claim_id}}"
        headers = {'Content-Type': 'application/json'}
        
        for claim in self:
            if not claim.imis_claim_id:
                continue
                
            try:
                response = requests.get(url.format(claim_id=claim.imis_claim_id), headers=headers)
                response.raise_for_status()
                
                result = response.json()
                claim.status = result.get('status', 'unknown')
                
            except requests.exceptions.RequestException as e:
                raise Warning(f"Error getting claim status: {str(e)}")
    
    def _prepare_claim_data(self):
        self.ensure_one()
        
        claim_data = {
            "resourceType": "Claim",
            "status": "active",
            "use": "claim",
            "type": {
                "coding": [{
                    "system": "http://terminology.hl7.org/CodeSystem/claim-type",
                    "code": "professional"
                }]
            },
            "patient": {
                "reference": f"Patient/{self.patient_id}"
            },
            "created": fields.Date.today().strftime('%Y-%m-%d'),
            "provider": {
                "reference": f"Organization/{self.facility_uuid}"
            },
            "priority": {
                "coding": [{
                    "system": "http://terminology.hl7.org/CodeSystem/processpriority",
                    "code": "normal"
                }]
            },
            "item": []
        }
        
        for item in self.claim_items:
            claim_data['item'].append({
                "sequence": item.sequence,
                "productOrService": {
                    "coding": [{
                        "code": item.service_code
                    }]
                },
                "unitPrice": {
                    "value": item.unit_price,
                    "currency": "NGN"
                },
                "quantity": {
                    "value": item.quantity
                },
                "net": {
                    "value": item.amount,
                    "currency": "NGN"
                }
            })
            
        total = sum(item.amount for item in self.claim_items)
        claim_data['total'] = {
            "value": total,
            "currency": "NGN"
        }
        
        return claim_data

class InsuranceClaimItem(models.Model):
    _name = 'insurance.claim.item'
    _description = 'Insurance Claim Item'
    
    claim_id = fields.Many2one(
        'insurance.claim',
        string='Claim'
    )
    sequence = fields.Integer(
        string='Sequence',
        default=1
    )
    service_code = fields.Char(
        string='Service Code'
    )
    description = fields.Text(
        string='Description'
    )
    quantity = fields.Float(
        string='Quantity',
        default=1.0
    )
    unit_price = fields.Float(
        string='Unit Price'
    )
    amount = fields.Float(
        string='Amount',
        compute='_compute_amount',
        store=True
    )
    
    @api.depends('quantity', 'unit_price')
    def _compute_amount(self):
        for item in self:
            item.amount = item.quantity * item.unit_price

    @api.onchange('service_code')
    def _onchange_service_code(self):
        if self.service_code and self.claim_id:
            tariff_manager = self.env['insurance.tariff.manager']
            price = tariff_manager.get_price(
                self.service_code,
                self.claim_id.band,
                self.claim_id.facility_uuid
            )
            if price:
                self.unit_price = price
