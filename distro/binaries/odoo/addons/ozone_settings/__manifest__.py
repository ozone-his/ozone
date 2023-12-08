# -*- coding: utf-8 -*-
{
    'name': "Ozone Odoo Settings",

    'summary': """
        Odoo Settings for ozone
    """,

    'description': """
        Odoo Settings for ozone
    """,

    'author': 'enyachoke',
    'website': 'https://mekomsolutions.com',
    'license': 'MIT',
    'category': 'Technical Settings',
    'version': '14.0.1.0.0',

    'depends': [
        'base',
        'sale_management'
    ],

    'data': [
        'data/ozone-settings.xml',
        'data/portal-user-template.xml'
    ],
}
