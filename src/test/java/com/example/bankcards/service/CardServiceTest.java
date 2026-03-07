package com.example.bankcards.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import com.example.bankcards.dto.CardBalanceResponse;
import com.example.bankcards.dto.CardCreateRequest;
import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.exception.ForbiddenOperationException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardCryptoService;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardCryptoService cardCryptoService;

    @InjectMocks
    private CardService cardService;

    @Test
    void createCardShouldSaveActiveCardAndReturnMaskedNumber() {
        com.example.bankcards.entity.User owner = com.example.bankcards.entity.User.builder()
                .id(10L)
                .fullName("Test User")
                .build();

        CardCreateRequest request = new CardCreateRequest();
        request.setUserId(10L);
        request.setCardNumber("4111 1111 1111 1234");
        request.setExpiryDate(YearMonth.now().plusMonths(12));
        request.setInitialBalance(new BigDecimal("5000.00"));

        when(userRepository.findById(10L)).thenReturn(Optional.of(owner));
        when(cardCryptoService.encrypt("4111111111111234")).thenReturn("ENC");
        when(cardRepository.existsByEncryptedNumber("ENC")).thenReturn(false);
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> {
            Card card = invocation.getArgument(0);
            card.setId(100L);
            return card;
        });

        CardResponse response = cardService.createCard(request);

        assertEquals(100L, response.getId());
        assertEquals("**** **** **** 1234", response.getMaskedNumber());
        assertEquals(CardStatus.ACTIVE, response.getStatus());
        assertEquals(10L, response.getUserId());

        ArgumentCaptor<Card> captor = ArgumentCaptor.forClass(Card.class);
        verify(cardRepository).save(captor.capture());
        assertEquals("ENC", captor.getValue().getEncryptedNumber());
        assertEquals("1234", captor.getValue().getLast4());
    }

    @Test
    void createCardShouldThrowWhenCardNumberInvalid() {
        com.example.bankcards.entity.User owner = com.example.bankcards.entity.User.builder().id(10L).build();
        CardCreateRequest request = new CardCreateRequest();
        request.setUserId(10L);
        request.setCardNumber("123");
        request.setExpiryDate(YearMonth.now().plusMonths(1));
        request.setInitialBalance(new BigDecimal("100.00"));

        when(userRepository.findById(10L)).thenReturn(Optional.of(owner));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> cardService.createCard(request));
        assertTrue(ex.getMessage().contains("16 digits"));
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void requestBlockShouldThrowWhenCardBelongsToAnotherUser() {
        com.example.bankcards.entity.User user = com.example.bankcards.entity.User.builder()
                .id(10L)
                .email("user@test.com")
                .build();

        Card card = Card.builder()
                .id(200L)
                .userId(99L)
                .status(CardStatus.ACTIVE)
                .expiryDate(YearMonth.now().plusMonths(6))
                .balance(new BigDecimal("1000.00"))
                .blockRequested(false)
                .last4("1111")
                .encryptedNumber("ENC")
                .ownerName("Another")
                .build();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(cardRepository.findById(200L)).thenReturn(Optional.of(card));

        assertThrows(ForbiddenOperationException.class, () -> cardService.requestBlock(200L, "user@test.com"));
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void requestBlockShouldSetRequestedFlagButNotBlockCardImmediately() {
        com.example.bankcards.entity.User user = com.example.bankcards.entity.User.builder()
                .id(10L)
                .email("user@test.com")
                .build();

        Card card = Card.builder()
                .id(201L)
                .userId(10L)
                .status(CardStatus.ACTIVE)
                .expiryDate(YearMonth.now().plusMonths(6))
                .balance(new BigDecimal("1000.00"))
                .blockRequested(false)
                .last4("1234")
                .encryptedNumber("ENC")
                .ownerName("Owner")
                .build();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(cardRepository.findById(201L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CardResponse response = cardService.requestBlock(201L, "user@test.com");

        assertEquals(CardStatus.ACTIVE, response.getStatus());
        assertTrue(response.isBlockRequested());
        verify(cardRepository).save(eq(card));
    }

    @Test
    void requestBlockShouldThrowForExpiredCard() {
        com.example.bankcards.entity.User user = com.example.bankcards.entity.User.builder()
                .id(10L)
                .email("user@test.com")
                .build();

        Card card = Card.builder()
                .id(202L)
                .userId(10L)
                .status(CardStatus.ACTIVE)
                .expiryDate(YearMonth.now().minusMonths(1))
                .balance(new BigDecimal("1000.00"))
                .blockRequested(false)
                .last4("1234")
                .encryptedNumber("ENC")
                .ownerName("Owner")
                .build();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(cardRepository.findById(202L)).thenReturn(Optional.of(card));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> cardService.requestBlock(202L, "user@test.com"));
        assertEquals("Cannot request block for expired card", ex.getMessage());
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void activateCardByAdminShouldThrowForExpiredCard() {
        Card card = Card.builder()
                .id(300L)
                .status(CardStatus.ACTIVE)
                .expiryDate(YearMonth.now().minusMonths(1))
                .build();

        when(cardRepository.findById(300L)).thenReturn(Optional.of(card));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> cardService.activateCardByAdmin(300L));
        assertEquals("Cannot activate expired card", ex.getMessage());
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void blockCardByAdminShouldSetBlockedAndClearRequestFlag() {
        Card card = Card.builder()
                .id(301L)
                .status(CardStatus.ACTIVE)
                .blockRequested(true)
                .expiryDate(YearMonth.now().plusMonths(1))
                .build();

        when(cardRepository.findById(301L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CardResponse response = cardService.blockCardByAdmin(301L);

        assertEquals(CardStatus.BLOCKED, response.getStatus());
        assertFalse(response.isBlockRequested());
        verify(cardRepository).save(eq(card));
    }

    @Test
    void getMyCardBalanceShouldMarkCardExpiredAndPersist() {
        com.example.bankcards.entity.User user = com.example.bankcards.entity.User.builder()
                .id(10L)
                .email("user@test.com")
                .build();

        Card card = Card.builder()
                .id(400L)
                .userId(10L)
                .status(CardStatus.ACTIVE)
                .expiryDate(YearMonth.now().minusMonths(1))
                .balance(new BigDecimal("777.77"))
                .build();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(cardRepository.findById(400L)).thenReturn(Optional.of(card));

        CardBalanceResponse response = cardService.getMyCardBalance(400L, "user@test.com");

        assertEquals(400L, response.getCardId());
        assertEquals(new BigDecimal("777.77"), response.getBalance());
        assertEquals(CardStatus.EXPIRED, card.getStatus());
        verify(cardRepository).save(eq(card));
    }

    @Test
    void getBlockRequestsShouldReturnOnlyRequestedCards() {
        Card requested = Card.builder()
                .id(500L)
                .userId(10L)
                .status(CardStatus.ACTIVE)
                .blockRequested(true)
                .expiryDate(YearMonth.now().plusMonths(2))
                .balance(new BigDecimal("1.00"))
                .last4("0001")
                .ownerName("User One")
                .encryptedNumber("ENC1")
                .build();

        when(cardRepository.findByBlockRequestedTrue(PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(requested)));

        var page = cardService.getBlockRequests(PageRequest.of(0, 20));

        assertEquals(1, page.getTotalElements());
        assertTrue(page.getContent().get(0).isBlockRequested());
    }

    @Test
    void rejectBlockRequestByAdminShouldClearRequestedFlag() {
        Card requested = Card.builder()
                .id(501L)
                .userId(10L)
                .status(CardStatus.ACTIVE)
                .blockRequested(true)
                .expiryDate(YearMonth.now().plusMonths(2))
                .balance(new BigDecimal("10.00"))
                .last4("0002")
                .ownerName("User Two")
                .encryptedNumber("ENC2")
                .build();

        when(cardRepository.findById(501L)).thenReturn(Optional.of(requested));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CardResponse response = cardService.rejectBlockRequestByAdmin(501L);

        assertFalse(response.isBlockRequested());
        assertEquals(CardStatus.ACTIVE, response.getStatus());
        verify(cardRepository).save(eq(requested));
    }
}
