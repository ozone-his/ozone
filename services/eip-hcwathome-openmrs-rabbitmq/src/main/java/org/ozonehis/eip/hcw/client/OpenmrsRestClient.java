package org.ozonehis.eip.hcw.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service("openmrsRestClient")
@Slf4j
public class OpenmrsRestClient {

    @Value("${openmrs.api.url}")
    private String openmrsApiUrl;

    private final WebClient webClient;

    public OpenmrsRestClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public void postEncounter(Object encounter) {
        log.info("Posting encounter to OpenMRS: {}", encounter);
        webClient.post()
            .uri(openmrsApiUrl + "/encounter")
            .bodyValue(encounter)
            .retrieve()
            .bodyToMono(String.class)
            .block();
    }
}
