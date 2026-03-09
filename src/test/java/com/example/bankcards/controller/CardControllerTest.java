package com.example.bankcards.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.example.bankcards.config.SecurityConfig;
import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.security.JwtAuthenticationFilter;
import com.example.bankcards.service.CardService;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CardController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CardService cardService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setupFilter() throws Exception {
        // Let mocked JWT filter pass requests further down the chain.
        doAnswer(invocation -> {
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "admin@bank.local", roles = {"ADMIN"})
    void createCardShouldReturnOkForAdmin() throws Exception {
        CardResponse response = CardResponse.builder()
                .id(100L)
                .maskedNumber("**** **** **** 1234")
                .ownerName("Test User")
                .expiryDate(YearMonth.of(2029, 12))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("500.00"))
                .userId(1L)
                .blockRequested(false)
                .build();

        when(cardService.createCard(any())).thenReturn(response);

        mockMvc.perform(post("/api/cards/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1,
                                  "cardNumber": "4111111111111234",
                                  "expiryDate": "2029-12",
                                  "initialBalance": 500.00
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.maskedNumber").value("**** **** **** 1234"));
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void createCardShouldBeForbiddenForUser() throws Exception {
        mockMvc.perform(post("/api/cards/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1,
                                  "cardNumber": "4111111111111234",
                                  "expiryDate": "2029-12",
                                  "initialBalance": 500.00
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void getMyCardsShouldReturnOkForUser() throws Exception {
        CardResponse response = CardResponse.builder()
                .id(101L)
                .maskedNumber("**** **** **** 1111")
                .ownerName("User")
                .expiryDate(YearMonth.of(2028, 10))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .userId(1L)
                .blockRequested(false)
                .build();

        Page<CardResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1);
        when(cardService.getMyCards(eq("user@test.com"), eq(CardStatus.ACTIVE), eq("11"), any())).thenReturn(page);

        mockMvc.perform(get("/api/cards/me")
                        .param("status", "ACTIVE")
                        .param("last4", "11")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(101))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));

        verify(cardService).getMyCards(eq("user@test.com"), eq(CardStatus.ACTIVE), eq("11"), any());
    }

    @Test
    void getMyCardsShouldBeForbiddenWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/cards/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@bank.local", roles = {"ADMIN"})
    void getAdminBlockRequestsShouldReturnOk() throws Exception {
        CardResponse response = CardResponse.builder()
                .id(102L)
                .maskedNumber("**** **** **** 2222")
                .ownerName("User")
                .expiryDate(YearMonth.of(2029, 1))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("150.00"))
                .userId(2L)
                .blockRequested(true)
                .build();

        when(cardService.getBlockRequests(any())).thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/cards/admin/block-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].blockRequested").value(true));
    }

    @Test
    @WithMockUser(username = "admin@bank.local", roles = {"ADMIN"})
    void rejectBlockRequestShouldReturnOkForAdmin() throws Exception {
        CardResponse response = CardResponse.builder()
                .id(103L)
                .maskedNumber("**** **** **** 3333")
                .ownerName("User")
                .expiryDate(YearMonth.of(2029, 1))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("250.00"))
                .userId(3L)
                .blockRequested(false)
                .build();

        when(cardService.rejectBlockRequestByAdmin(103L)).thenReturn(response);

        mockMvc.perform(patch("/api/cards/admin/103/reject-block-request"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(103))
                .andExpect(jsonPath("$.blockRequested").value(false));

        verify(cardService).rejectBlockRequestByAdmin(103L);
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void adminBlockEndpointShouldBeForbiddenForUser() throws Exception {
        mockMvc.perform(patch("/api/cards/admin/{cardId}/block", 1L))
                .andExpect(status().isForbidden());
    }
}
