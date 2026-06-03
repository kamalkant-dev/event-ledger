package com.kp.eventledger.gateway.service;

import com.kp.eventledger.gateway.entity.Event;
import com.kp.eventledger.gateway.repository.EventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventServiceTest {

    private final EventRepository repository = mock(EventRepository.class);
    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private final EventService service =
            new EventService(repository, restTemplate, meterRegistry);

    @Test
    void shouldThrowExceptionForInvalidAmount() {

        Event event = new Event("evt-1", "acct-1", "CREDIT", 0.0, "time");

        assertThrows(IllegalArgumentException.class,
                () -> service.createEvent(event));
    }

    @Test
    void shouldReturnExistingEventForDuplicate() {

        Event event = new Event("evt-1", "acct-1", "CREDIT", 100.0, "time");

        when(repository.findById("evt-1")).thenReturn(Optional.of(event));

        Event result = service.createEvent(event);

        assertEquals("evt-1", result.getEventId());
    }
}