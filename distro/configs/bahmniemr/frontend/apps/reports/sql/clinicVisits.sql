SELECT
  DISTINCT(pi.identifier)                                                   AS "Patient Identifier",
  concat(pn.given_name, " ", ifnull(pn.family_name, ""))                    AS "Patient Name",
  floor(DATEDIFF(DATE(v.date_started), p.birthdate) / 365)                  AS "Age",
  DATE_FORMAT(p.birthdate, "%d-%b-%Y")                                      AS "Birthdate",
  p.gender                                                                  AS "Gender",
  DATE_FORMAT(p.date_created, "%d-%b-%Y")                                   AS "Patient Created Date",
  vt.name                                                                   AS "Visit type",
  DATE_FORMAT(v.date_started, "%d-%b-%Y")                                   AS "Date started",
  DATE_FORMAT(v.date_stopped, "%d-%b-%Y")                                   AS "Date stopped",
  GROUP_CONCAT(DISTINCT(IF(pat.name = "phoneNumber",pa.value, NULL)))       AS "Phone number",
  paddress.city_village                                                     AS "City/Village",
  paddress.state_province                                                   AS "State",
  CASE WHEN v.date_stopped IS NULL THEN "Active"
  ELSE "Inactive"
  END                                                                       AS "Visit Status"
FROM visit v
  JOIN visit_type vt ON v.visit_type_id = vt.visit_type_id
  JOIN person p ON p.person_id = v.patient_id AND p.voided is FALSE
  JOIN patient_identifier pi ON p.person_id = pi.patient_id AND pi.voided is FALSE
  JOIN patient_identifier_type pit ON pi.identifier_type = pit.patient_identifier_type_id AND pit.retired is FALSE
  JOIN person_name pn ON pn.person_id = p.person_id AND pn.voided is FALSE
  LEFT OUTER JOIN person_address paddress ON p.person_id = paddress.person_id AND paddress.voided is FALSE
  LEFT OUTER JOIN person_attribute pa ON pa.person_id = p.person_id AND pa.voided is FALSE
  LEFT OUTER JOIN person_attribute_type pat ON pat.person_attribute_type_id = pa.person_attribute_type_id AND pat.retired is FALSE
  WHERE v.voided is FALSE
  AND cast(v.date_started AS DATE) BETWEEN '#startDate#' AND '#endDate#'
GROUP BY v.visit_id;
