from odoo import fields, models

class ResCompany(models.Model):
    _inherit = "res.company"

    x_facility_uuid = fields.Char(string="Facility UUID", help="The UUID of the facility in OpenMRS/openIMIS")
