package com.kp.eventledger.gateway.service;

import com.kp.eventledger.gateway.entity.Event;
import com.kp.eventledger.gateway.exception.ServiceUnavailableException;
import com.kp.eventledger.gateway.repository.EventRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository repository;
    private final RestTemplate restTemplate;
    private final MeterRegistry meterRegistry;

    @Value("${account.service.url}")
    private String accountServiceUrl;

    @CircuitBreaker(name = "accountService", fallbackMethod = "fallbackCreateEvent")
    public Event createEvent(Event event) {

        log.info("Processing event: eventId={}, accountId={}",
                event.getEventId(), event.getAccountId());

        if (event.getAmount() == null || event.getAmount() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }

        if (!"CREDIT".equals(event.getType()) && !"DEBIT".equals(event.getType())) {
            throw new IllegalArgumentException("Event type must be CREDIT or DEBIT");
        }

        Optional<Event> existingEvent = repository.findById(event.getEventId());
        if (existingEvent.isPresent()) {
            log.warn("Duplicate event detected: eventId={}", event.getEventId());
            return existingEvent.get();
        }

        try {
            String url = accountServiceUrl + "/accounts/"
                    + event.getAccountId()
                    + "/transactions";

            Map<String, Object> request = Map.of(
                    "type", event.getType(),
                    "amount", event.getAmount()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Trace-Id", MDC.get("traceId"));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            log.info("Calling Account Service for accountId={}", event.getAccountId());

            restTemplate.postForEntity(url, entity, Void.class);

            Event savedEvent = repository.save(event);

            meterRegistry.counter("events.processed.count").increment();

            log.info("Event stored successfully: eventId={}", savedEvent.getEventId());

            return savedEvent;

        } catch (RestClientException ex) {
            log.error("Failed to call Account Service for eventId={}", event.getEventId(), ex);

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
        log.error("Circuit breaker triggered for eventId={}", event.getEventId(), ex);

        meterRegistry.counter("events.failed.count").increment();

        throw new ServiceUnavailableException(
                "Account Service temporarily unavailable (circuit breaker open)"
        );
    }
}