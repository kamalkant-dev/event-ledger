package com.kp.eventledger.gateway.service;

import com.kp.eventledger.gateway.entity.Event;
import com.kp.eventledger.gateway.repository.EventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EventServiceTraceTest {

    @Test
    void shouldPropagateTraceIdToAccountService() {

        EventRepository repository = mock(EventRepository.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        EventService service = new EventService(repository, restTemplate, meterRegistry);

        Event event = new Event("evt-1", "acct-1", "CREDIT", 100.0, "2026");

        when(repository.findById(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenReturn(event);

        // Set traceId in MDC
        MDC.put("traceId", "test-trace-123");

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);

        service.createEvent(event);

        verify(restTemplate).postForEntity(
                anyString(),
                captor.capture(),
                eq(Void.class)
        );

        HttpHeaders headers = captor.getValue().getHeaders();

        assertThat(headers.getFirst("X-Trace-Id"))
                .isEqualTo("test-trace-123");

        MDC.clear();
    }
}