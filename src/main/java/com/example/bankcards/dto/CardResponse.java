package com.example.bankcards.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;
import com.example.bankcards.entity.CardStatus;

import java.math.BigDecimal;
import java.time.YearMonth;
@Getter
@Builder
public class CardResponse {
    private Long id;
    private String maskedNumber;
    private String ownerName;

    @JsonFormat(pattern = "yyyy-MM")
    private YearMonth expiryDate;

    private CardStatus status;
    private BigDecimal balance;
    private Long userId;
    private boolean blockRequested;
}
