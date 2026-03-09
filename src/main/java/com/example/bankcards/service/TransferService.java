package com.example.bankcards.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.dto.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.ForbiddenOperationException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;

import java.math.BigDecimal;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;

    @Transactional
    public TransferResponse transferBetweenOwnCards(TransferRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        if (request.getFromCardId().equals(request.getToCardId())) {
            throw new IllegalArgumentException("Source and destination cards must be different");
        }

        Card fromCard = getCardById(request.getFromCardId());
        Card toCard = getCardById(request.getToCardId());

        validateOwnership(user, fromCard, toCard);
        syncExpiredStatus(fromCard);
        syncExpiredStatus(toCard);
        validateCardAvailableForTransfer(fromCard, "source");
        validateCardAvailableForTransfer(toCard, "destination");

        BigDecimal amount = request.getAmount();
        if (fromCard.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        fromCard.setBalance(fromCard.getBalance().subtract(amount));
        toCard.setBalance(toCard.getBalance().add(amount));

        cardRepository.save(fromCard);
        cardRepository.save(toCard);

        return TransferResponse.builder()
                .fromCardId(fromCard.getId())
                .toCardId(toCard.getId())
                .amount(amount)
                .fromCardBalance(fromCard.getBalance())
                .toCardBalance(toCard.getBalance())
                .build();
    }

    private Card getCardById(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found: " + cardId));
    }

    private void validateOwnership(User user, Card fromCard, Card toCard) {
        Long userId = user.getId();
        if (!userId.equals(fromCard.getUserId()) || !userId.equals(toCard.getUserId())) {
            throw new ForbiddenOperationException("Transfer is allowed only between your own cards");
        }
    }

    private void validateCardAvailableForTransfer(Card card, String direction) {
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new IllegalArgumentException("The " + direction + " card is not active");
        }
    }

    private void syncExpiredStatus(Card card) {
        if (card.getExpiryDate().isBefore(YearMonth.now())) {
            card.setStatus(CardStatus.EXPIRED);
            cardRepository.save(card);
        }
    }
}
