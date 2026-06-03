package com.kp.eventledger.gateway.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    private String eventId;

    private String accountId;
    private String type;
    private Double amount;
    private String eventTimestamp;
}