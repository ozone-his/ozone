from odoo import api, fields, models, _
from odoo.exceptions import ValidationError

class TariffBandD(models.Model):
    _name = "tariff.band.d"
    _description = "Band D Global Tariff (Uniform)"
    _sql_constraints = [
        ("code_unique", "unique(service_code)", "Each service_code must be unique for Band D.")
    ]

    service_code = fields.Char(required=True, index=True)
    description = fields.Char()
    price_ngn = fields.Float(required=True)
    effective_date = fields.Date(required=True, default=fields.Date.today)
