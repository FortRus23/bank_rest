package com.example.bankcards.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.bankcards.dto.CardBalanceResponse;
import com.example.bankcards.dto.CardCreateRequest;
import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.ForbiddenOperationException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardCryptoService;
import com.example.bankcards.util.CardSpecifications;

import java.math.BigDecimal;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardCryptoService cardCryptoService;

    @Transactional
    public CardResponse createCard(CardCreateRequest request) {
        User owner = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUserId()));

        String normalizedCardNumber = normalizeCardNumber(request.getCardNumber());
        if (!normalizedCardNumber.matches("\\d{16}")) {
            throw new IllegalArgumentException("Card number must contain exactly 16 digits");
        }

        if (request.getExpiryDate().isBefore(YearMonth.now())) {
            throw new IllegalArgumentException("Expiry date cannot be in the past");
        }

        String encryptedNumber = cardCryptoService.encrypt(normalizedCardNumber);
        if (cardRepository.existsByEncryptedNumber(encryptedNumber)) {
            throw new IllegalArgumentException("Card already exists");
        }

        Card card = Card.builder()
                .encryptedNumber(encryptedNumber)
                .ownerName(owner.getFullName())
                .last4(normalizedCardNumber.substring(normalizedCardNumber.length() - 4))
                .expiryDate(request.getExpiryDate())
                .status(CardStatus.ACTIVE)
                .balance(request.getInitialBalance())
                .userId(owner.getId())
                .blockRequested(false)
                .build();

        return toResponse(cardRepository.save(card));
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> getAllCards(Long userId, CardStatus status, String last4, Pageable pageable) {
        Specification<Card> spec = Specification.where(CardSpecifications.hasUserId(userId))
                .and(CardSpecifications.hasStatus(status))
                .and(CardSpecifications.last4Contains(last4));

        return cardRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional
    public CardResponse blockCardByAdmin(Long cardId) {
        Card card = getCardById(cardId);
        syncExpiredStatus(card);

        if (card.getStatus() == CardStatus.EXPIRED) {
            throw new IllegalArgumentException("Cannot block expired card");
        }

        card.setStatus(CardStatus.BLOCKED);
        card.setBlockRequested(false);
        return toResponse(cardRepository.save(card));
    }

    @Transactional
    public CardResponse activateCardByAdmin(Long cardId) {
        Card card = getCardById(cardId);
        syncExpiredStatus(card);

        if (card.getStatus() == CardStatus.EXPIRED) {
            throw new IllegalArgumentException("Cannot activate expired card");
        }

        card.setStatus(CardStatus.ACTIVE);
        card.setBlockRequested(false);
        return toResponse(cardRepository.save(card));
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> getBlockRequests(Pageable pageable) {
        return cardRepository.findByBlockRequestedTrue(pageable).map(this::toResponse);
    }

    @Transactional
    public CardResponse rejectBlockRequestByAdmin(Long cardId) {
        Card card = getCardById(cardId);
        card.setBlockRequested(false);
        return toResponse(cardRepository.save(card));
    }

    @Transactional
    public void deleteCardByAdmin(Long cardId) {
        Card card = getCardById(cardId);
        cardRepository.delete(card);
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> getMyCards(String email, CardStatus status, String last4, Pageable pageable) {
        User user = getUserByEmail(email);

        Specification<Card> spec = Specification.where(CardSpecifications.hasUserId(user.getId()))
                .and(CardSpecifications.hasStatus(status))
                .and(CardSpecifications.last4Contains(last4));

        return cardRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional
    public CardResponse requestBlock(Long cardId, String email) {
        User user = getUserByEmail(email);
        Card card = getCardById(cardId);

        validateOwnership(card, user.getId());
        syncExpiredStatus(card);

        if (card.getStatus() == CardStatus.EXPIRED) {
            throw new IllegalArgumentException("Cannot request block for expired card");
        }

        card.setBlockRequested(true);

        return toResponse(cardRepository.save(card));
    }

    @Transactional
    public CardBalanceResponse getMyCardBalance(Long cardId, String email) {
        User user = getUserByEmail(email);
        Card card = getCardById(cardId);

        validateOwnership(card, user.getId());
        CardStatus beforeStatus = card.getStatus();
        syncExpiredStatus(card);

        if (beforeStatus != card.getStatus()) {
            cardRepository.save(card);
        }

        return CardBalanceResponse.builder()
                .cardId(card.getId())
                .balance(card.getBalance())
                .build();
    }

    private Card getCardById(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found: " + cardId));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private void validateOwnership(Card card, Long userId) {
        if (!card.getUserId().equals(userId)) {
            throw new ForbiddenOperationException("You can only access your own cards");
        }
    }

    private void syncExpiredStatus(Card card) {
        if (card.getExpiryDate().isBefore(YearMonth.now())) {
            card.setStatus(CardStatus.EXPIRED);
        }
    }

    private String normalizeCardNumber(String cardNumber) {
        return cardNumber == null ? "" : cardNumber.replaceAll("[^0-9]", "");
    }

    private CardResponse toResponse(Card card) {
        return CardResponse.builder()
                .id(card.getId())
                .maskedNumber(card.getMaskedNumber())
                .ownerName(card.getOwnerName())
                .expiryDate(card.getExpiryDate())
                .status(card.getStatus())
                .balance(card.getBalance())
                .userId(card.getUserId())
                .blockRequested(card.isBlockRequested())
                .build();
    }
}
