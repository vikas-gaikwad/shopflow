package com.shopflow.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(
        @NotBlank(message = "Customer name is required")
        @Size(max = 150, message = "Customer name cannot exceed 150 characters")
        String customerName,

        @Email(message = "Customer email must be valid")
        @Size(max = 254, message = "Customer email cannot exceed 254 characters")
        String customerEmail,

        @NotEmpty(message = "An order must contain at least one item")
        List<@Valid OrderItemRequest> items
) {
}