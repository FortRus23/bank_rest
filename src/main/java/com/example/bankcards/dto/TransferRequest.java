package com.example.bankcards.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TransferRequest {

    @NotNull
    private Long fromCardId;

    @NotNull
    private Long toCardId;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;
}
