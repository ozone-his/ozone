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
        // In a real scenario, this would involve mapping Debezium's complex JSON to HCW@Home's API format
        log.info("Transforming OpenMRS event to HCW@Home format");
        Object body = exchange.getIn().getBody();
        exchange.getIn().setBody(body);
    }
}
