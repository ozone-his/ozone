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
    
    @api.multi
    def submit_claim(self):
        url = 'http://localhost:8085/api/v1/claims'
        headers = {'Content-Type': 'application/json'}
        
        for claim in self:
            claim_data = claim._prepare_claim_data()
            try:
                response = requests.post(url, json=claim_data, headers=headers)
                response.raise_for_status()
                
                result = response.json()
                claim.imis_claim_id = result.get('claimId')
                claim.status = 'submitted'
                
                self.env.cr.commit()
                
            except requests.exceptions.RequestException as e:
                raise Warning(f"Error submitting claim: {str(e)}")
    
    @api.multi
    def get_claim_status(self):
        url = 'http://localhost:8085/api/v1/claims/{claim_id}'
        headers = {'Content-Type': 'application/json'}
        
        for claim in self:
            if not claim.imis_claim_id:
                continue
                
            try:
                response = requests.get(url.format(claim_id=claim.imis_claim_id), headers=headers)
                response.raise_for_status()
                
                result = response.json()
                claim.status = result.get('status', 'unknown')
                
                self.env.cr.commit()
                
            except requests.exceptions.RequestException as e:
                raise Warning(f"Error getting claim status: {str(e)}")
    
    @api.multi
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
