package com.kp.eventledger.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kp.eventledger.gateway.entity.Event;
import com.kp.eventledger.gateway.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EventIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    void shouldProcessFullFlowSuccessfully() throws Exception {

        Event event = new Event(
                "evt-int-1",
                "acct-1",
                "CREDIT",
                200.0,
                "2026"
        );

        when(restTemplate.postForEntity(
                anyString(),
                any(),
                eq(Void.class)
        )).thenReturn(new ResponseEntity<>(HttpStatus.OK));

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk());

        assertTrue(eventRepository.findById("evt-int-1").isPresent());
    }

    @Test
    void shouldReturn503WhenAccountServiceFails() throws Exception {

        Event event = new Event(
                "evt-fail-1",
                "acct-1",
                "CREDIT",
                100.0,
                "2026"
        );

        when(restTemplate.postForEntity(
                anyString(),
                any(),
                eq(Void.class)
        )).thenThrow(new ResourceAccessException("Account service down"));

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void shouldReturn400ForInvalidAmountBeforeCallingAccountService() throws Exception {

        Event event = new Event(
                "evt-invalid-1",
                "acct-1",
                "CREDIT",
                0.0,
                "2026"
        );

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isBadRequest());

        verify(restTemplate, never())
                .postForEntity(anyString(), any(), eq(Void.class));
    }
}