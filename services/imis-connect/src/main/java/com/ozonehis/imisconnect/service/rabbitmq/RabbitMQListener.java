package com.ozonehis.imisconnect.service.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class RabbitMQListener {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queues = "openmrs.patient.registered")
    public void handlePatientRegistered(String message) {
        try {
            log.info("Patient registered event received: {}", message);
        } catch (Exception e) {
            log.error("Error processing patient registered event: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "openmrs.encounter.created")
    public void handleEncounterCreated(String message) {
        try {
            log.info("Encounter created event received: {}", message);
        } catch (Exception e) {
            log.error("Error processing encounter created event: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "openmrs.claim.status.updated")
    public void handleClaimStatusUpdated(String message) {
        try {
            log.info("Claim status updated event received: {}", message);
        } catch (Exception e) {
            log.error("Error processing claim status updated event: {}", e.getMessage(), e);
        }
    }
}
