package com.example.safariwave_backend.service.payment;

import com.example.safariwave_backend.dto.request.PaymentRequestDTO;
import com.example.safariwave_backend.entity.Booking;
import com.example.safariwave_backend.exception.BadRequestException;
import com.example.safariwave_backend.security.AuthenticatedUser;
import com.example.safariwave_backend.security.Roles;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaymentContextTest {

    private final PaymentContext context = new PaymentContext(List.of(new CardPaymentStrategy(), new CashPaymentStrategy()));

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static void loginAs(String role) {
        AuthenticatedUser user = new AuthenticatedUser(1, "user@safariwave.test", role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, Roles.authoritiesFor(role)));
    }

    private static PaymentRequestDTO request(String method, String outcome) {
        PaymentRequestDTO dto = new PaymentRequestDTO();
        dto.setMethod(method);
        dto.setOutcome(outcome);
        return dto;
    }

    // ---- choosing the strategy ----

    @Test
    void picksTheStrategyForTheRequestedMethod_ignoringCase() {
        assertInstanceOf(CardPaymentStrategy.class, context.strategyFor("card"));
        assertInstanceOf(CashPaymentStrategy.class, context.strategyFor(" CASH "));
    }

    @Test
    void noMethod_meansCard_soTheExistingPayButtonKeepsWorking() {
        assertInstanceOf(CardPaymentStrategy.class, context.strategyFor(null));
        assertInstanceOf(CardPaymentStrategy.class, context.strategyFor(""));
    }

    @Test
    void unknownMethod_isRejected() {
        assertThrows(BadRequestException.class, () -> context.strategyFor("BITCOIN"));
    }

    // ---- the strategies ----

    @Test
    void card_followsTheMockGatewayOutcome() {
        CardPaymentStrategy card = new CardPaymentStrategy();
        assertTrue(card.pay(new Booking(), request("CARD", "success")).succeeded());

        PaymentOutcome declined = card.pay(new Booking(), request("CARD", "FAILED"));
        assertEquals("FAILED", declined.status());
        assertEquals("Mock payment declined", declined.failureReason());

        assertThrows(BadRequestException.class, () -> card.pay(new Booking(), request("CARD", null)));
        assertEquals("MOCK_CARD", card.provider());
    }

    @Test
    void cash_isRecordedByStaff() {
        loginAs(Roles.CUSTOMER_RELATIONS);
        PaymentOutcome outcome = new CashPaymentStrategy().pay(new Booking(), request("CASH", null));

        assertTrue(outcome.succeeded());
        assertNull(outcome.failureReason());
    }

    @Test
    void cash_cannotBeUsedByACustomer() {
        loginAs(Roles.CUSTOMER);
        assertThrows(AccessDeniedException.class, () -> new CashPaymentStrategy().pay(new Booking(), request("CASH", null)));
    }
}
