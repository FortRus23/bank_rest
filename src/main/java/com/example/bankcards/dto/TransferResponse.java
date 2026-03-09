package com.example.bankcards.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class TransferResponse {
    private Long fromCardId;
    private Long toCardId;
    private BigDecimal amount;
    private BigDecimal fromCardBalance;
    private BigDecimal toCardBalance;
}
