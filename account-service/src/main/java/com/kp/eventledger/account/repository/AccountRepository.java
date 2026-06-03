package com.kp.eventledger.account.repository;

import com.kp.eventledger.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {
}