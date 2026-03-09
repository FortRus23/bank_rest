package com.example.bankcards.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.dto.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.ForbiddenOperationException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransferService transferService;

    @Test
    void transferBetweenOwnCardsShouldSucceed() {
        User user = User.builder().id(10L).email("user@test.com").build();
        Card from = Card.builder()
                .id(1L)
                .userId(10L)
                .status(CardStatus.ACTIVE)
                .expiryDate(YearMonth.now().plusMonths(6))
                .balance(new BigDecimal("1000.00"))
                .build();
        Card to = Card.builder()
                .id(2L)
                .userId(10L)
                .status(CardStatus.ACTIVE)
                .expiryDate(YearMonth.now().plusMonths(6))
                .balance(new BigDecimal("100.00"))
                .build();

        TransferRequest request = new TransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(2L);
        request.setAmount(new BigDecimal("250.00"));

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(from));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(to));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransferResponse response = transferService.transferBetweenOwnCards(request, "user@test.com");

        assertEquals(new BigDecimal("750.00"), response.getFromCardBalance());
        assertEquals(new BigDecimal("350.00"), response.getToCardBalance());
        verify(cardRepository, times(2)).save(any(Card.class));
    }

    @Test
    void transferShouldFailWhenSameCard() {
        User user = User.builder().id(10L).email("user@test.com").build();
        TransferRequest request = new TransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(1L);
        request.setAmount(new BigDecimal("10.00"));

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transferService.transferBetweenOwnCards(request, "user@test.com"));
        assertEquals("Source and destination cards must be different", ex.getMessage());
        verify(cardRepository, never()).findById(any());
    }

    @Test
    void transferShouldFailWhenForeignCardUsed() {
        User user = User.builder().id(10L).email("user@test.com").build();
        Card from = Card.builder().id(1L).userId(10L).status(CardStatus.ACTIVE).expiryDate(YearMonth.now().plusMonths(6)).balance(new BigDecimal("100.00")).build();
        Card to = Card.builder().id(2L).userId(99L).status(CardStatus.ACTIVE).expiryDate(YearMonth.now().plusMonths(6)).balance(new BigDecimal("100.00")).build();

        TransferRequest request = new TransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(2L);
        request.setAmount(new BigDecimal("10.00"));

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(from));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(to));

        assertThrows(ForbiddenOperationException.class,
                () -> transferService.transferBetweenOwnCards(request, "user@test.com"));
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void transferShouldFailWhenCardNotActive() {
        User user = User.builder().id(10L).email("user@test.com").build();
        Card from = Card.builder().id(1L).userId(10L).status(CardStatus.BLOCKED).expiryDate(YearMonth.now().plusMonths(6)).balance(new BigDecimal("100.00")).build();
        Card to = Card.builder().id(2L).userId(10L).status(CardStatus.ACTIVE).expiryDate(YearMonth.now().plusMonths(6)).balance(new BigDecimal("100.00")).build();

        TransferRequest request = new TransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(2L);
        request.setAmount(new BigDecimal("10.00"));

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(from));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(to));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transferService.transferBetweenOwnCards(request, "user@test.com"));
        assertEquals("The source card is not active", ex.getMessage());
    }

    @Test
    void transferShouldFailWhenInsufficientFunds() {
        User user = User.builder().id(10L).email("user@test.com").build();
        Card from = Card.builder().id(1L).userId(10L).status(CardStatus.ACTIVE).expiryDate(YearMonth.now().plusMonths(6)).balance(new BigDecimal("5.00")).build();
        Card to = Card.builder().id(2L).userId(10L).status(CardStatus.ACTIVE).expiryDate(YearMonth.now().plusMonths(6)).balance(new BigDecimal("100.00")).build();

        TransferRequest request = new TransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(2L);
        request.setAmount(new BigDecimal("10.00"));

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(from));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(to));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transferService.transferBetweenOwnCards(request, "user@test.com"));
        assertEquals("Insufficient funds", ex.getMessage());
    }
}
