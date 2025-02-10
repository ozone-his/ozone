select  ifnull(sum(if(visit_type_name='OPD' and date_started=date_created,1,0)),0) as New_OPD,
        ifnull(sum(if(visit_type_name='OPD' and date_started!=date_created,1,0)),0) as Old_OPD,
        ifnull(sum(if(visit_type_name='OPD',1,0)),0) as Total_OPD,
        ifnull(sum(if(visit_type_name='EMERGENCY' and date_started=date_created,1,0)),0) as New_Emergency,
        ifnull(sum(if(visit_type_name='EMERGENCY' and date_started!=date_created,1,0)),0) as Old_Emergency,
        ifnull(sum(if(visit_type_name='EMERGENCY',1,0)),0) as Total_Emergency
        from
        (select v.visit_id, vt.name as visit_type_name, p.person_id, date(v.date_started) as date_started, date(p.date_created) as date_created from visit v
            inner join person p on p.person_id=v.patient_id
            inner join visit_type vt on vt.visit_type_id=v.visit_type_id
            where cast(v.date_started as date) between '#startDate#' and '#endDate#') as raw_result;