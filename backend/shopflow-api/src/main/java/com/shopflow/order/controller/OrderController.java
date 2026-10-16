
package com.shopflow.order.controller;

import com.shopflow.common.dto.PageResponse;
import com.shopflow.order.dto.CreateOrderRequest;
import com.shopflow.order.dto.OrderResponse;
import com.shopflow.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return orderService.createOrder(request);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable Long id) {
        return orderService.getOrder(id);
    }

    @GetMapping
    public PageResponse<OrderResponse> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return orderService.getOrders(page, size);
    }

    @PatchMapping("/{id}/cancel")
    public OrderResponse cancelOrder(@PathVariable Long id) {
        return orderService.cancelOrder(id);
    }
}
