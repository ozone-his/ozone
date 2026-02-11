package org.ozonehis.eip.hcw.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class OpenmrsDebeziumRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        errorHandler(defaultErrorHandler()
                .maximumRedeliveries(5)
                .redeliveryDelay(10000));

        from("debezium-mysql:{{openmrs.db.host}}?" +
                "databaseHostname={{openmrs.db.host}}&" +
                "databasePort={{openmrs.db.port}}&" +
                "databaseUser={{openmrs.db.user}}&" +
                "databasePassword={{openmrs.db.password}}&" +
                "databaseServerId={{openmrs.db.serverId}}&" +
                "databaseServerName=openmrs&" +
                "databaseIncludeList={{openmrs.db.name}}&" +
                "tableIncludeList=openmrs.patient,openmrs.encounter&" +
                "databaseHistoryFileFilename={{debezium.history.file:/var/lib/eip/dbhistory.dat}}")
            .routeId("OpenmrsDebeziumRoute")
            .to("direct:openmrs-events");
    }
}
