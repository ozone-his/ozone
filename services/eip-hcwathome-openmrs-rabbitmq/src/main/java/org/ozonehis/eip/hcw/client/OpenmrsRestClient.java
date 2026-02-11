package org.ozonehis.eip.hcw.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;

@Service("openmrsRestClient")
@Slf4j
public class OpenmrsRestClient {

    @Value("${openmrs.api.url}")
    private String openmrsApiUrl;

    @Value("${openmrs.api.username}")
    private String username;

    @Value("${openmrs.api.password}")
    private String password;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void init() {
        restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(username, password));
    }

    public void postEncounter(Object encounter) {
        log.info("Posting encounter to OpenMRS: {}", encounter);
        restTemplate.postForObject(openmrsApiUrl + "/encounter", encounter, String.class);
    }
}
