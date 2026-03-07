package com.example.bankcards.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;

public interface CardRepository extends JpaRepository<Card, Long>, JpaSpecificationExecutor<Card> {
    Page<Card> findByUserId(Long userId, Pageable pageable);

    Page<Card> findByUserIdAndStatus(Long userId, CardStatus status, Pageable pageable);

    Page<Card> findByUserIdAndLast4Containing(Long userId, String last4, Pageable pageable);

    Page<Card> findByBlockRequestedTrue(Pageable pageable);

    boolean existsByEncryptedNumber(String encryptedNumber);
}
