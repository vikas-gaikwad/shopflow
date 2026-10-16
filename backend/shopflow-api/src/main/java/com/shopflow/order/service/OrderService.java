
package com.shopflow.order.service;

import com.shopflow.common.dto.PageResponse;
import com.shopflow.order.dto.CreateOrderRequest;
import com.shopflow.order.dto.OrderItemRequest;
import com.shopflow.order.dto.OrderResponse;
import com.shopflow.order.entity.CustomerOrder;
import com.shopflow.order.entity.OrderItem;
import com.shopflow.order.entity.OrderStatus;
import com.shopflow.order.repository.OrderRepository;
import com.shopflow.product.entity.Product;
import com.shopflow.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(
            OrderRepository orderRepository,
            ProductRepository productRepository
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        List<OrderItemRequest> requestedItems = request.items();

        // Reject duplicate products in one order.
        Set<Long> productIds = new HashSet<>();

        for (OrderItemRequest item : requestedItems) {
            if (!productIds.add(item.productId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A product can appear only once in an order: "
                                + item.productId()
                );
            }
        }

        // Lock product rows in a consistent order to reduce deadlocks.
        List<Long> sortedProductIds = productIds.stream()
                .sorted()
                .toList();

        Map<Long, Product> lockedProducts = new HashMap<>();

        for (Long productId : sortedProductIds) {
            Product product = productRepository
                    .findByIdForUpdate(productId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Product not found: " + productId
                    ));

            if (!product.isActive()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Product is inactive: " + productId
                );
            }

            lockedProducts.put(productId, product);
        }

        // Validate every requested quantity before changing any stock.
        for (OrderItemRequest item : requestedItems) {
            Product product = lockedProducts.get(item.productId());

            if (product.getStockQuantity() < item.quantity()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Insufficient stock for product "
                                + product.getSku()
                                + ". Available: "
                                + product.getStockQuantity()
                                + ", requested: "
                                + item.quantity()
                );
            }
        }

        CustomerOrder order = new CustomerOrder(
                request.customerName().trim(),
                normalizeEmail(request.customerEmail())
        );

        BigDecimal totalAmount = BigDecimal.ZERO;

        // Reserve stock and capture purchase-time product details.
        for (OrderItemRequest item : requestedItems) {
            Product product = lockedProducts.get(item.productId());

            product.updateStockQuantity(
                    product.getStockQuantity() - item.quantity()
            );

            OrderItem orderItem = new OrderItem(
                    product,
                    item.quantity(),
                    product.getPrice()
            );

            order.addItem(orderItem);
            totalAmount = totalAmount.add(orderItem.getLineTotal());
        }

        order.setTotalAmount(totalAmount);

        // Flush within the transaction so persistence errors surface here.
        CustomerOrder savedOrder = orderRepository.saveAndFlush(order);

        return OrderResponse.from(savedOrder);
    }

    public OrderResponse getOrder(Long orderId) {
        CustomerOrder order = orderRepository
                .findByIdWithItems(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found: " + orderId
                ));

        return OrderResponse.from(order);
    }

    public PageResponse<OrderResponse> getOrders(
            int page,
            int size
    ) {
        if (page < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be zero or greater"
            );
        }

        if (size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page size must be between 1 and 100"
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<OrderResponse> result = orderRepository
                .findAllByOrderByCreatedAtDesc(pageable)
                .map(OrderResponse::from);

        return PageResponse.from(result);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        CustomerOrder order = orderRepository
                .findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found: " + orderId
                ));

        if (order.getStatus() != OrderStatus.PLACED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only placed orders can be cancelled"
            );
        }

        // Collect and sort product IDs before obtaining inventory locks.
        List<Long> sortedProductIds = order.getItems()
                .stream()
                .map(item -> item.getProduct().getId())
                .distinct()
                .sorted()
                .toList();

        Map<Long, Product> lockedProducts = new HashMap<>();

        for (Long productId : sortedProductIds) {
            Product product = productRepository
                    .findByIdForUpdate(productId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Cannot restore stock because product "
                                    + productId + " no longer exists"
                    ));

            lockedProducts.put(productId, product);
        }

        // Restore inventory while all relevant product rows are locked.
        for (OrderItem item : order.getItems()) {
            Product product = lockedProducts.get(
                    item.getProduct().getId()
            );

            try {
                product.updateStockQuantity(
                        Math.addExact(
                                product.getStockQuantity(),
                                item.getQuantity()
                        )
                );
            } catch (ArithmeticException exception) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Stock quantity overflow for product "
                                + product.getId()
                );
            }
        }

        order.cancel();

        CustomerOrder savedOrder = orderRepository.saveAndFlush(order);

        return OrderResponse.from(savedOrder);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
