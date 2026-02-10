package org.ozonehis.eip.hcw.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service("openmrsRestClient")
@Slf4j
public class OpenmrsRestClient {

    @Value("${openmrs.api.url}")
    private String openmrsApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public void postEncounter(Object encounter) {
        log.info("Posting encounter to OpenMRS: {}", encounter);
        restTemplate.postForObject(openmrsApiUrl + "/encounter", encounter, String.class);
    }
}
