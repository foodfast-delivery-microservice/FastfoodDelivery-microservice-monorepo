package com.example.userservice.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Application service for serializing event payloads.
 * This service encapsulates Jackson ObjectMapper usage,
 * keeping use cases free from infrastructure dependencies.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventPayloadSerializer {

    private final ObjectMapper objectMapper;

    /**
     * Serialize an object to JSON string for event payload
     * @param payloadObject The object to serialize
     * @return JSON string representation
     * @throws RuntimeException if serialization fails
     */
    public String serialize(Object payloadObject) {
        try {
            return objectMapper.writeValueAsString(payloadObject);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event payload: {}", payloadObject, e);
            throw new RuntimeException("Failed to serialize event payload", e);
        }
    }
}
