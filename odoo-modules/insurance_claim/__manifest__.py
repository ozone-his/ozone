{
    'name': 'Insurance Claim Management',
    'version': '1.0',
    'category': 'Healthcare/Insurance',
    'summary': 'Insurance claim management with band-aware tariffs',
    'description': """
    This module provides insurance claim management functionality for Ozone HIS, including:
    - Band-aware tariff management (Band D shared, Bands A/B/C facility-specific)
    - Insurance eligibility check
    - Claim submission to openIMIS
    - Claim status tracking
    - Integration with IMIS-Connect API
    """,
    'author': 'Ozone HIS Team',
    'website': 'https://ozone-his.com',
    'depends': ['base', 'account', 'sale', 'purchase'],
    'data': [
        'security/ir.model.access.csv',
        'views/tariff_views.xml',
        'views/claim_views.xml',
        'views/menu.xml',
    ],
    'installable': True,
    'application': True,
}
