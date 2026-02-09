package com.ozonehis.imisconnect.service.rabbitmq;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class RabbitMQListener {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queues = "openmrs.patient.registered")
    public void handlePatientRegistered(String message) {
        try {
            System.out.println("Patient registered event received: " + message);
        } catch (Exception e) {
            System.error("Error processing patient registered event: " + e.getMessage());
        }
    }

    @RabbitListener(queues = "openmrs.encounter.created")
    public void handleEncounterCreated(String message) {
        try {
            System.out.println("Encounter created event received: " + message);
        } catch (Exception e) {
            System.error("Error processing encounter created event: " + e.getMessage());
        }
    }

    @RabbitListener(queues = "openmrs.claim.status.updated")
    public void handleClaimStatusUpdated(String message) {
        try {
            System.out.println("Claim status updated event received: " + message);
        } catch (Exception e) {
            System.error("Error processing claim status updated event: " + e.getMessage());
        }
    }
}
