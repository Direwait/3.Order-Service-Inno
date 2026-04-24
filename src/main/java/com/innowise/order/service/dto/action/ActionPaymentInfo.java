package com.innowise.order.service.dto.action;

import com.innowise.order.dao.enums.EventType;
import com.innowise.order.dao.enums.Status;

import java.math.BigDecimal;

public record ActionPaymentInfo(
        EventType eventType,
        String paymentId,
        String orderId,
        String userId,
        Status status,
        BigDecimal paymentAmount
) {}
