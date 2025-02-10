SELECT DISTINCT
	 pai.identifier AS 'Patient ID',
	 concat(pn.given_name, ' ', IF(pn.middle_name IS NULL OR pn.middle_name = '', '', concat(pn.middle_name, ' ')),
		IF(pn.family_name IS NULL OR pn.family_name = '', '', pn.family_name)) AS "Patient Name",
	 TIMESTAMPDIFF(YEAR, p.birthdate, CURDATE()) AS "Age",
	 p.gender AS 'Gender',
	 pMobile.phoneNumber AS 'Phone Number',
     DATE_FORMAT(start_date_time, "%d/%m/%Y %h:%i %p") AS 'Appointment Time'
FROM patient_appointment pa
   JOIN appointment_service app_service
    ON app_service.appointment_service_id = pa.appointment_service_id AND app_service.voided IS FALSE
   LEFT JOIN person p ON p.person_id = pa.patient_id AND pa.voided IS FALSE
   LEFT JOIN person_name pn ON p.person_id = pn.person_id AND pn.voided IS FALSE
   LEFT JOIN patient_identifier pai ON (pai.patient_id = pa.patient_id AND pai.preferred = 1)
   LEFT JOIN (select paMobile.person_id as 'pMobilePersonId', paMobile.value AS 'phoneNumber' from person_attribute paMobile
   JOIN person_attribute_type patMobile ON patMobile.name = "phoneNumber" AND patMobile.retired IS FALSE
    AND patMobile.person_attribute_type_id = paMobile.person_attribute_type_id) AS pMobile ON pa.patient_id = pMobile.pMobilePersonId
WHERE pa.status = 'Scheduled' and cast(pa.start_date_time AS DATE) BETWEEN '#startDate#' AND '#endDate#' ORDER BY pa.start_date_time DESC;
