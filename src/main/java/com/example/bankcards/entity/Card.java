package com.example.bankcards.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import com.example.bankcards.util.YearMonthAttributeConverter;

import java.math.BigDecimal;
import java.time.YearMonth;
@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "encrypted_number", nullable = false, unique = true, length = 512)
    private String encryptedNumber;

    @NotBlank
    @Size(max = 255)
    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    @NotBlank
    @Pattern(regexp = "\\d{4}")
    @Column(name = "last4", nullable = false, length = 4)
    private String last4;

    @NotNull
    @Convert(converter = YearMonthAttributeConverter.class)
    @Column(name = "expiry_date", nullable = false)
    private YearMonth expiryDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CardStatus status;

    @NotNull
    @DecimalMin(value = "0.00")
    @Digits(integer = 15, fraction = 2)
    @Column(name = "balance", nullable = false, precision = 17, scale = 2)
    private BigDecimal balance;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotNull
    @Column(name = "block_requested", nullable = false)
    private boolean blockRequested;

    public String getMaskedNumber() {
        return "**** **** **** " + last4;
    }


}
