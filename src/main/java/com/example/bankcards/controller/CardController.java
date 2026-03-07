package com.example.bankcards.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.bankcards.dto.CardBalanceResponse;
import com.example.bankcards.dto.CardCreateRequest;
import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.service.CardService;

import java.security.Principal;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public CardResponse createCard(@Valid @RequestBody CardCreateRequest request) {
        return cardService.createCard(request);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<CardResponse> getAllCards(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) String last4,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return cardService.getAllCards(userId, status, last4, pageable);
    }

    @PatchMapping("/admin/{cardId}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public CardResponse blockCardByAdmin(@PathVariable Long cardId) {
        return cardService.blockCardByAdmin(cardId);
    }

    @PatchMapping("/admin/{cardId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public CardResponse activateCardByAdmin(@PathVariable Long cardId) {
        return cardService.activateCardByAdmin(cardId);
    }

    @GetMapping("/admin/block-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<CardResponse> getBlockRequests(@PageableDefault(size = 20) Pageable pageable) {
        return cardService.getBlockRequests(pageable);
    }

    @PatchMapping("/admin/{cardId}/reject-block-request")
    @PreAuthorize("hasRole('ADMIN')")
    public CardResponse rejectBlockRequestByAdmin(@PathVariable Long cardId) {
        return cardService.rejectBlockRequestByAdmin(cardId);
    }

    @DeleteMapping("/admin/{cardId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteCardByAdmin(@PathVariable Long cardId) {
        cardService.deleteCardByAdmin(cardId);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Page<CardResponse> getMyCards(
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) String last4,
            @PageableDefault(size = 20) Pageable pageable,
            Principal principal
    ) {
        return cardService.getMyCards(principal.getName(), status, last4, pageable);
    }

    @PatchMapping("/me/{cardId}/block-request")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public CardResponse requestBlock(@PathVariable Long cardId, Principal principal) {
        return cardService.requestBlock(cardId, principal.getName());
    }

    @GetMapping("/me/{cardId}/balance")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public CardBalanceResponse getBalance(@PathVariable Long cardId, Principal principal) {
        return cardService.getMyCardBalance(cardId, principal.getName());
    }
}
