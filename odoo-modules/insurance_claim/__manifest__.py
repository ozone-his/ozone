# -*- coding: utf-8 -*-
{
  "name": "Insurance Claim (Nigeria HMO)",
  "summary": "Band-aware tariffs, eligibility, claim submission and tracking",
  "version": "16.0.1.0.0",
  "author": "OzNHMO",
  "depends": ["account", "contacts", "sale"],
  "data": [
    "security/ir.model.access.csv",
    "views/menu.xml",
    "views/tariffs.xml",
    "views/account_move_view.xml",
    "views/claim_kanban.xml",
    "views/res_company_view.xml",
    "data/seed/tariff_band_d.xml",
    "data/seed/tariff_abc_facility.xml"
  ],
  "installable": True
}
