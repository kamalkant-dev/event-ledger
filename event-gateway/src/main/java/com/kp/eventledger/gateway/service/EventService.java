package com.kp.eventledger.gateway.service;

import com.kp.eventledger.gateway.entity.Event;
import com.kp.eventledger.gateway.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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

        log.info("Processing event: eventId={}, accountId={}", event.getEventId(), event.getAccountId());
        String url = "http://localhost:8081/accounts/"
                + event.getAccountId() + "/transactions";

        Map<String, Object> request = Map.of(
                "type", event.getType(),
                "amount", event.getAmount()
        );

        restTemplate.postForEntity(url, request, Void.class);
        Event saved = repository.save(event);
        log.info("Event stored successfully: eventId={}", saved.getEventId());
        return repository.save(event);
    }

    public Optional<Event> getEvent(String id) {
        return repository.findById(id);
    }

    public List<Event> getEvents(String accountId) {
        return repository.findByAccountIdOrderByEventTimestamp(accountId);
    }
}