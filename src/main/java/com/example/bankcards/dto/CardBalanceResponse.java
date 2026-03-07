package com.example.bankcards.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
@Getter
@Builder
public class CardBalanceResponse {
    private Long cardId;
    private BigDecimal balance;
}
