package com.innowise.order.service.impl.totalPriceCalculator;

import com.innowise.order.dao.model.ItemModel;
import com.innowise.order.dao.model.OrderItemModel;
import com.innowise.order.dao.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderTotalPriceCalculator {
    private final ItemRepository itemRepository;

    public BigDecimal calculateTotalPrice(List<OrderItemModel> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return items.stream()
                .map(item -> {
                    UUID itemId = item.getItem().getId();
                    BigDecimal itemPrice = findItemPriceById(itemId);
                    if (itemPrice == null) {
                        return BigDecimal.ZERO;
                    }

                    return itemPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal findItemPriceById(UUID itemId) {
        return itemRepository.findById(itemId)
                .map(ItemModel::getPrice)
                .orElseGet(() -> {
                    log.warn("Item not found or price is null for id: {}", itemId);
                    return BigDecimal.ZERO;
                });
    }

}
