package com.kp.eventledger.gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kp.eventledger.gateway.entity.Event;
import com.kp.eventledger.gateway.exception.ServiceUnavailableException;
import com.kp.eventledger.gateway.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateEventSuccessfully() throws Exception {

        Event event = new Event("evt-1", "acct-1", "CREDIT", 100.0, "2026");

        when(eventService.createEvent(any())).thenReturn(event);

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("evt-1"));
    }

    @Test
    void shouldFailForInvalidAmount() throws Exception {

        Event event = new Event("evt-invalid", "acct-1", "CREDIT", 0.0, "2026");

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFailForInvalidEventType() throws Exception {

        Event event = new Event("evt-invalid-type", "acct-1", "TRANSFER", 100.0, "2026");

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn503WhenServiceDown() throws Exception {

        Event event = new Event("evt-2", "acct-1", "CREDIT", 100.0, "2026");

        when(eventService.createEvent(any()))
                .thenThrow(new ServiceUnavailableException("Service Down"));

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void shouldReturnEventById() throws Exception {

        Event event = new Event("evt-1", "acct-1", "CREDIT", 100.0, "2026");

        when(eventService.getEvent("evt-1"))
                .thenReturn(Optional.of(event));

        mockMvc.perform(get("/events/evt-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("evt-1"))
                .andExpect(jsonPath("$.accountId").value("acct-1"));
    }

    @Test
    void shouldReturn404WhenEventNotFound() throws Exception {

        when(eventService.getEvent("evt-404"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/events/evt-404"))
                .andExpect(status().isNotFound());
    }
}