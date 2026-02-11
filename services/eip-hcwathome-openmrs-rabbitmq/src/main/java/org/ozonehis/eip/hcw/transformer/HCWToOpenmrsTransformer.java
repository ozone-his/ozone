package org.ozonehis.eip.hcw.transformer;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class HCWToOpenmrsTransformer implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        // Basic mapping: HCW@Home event to OpenMRS format
        log.info("Transforming HCW@Home event to OpenMRS format");

        // Example: Creating an OpenMRS Encounter JSON
        /*
        {
          "patient": "PATIENT_UUID",
          "encounterType": "TELECONSULTATION_UUID",
          "location": "LOCATION_UUID",
          "encounterDatetime": "2023-10-27T10:00:00.000+0000",
          "provider": "PROVIDER_UUID"
        }
        */

        Object body = exchange.getIn().getBody();
        // Transformation logic goes here
        exchange.getIn().setBody(body);
    }
}
