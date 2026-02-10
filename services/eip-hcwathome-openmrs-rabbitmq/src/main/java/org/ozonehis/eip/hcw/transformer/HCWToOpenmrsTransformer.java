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
        Object body = exchange.getIn().getBody();
        exchange.getIn().setBody(body);
    }
}
