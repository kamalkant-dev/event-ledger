package com.kp.eventledger.account.service;

import com.kp.eventledger.account.entity.Account;
import com.kp.eventledger.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository repository;

    public void applyTransaction(String accountId, String type, Double amount) {
        log.info("Applying transaction: accountId={}, type={}, amount={}", accountId, type, amount);
        Account account = repository.findById(accountId)
                .orElse(new Account(accountId, 0.0));

        if ("CREDIT".equalsIgnoreCase(type)) {
            account.setBalance(account.getBalance() + amount);
        } else if ("DEBIT".equalsIgnoreCase(type)) {
            account.setBalance(account.getBalance() - amount);
        }

        repository.save(account);
        log.info("Updated balance for accountId={} is {}", accountId, account.getBalance());
    }

    public Double getBalance(String accountId) {
        return repository.findById(accountId)
                .map(Account::getBalance)
                .orElse(0.0);
    }
}