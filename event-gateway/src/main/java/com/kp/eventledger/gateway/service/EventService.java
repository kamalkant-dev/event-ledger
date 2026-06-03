package com.kp.eventledger.gateway.service;

import com.kp.eventledger.gateway.entity.Event;
import com.kp.eventledger.gateway.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.slf4j.MDC;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository repository;
    private final RestTemplate restTemplate;

    public Event createEvent(Event event) {

        log.info("Processing event: eventId={}, accountId={}",
                event.getEventId(), event.getAccountId());

        //STEP 1: Idempotency check
        Optional<Event> existingEvent = repository.findById(event.getEventId());

        if (existingEvent.isPresent()) {
            log.warn("Duplicate event detected: eventId={}", event.getEventId());
            return existingEvent.get(); // return existing event, DO NOT process again
        }

        //STEP 2: Call Account Service
        String url = "http://localhost:8081/accounts/"
                + event.getAccountId() + "/transactions";

        Map<String, Object> request = Map.of(
                "type", event.getType(),
                "amount", event.getAmount()
        );

        log.info("Calling Account Service for accountId={}", event.getAccountId());
        String traceId = MDC.get("traceId");

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Trace-Id", traceId);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        restTemplate.postForEntity(url, entity, Void.class);

        // STEP 3: Save event ONLY once
        Event savedEvent = repository.save(event);

        log.info("Event stored successfully: eventId={}", savedEvent.getEventId());

        return savedEvent;
    }

    public Optional<Event> getEvent(String id) {
        log.info("Fetching event by id={}", id);
        return repository.findById(id);
    }

    public List<Event> getEvents(String accountId) {
        log.info("Fetching events for accountId={}", accountId);
        return repository.findByAccountIdOrderByEventTimestamp(accountId);
    }
}