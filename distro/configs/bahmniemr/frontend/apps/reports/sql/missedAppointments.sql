
SELECT DISTINCT 
	 pai.identifier AS 'Patient ID',
	 concat(pn.given_name, ' ', IF(pn.middle_name IS NULL OR pn.middle_name = '', '', concat(pn.middle_name, ' ')), 
		IF(pn.family_name IS NULL OR pn.family_name = '', '', pn.family_name)) AS "Patient Name", 
	 TIMESTAMPDIFF(YEAR, p.birthdate, CURDATE()) AS "Age", 
	 p.gender AS 'Gender', 
	 DATE_FORMAT(start_date_time, "%d/%m/%Y %h:%i %p") AS 'Missed Appointment Date', 
	 app_service.name AS 'Missed Appointment Service', 
	 concat(ppn.given_name, ' ', IF(ppn.family_name IS NULL OR ppn.family_name = '', '', ppn.family_name)) AS "Provider Name", 
	 pMobile.phoneNumber AS 'Phone Number', 
	 pAlternateMobile.alternatePhoneNumber AS 'Alternate Phone Number' 
FROM patient_appointment pa 
   LEFT JOIN appointment_service app_service 
    ON app_service.appointment_service_id = pa.appointment_service_id AND app_service.voided IS FALSE 
   LEFT JOIN person p ON p.person_id = pa.patient_id AND pa.voided IS FALSE 
   LEFT JOIN person_name pn ON p.person_id = pn.person_id AND pn.voided IS FALSE 
   LEFT JOIN patient_identifier pai ON (pai.patient_id = pa.patient_id AND pai.preferred = 1) 
   LEFT JOIN (select paMobile.person_id as 'pMobilePersonId', paMobile.value AS 'phoneNumber' from person_attribute paMobile 
   JOIN person_attribute_type patMobile ON patMobile.name = 'phoneNumber' AND patMobile.retired IS FALSE 
    AND patMobile.person_attribute_type_id = paMobile.person_attribute_type_id) AS pMobile ON pa.patient_id = pMobile.pMobilePersonId 
   LEFT JOIN (select paaMobile.person_id as 'pMobilePersonId', paaMobile.value AS 'alternatePhoneNumber' from person_attribute paaMobile 
    JOIN person_attribute_type patMobile ON patMobile.name = 'alternatePhoneNumber' AND patMobile.retired IS FALSE 
    AND patMobile.person_attribute_type_id = paaMobile.person_attribute_type_id) AS pAlternateMobile 
	ON pa.patient_id = pAlternateMobile.pMobilePersonId 
   LEFT JOIN patient_appointment_provider pap ON pap.patient_appointment_id = pa.patient_appointment_id 
	AND (pap.voided IS FALSE or pap.voided is null) 
	LEFT JOIN provider pr ON pr.provider_id = pap.provider_id AND (pr.retired IS FALSE or pr.retired IS null) 
	LEFT JOIN person pp ON pp.person_id = pr.person_id AND pp.voided IS FALSE 
    LEFT JOIN person_name ppn ON ppn.person_id = pp.person_id AND ppn.voided IS FALSE 
WHERE pa.status = 'Missed' and cast(pa.start_date_time AS DATE) BETWEEN '#startDate#' AND '#endDate#' ORDER BY pa.start_date_time DESC; 
