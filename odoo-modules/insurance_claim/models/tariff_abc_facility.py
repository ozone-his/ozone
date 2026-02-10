from odoo import api, fields, models, _
from odoo.exceptions import ValidationError

class TariffABCF(models.Model):
    _name = "tariff.abc.facility"
    _description = "Band A/B/C Tariff per Facility"
    _sql_constraints = [
        ("code_band_facility_unique",
         "unique(service_code, band, facility_uuid)",
         "Combination of service_code, band, and facility must be unique.")
    ]

    service_code = fields.Char(required=True, index=True)
    band = fields.Selection([("A","A"),("B","B"),("C","C")], required=True)
    facility_uuid = fields.Char(required=True, index=True)
    description = fields.Char()
    price_ngn = fields.Float(required=True)
    effective_date = fields.Date(required=True, default=fields.Date.today)
