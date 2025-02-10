SELECT
  DISTINCT (@rownum := @rownum + 1)                                                                                                       AS "Sr. No.",
  IF(extraIdentifier.identifier IS NULL OR extraIdentifier.identifier = "", primaryIdentifier.identifier, extraIdentifier.identifier)     AS "Patient Id",
  concat(pn.given_name, " ", ifnull(pn.family_name, ""))                                                                                  AS "Patient Name",
  floor(DATEDIFF(NOW(), p.birthdate) / 365)                                                                                               AS "Age",
  p.gender                                                                                                                                AS "Gender",
  DATE_FORMAT(pt.date_created, "%d-%b-%Y")                                                                   AS "Registration Date"

FROM patient pt
  JOIN person p ON p.person_id = pt.patient_id AND p.voided is FALSE
  JOIN person_name pn ON pn.person_id = p.person_id AND pn.voided is FALSE
  JOIN (SELECT pri.patient_id, pri.identifier 
    FROM patient_identifier pri 
      JOIN patient_identifier_type pit ON pri.identifier_type = pit.patient_identifier_type_id AND pit.retired is FALSE
      JOIN global_property gp ON gp.property="bahmni.primaryIdentifierType" AND INSTR (gp.property_value, pit.uuid)) primaryIdentifier 
		ON pt.patient_id = primaryIdentifier.patient_id		
  LEFT OUTER JOIN (SELECT ei.patient_id, ei.identifier 
    FROM patient_identifier ei 
      JOIN patient_identifier_type pit ON ei.identifier_type = pit.patient_identifier_type_id AND pit.retired is FALSE
      JOIN global_property gp ON gp.property="bahmni.extraPatientIdentifierTypes" AND INSTR (gp.property_value, pit.uuid)) extraIdentifier 
		ON pt.patient_id = extraIdentifier.patient_id
  CROSS JOIN (SELECT @rownum := 0) AS dummy
  WHERE pt.voided is FALSE
  AND cast(pt.date_created AS DATE) BETWEEN '#startDate#' AND '#endDate#';