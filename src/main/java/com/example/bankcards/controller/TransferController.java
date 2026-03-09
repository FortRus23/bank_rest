package com.example.bankcards.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.dto.TransferResponse;
import com.example.bankcards.service.TransferService;

import java.security.Principal;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping("/me")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public TransferResponse transferBetweenOwnCards(@Valid @RequestBody TransferRequest request, Principal principal) {
        return transferService.transferBetweenOwnCards(request, principal.getName());
    }
}
