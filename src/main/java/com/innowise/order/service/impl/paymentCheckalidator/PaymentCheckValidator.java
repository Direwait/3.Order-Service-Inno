package com.innowise.order.service.impl.paymentCheckalidator;

import com.innowise.order.dao.enums.Status;
import com.innowise.order.dao.model.OrderModel;
import com.innowise.order.service.dto.action.ActionPaymentInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentCheckValidator {

    public Status resolveStatus(ActionPaymentInfo action, OrderModel order) {
        if (action.paymentAmount().compareTo(order.getTotalPrice()) != 0) {
            log.warn("Amount mismatch: payment={}, order={}",
                    action.paymentAmount(), order.getTotalPrice());
            return Status.FAILED;
        }

        if (!order.getUserId().toString().equals(action.userId())) {
            log.warn("User mismatch");
            return Status.FAILED;
        }

        return action.status();
    }
}
