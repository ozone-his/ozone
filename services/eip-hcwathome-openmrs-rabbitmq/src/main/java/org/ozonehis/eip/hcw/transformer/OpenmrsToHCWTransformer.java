package org.ozonehis.eip.hcw.transformer;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OpenmrsToHCWTransformer implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        // Basic mapping: OpenMRS Debezium event to HCW@Home format
        log.info("Transforming OpenMRS event to HCW@Home format");

        // Example: Extracting patient info from Debezium payload
        // In Ozone, we might look for specific encounter types (e.g., Teleconsultation)
        // String ENCOUNTER_TYPE_TELECONSULTATION = "d7151dce-416b-4395-9762-817a783788a8";

        Object body = exchange.getIn().getBody();
        // Transformation logic goes here
        exchange.getIn().setBody(body);
    }
}
