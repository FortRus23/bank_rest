package com.example.bankcards.util;

import org.springframework.data.jpa.domain.Specification;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;

public final class CardSpecifications {

    private CardSpecifications() {
    }

    public static Specification<Card> hasUserId(Long userId) {
        return (root, query, cb) -> userId == null ? null : cb.equal(root.get("userId"), userId);
    }

    public static Specification<Card> hasStatus(CardStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Card> last4Contains(String last4) {
        return (root, query, cb) -> {
            if (last4 == null || last4.isBlank()) {
                return null;
            }
            return cb.like(root.get("last4"), "%" + last4 + "%");
        };
    }
}
