package com.shopflow.product.dto;

import jakarta.validation.constraints.NotNull;

public record StockAdjustmentRequest(
        @NotNull(message = "Adjustment is required")
        Integer adjustment
) {
}