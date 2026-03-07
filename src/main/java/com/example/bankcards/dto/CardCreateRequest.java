package com.example.bankcards.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.YearMonth;
@Getter
@Setter
public class CardCreateRequest {

    @NotNull
    private Long userId;

    @NotBlank
    private String cardNumber;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM")
    private YearMonth expiryDate;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal initialBalance;
}
