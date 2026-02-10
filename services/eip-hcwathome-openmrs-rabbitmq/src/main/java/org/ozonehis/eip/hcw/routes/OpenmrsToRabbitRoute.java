package org.ozonehis.eip.hcw.routes;

import org.apache.camel.builder.RouteBuilder;
import org.ozonehis.eip.hcw.transformer.OpenmrsToHCWTransformer;
import org.springframework.stereotype.Component;

@Component
public class OpenmrsToRabbitRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("direct:openmrs-events")
            .routeId("OpenmrsToRabbitRoute")
            .bean(OpenmrsToHCWTransformer.class)
            .to("spring-rabbitmq:{{eip.exchange.hcwathome}}?routingKey=openmrs.events");
    }
}
