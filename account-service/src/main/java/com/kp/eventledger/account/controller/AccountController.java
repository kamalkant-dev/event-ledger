package com.kp.eventledger.account.controller;

import com.kp.eventledger.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.extern.slf4j.XSlf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService service;

    @PostMapping("/{accountId}/transactions")
    public ResponseEntity<Void> applyTransaction(
            @PathVariable String accountId,
            @RequestBody Map<String, Object> request) {

        log.info("Received transaction request for accountId={}", accountId);
        String type = (String) request.get("type");
        Double amount = Double.valueOf(request.get("amount").toString());

        service.applyTransaction(accountId, type, amount);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<Double> getBalance(@PathVariable String accountId) {
        return ResponseEntity.ok(service.getBalance(accountId));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("UP");
    }
}