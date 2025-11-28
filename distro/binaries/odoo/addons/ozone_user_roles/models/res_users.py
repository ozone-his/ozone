import logging

from jose import jwt
from odoo import api, models

_logger = logging.getLogger(__name__)


class CustomUser(models.Model):
    _inherit = "res.users"

    @api.model
    def auth_oauth(self, provider, params):
        credentials = super().auth_oauth(provider, params)
        self.assign_roles(credentials, params)
        return credentials

    def assign_roles(self, credentials, params):
        user = self.search([("login", "=", credentials[1]), ('oauth_access_token', '=', params['access_token'])])
        claims = jwt.get_unverified_claims(params['access_token'])
        odoo_access = claims['resource_access'].get('odoo')
        if odoo_access and odoo_access.get('roles'):
            odoo_roles = odoo_access.get('roles')
            group_ids = []
            for r in odoo_roles:
                group_id = self.env["res.groups"].search([("full_name", "=", r)])
                if group_id.id:
                    group_ids.append(group_id.id)
                else:
                    _logger.warning('No group found with full name %s', r)
            user.write({'groups_id': [(6, 0, group_ids)]})
        else:
            user.write({'groups_id': [(5, 0, 0)]})
