DELETE from episode_patient_program;
DELETE from episode_encounter;
DELETE FROM episode;
DROP PROCEDURE IF EXISTS createEpisodeAndMap;

DELIMITER //
CREATE PROCEDURE createEpisodeAndMap(IN iPatientProgramId INT)
  BEGIN
    DECLARE vCreator INT;
    DECLARE vEpisodeId INT;
    DECLARE vDateCreated DATETIME;
    DECLARE vChangedBy INT;
    DECLARE vDateChanged DATETIME;
    DECLARE vVoided TINYINT;
    DECLARE vVoidedBy INT;
    DECLARE vDateVoided DATETIME;
    DECLARE vVoidReason VARCHAR(255);
    DECLARE vUuid VARCHAR(38);

    SELECT
      creator,
      date_created,
      changed_by,
      date_changed,
      voided,
      voided_by,
      date_voided,
      void_reason
    INTO vCreator, vDateCreated, vChangedBy, vDateChanged, vVoided, vVoidedBy, vDateVoided, vVoidReason
    FROM patient_program
    WHERE patient_program_id = iPatientProgramId;

    SELECT uuid() INTO vUuid;
    INSERT INTO episode (creator, date_created, changed_by, date_changed, voided, voided_by, date_voided, void_reason, uuid)
    VALUES (vCreator, vDateCreated, vChangedBy, vDateChanged, vVoided, vVoidedBy, vDateVoided, vVoidReason, vUuid);

    SELECT episode_id INTO vEpisodeId FROM episode WHERE uuid = vUuid;
    INSERT INTO episode_patient_program VALUES (vEpisodeId, iPatientProgramId);
  END;
//

DELIMITER ;
DROP PROCEDURE IF EXISTS mapPatientProgramToEpisode;
DELIMITER //
CREATE PROCEDURE mapPatientProgramToEpisode()
  BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE vPatientProgramId INT;
    DECLARE cPatientProgramIds CURSOR FOR
      SELECT patient_program_id
      FROM patient_program
      WHERE patient_program_id NOT IN (SELECT patient_program_id
                                       FROM episode_patient_program);
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cPatientProgramIds;

    fetchPatientProgramIdLoop: LOOP
      FETCH cPatientProgramIds
      INTO vPatientProgramId;
      IF done
      THEN
        LEAVE fetchPatientProgramIdLoop;
      END IF;
      CALL createEpisodeAndMap(vPatientProgramId);
    END LOOP;

    CLOSE cPatientProgramIds;

    INSERT INTO episode_encounter (episode_id, encounter_id) (
      SELECT epp.episode_id, e.encounter_id from episode_patient_program epp
        INNER JOIN patient_program pp
          ON pp.patient_program_id = epp.patient_program_id
        INNER JOIN encounter e ON
                                 e.patient_id = pp.patient_id
                                 AND e.encounter_datetime >= pp.date_enrolled
                                 AND (e.encounter_datetime <= pp.date_completed OR pp.date_completed IS NULL)
    );

  END;
//
DELIMITER ;
CALL mapPatientProgramToEpisode();

DROP PROCEDURE IF EXISTS mapPatientProgramToEpisode;
DROP PROCEDURE IF EXISTS createEpisodeAndMap;
