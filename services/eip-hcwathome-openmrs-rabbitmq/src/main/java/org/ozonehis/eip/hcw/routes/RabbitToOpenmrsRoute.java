package org.ozonehis.eip.hcw.routes;

import org.apache.camel.builder.RouteBuilder;
import org.ozonehis.eip.hcw.transformer.HCWToOpenmrsTransformer;
import org.springframework.stereotype.Component;

@Component
public class RabbitToOpenmrsRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        // Error handling: if a message fails, it will be retried via RabbitMQ DLX
        errorHandler(deadLetterChannel("log:error?level=ERROR")
                .maximumRedeliveries(3)
                .redeliveryDelay(5000));

        from("spring-rabbitmq:{{eip.exchange.hcwathome}}?queues={{eip.queue.hcwathome.in}}&routingKey=hcwathome.events")
            .routeId("RabbitToOpenmrsRoute")
            .bean(HCWToOpenmrsTransformer.class)
            .to("bean:openmrsRestClient?method=postEncounter");
    }
}
