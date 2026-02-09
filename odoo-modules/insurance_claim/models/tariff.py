from odoo import models, fields, api

class TariffBandD(models.Model):
    _name = 'insurance.tariff.band.d'
    _description = 'Band D Tariff Table'
    
    service_code = fields.Char(
        string='Service Code',
        required=True,
        copy=False
    )
    price_ngn = fields.Float(
        string='Price (NGN)',
        required=True,
        digits=(16, 2)
    )
    effective_date = fields.Date(
        string='Effective Date',
        required=True,
        default=fields.Date.today
    )
    
    _sql_constraints = [
        ('service_code_unique', 'unique(service_code, effective_date)',
         'Service code must be unique for each effective date'),
    ]

class TariffABCFacility(models.Model):
    _name = 'insurance.tariff.abc.facility'
    _description = 'Band A/B/C Facility-Specific Tariff'
    
    service_code = fields.Char(
        string='Service Code',
        required=True,
        copy=False
    )
    band = fields.Selection(
        string='Band',
        selection=[('A', 'Band A'), ('B', 'Band B'), ('C', 'Band C')],
        required=True,
        copy=False
    )
    facility_uuid = fields.Char(
        string='Facility UUID',
        required=True,
        copy=False
    )
    price_ngn = fields.Float(
        string='Price (NGN)',
        required=True,
        digits=(16, 2)
    )
    effective_date = fields.Date(
        string='Effective Date',
        required=True,
        default=fields.Date.today
    )
    
    _sql_constraints = [
        ('service_band_facility_unique', 'unique(service_code, band, facility_uuid, effective_date)',
         'Service code, band, and facility combination must be unique'),
    ]

class InsuranceTariffManager(models.Model):
    _name = 'insurance.tariff.manager'
    _description = 'Insurance Tariff Manager'
    
    @api.model
    def get_price(self, service_code, band, facility_uuid=None):
        if band == 'D':
            return self._get_band_d_price(service_code)
        else:
            if not facility_uuid:
                raise ValueError('Facility UUID is required for Bands A/B/C')
            return self._get_abc_facility_price(service_code, band, facility_uuid)
    
    @api.model
    def _get_band_d_price(self, service_code):
        tariff = self.env['insurance.tariff.band.d'].search([
            ('service_code', '=', service_code),
            ('effective_date', '<=', fields.Date.today())
        ], order='effective_date desc', limit=1)
        
        return tariff.price_ngn if tariff else 0.0
    
    @api.model
    def _get_abc_facility_price(self, service_code, band, facility_uuid):
        tariff = self.env['insurance.tariff.abc.facility'].search([
            ('service_code', '=', service_code),
            ('band', '=', band),
            ('facility_uuid', '=', facility_uuid),
            ('effective_date', '<=', fields.Date.today())
        ], order='effective_date desc', limit=1)
        
        return tariff.price_ngn if tariff else 0.0
