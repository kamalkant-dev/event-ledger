package com.kp.eventledger.gateway.service;

import com.kp.eventledger.gateway.entity.Event;
import com.kp.eventledger.gateway.exception.ServiceUnavailableException;
import com.kp.eventledger.gateway.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.slf4j.MDC;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.web.client.RestTemplate;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.Optional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository repository;
    private final RestTemplate restTemplate;
    private final MeterRegistry meterRegistry;
    @CircuitBreaker(name = "accountService", fallbackMethod = "fallbackCreateEvent")
    public Event createEvent(Event event) {

        log.info("Processing event: eventId={}, accountId={}",
                event.getEventId(), event.getAccountId());

        // STEP 1: VALIDATION FIRST
        if (event.getAmount() == null || event.getAmount() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }

        // STEP 2: Idempotency
        Optional<Event> existingEvent = repository.findById(event.getEventId());
        if (existingEvent.isPresent()) {
            log.warn("Duplicate event detected: eventId={}", event.getEventId());
            return existingEvent.get();
        }

        try {
            String url = "http://localhost:8081/accounts/"
                    + event.getAccountId() + "/transactions";

            Map<String, Object> request = Map.of(
                    "type", event.getType(),
                    "amount", event.getAmount()
            );

            String traceId = MDC.get("traceId");

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Trace-Id", traceId);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            log.info("Calling Account Service for accountId={}", event.getAccountId());

            restTemplate.postForEntity(url, entity, Void.class);

            //STEP 3: Save event
            Event savedEvent = repository.save(event);

            log.info("Event stored successfully: eventId={}", savedEvent.getEventId());

            //METRIC: success count
            meterRegistry.counter("events.processed.count").increment();

            return savedEvent;

        } catch (RestClientException ex) {

            meterRegistry.counter("events.failed.count").increment();

            throw new ServiceUnavailableException(
                    "Account Service is unavailable. Please try again later."
            );
        }
    }

    public Optional<Event> getEvent(String id) {
        return repository.findById(id);
    }

    public List<Event> getEvents(String accountId) {
        return repository.findByAccountIdOrderByEventTimestamp(accountId);
    }
    public Event fallbackCreateEvent(Event event, Exception ex) {

        meterRegistry.counter("events.failed.count").increment();

        throw new ServiceUnavailableException(
                "Account Service temporarily unavailable (circuit breaker open)"
        );
    }
}