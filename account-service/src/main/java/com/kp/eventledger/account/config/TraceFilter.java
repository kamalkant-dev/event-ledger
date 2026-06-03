package com.kp.eventledger.account.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class TraceFilter implements jakarta.servlet.Filter {

    private static final String TRACE_ID = "traceId";
    private static final String HEADER_NAME = "X-Trace-Id";

    @Override
    public void doFilter(
            jakarta.servlet.ServletRequest request,
            jakarta.servlet.ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;

            String traceId = httpRequest.getHeader(HEADER_NAME);

            if (traceId == null || traceId.isEmpty()) {
                traceId = UUID.randomUUID().toString();
            }
            MDC.put(TRACE_ID, traceId);

            chain.doFilter(request, response);

        } finally {
            MDC.clear();
        }
    }
}