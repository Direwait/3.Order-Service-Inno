package com.innowise.order.service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemDto {

    @NotNull(message = "Item ID cannot be null")
    private UUID itemId;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 9999, message = "Quantity cannot exceed 9999")
    private int quantity;
}
