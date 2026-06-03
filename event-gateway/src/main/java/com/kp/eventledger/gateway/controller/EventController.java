package com.kp.eventledger.gateway.controller;

import com.kp.eventledger.gateway.entity.Event;
import com.kp.eventledger.gateway.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Slf4j
public class EventController {

    private final EventService service;

    @PostMapping
    public ResponseEntity<Event> createEvent(@RequestBody Event event) {
        log.info("Received event request: eventId={}", event.getEventId());
        return ResponseEntity.ok(service.createEvent(event));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getEvent(@PathVariable String id) {
        return service.getEvent(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Event>> getEvents(@RequestParam String account) {
        return ResponseEntity.ok(service.getEvents(account));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("UP");
    }
}