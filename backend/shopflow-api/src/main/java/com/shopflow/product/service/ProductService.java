
package com.shopflow.product.service;

import com.shopflow.common.exception.DuplicateProductSkuException;
import com.shopflow.common.exception.ProductNotFoundException;
import com.shopflow.product.dto.*;
import com.shopflow.product.entity.Product;
import com.shopflow.product.repository.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "sku",
            "name",
            "price",
            "stockQuantity",
            "active",
            "createdAt",
            "updatedAt"
    );

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        String sku = request.sku().trim().toUpperCase(Locale.ROOT);

        if (productRepository.existsBySkuIgnoreCase(sku)) {
            throw new DuplicateProductSkuException(sku);
        }

        Product product = new Product(
                sku,
                request.name().trim(),
                request.description(),
                request.price(),
                request.stockQuantity()
        );

        try {
            return ProductResponse.from(
                    productRepository.saveAndFlush(product)
            );
        } catch (DataIntegrityViolationException exception) {
            // Also protects against concurrent requests for the same SKU.
            throw new DuplicateProductSkuException(sku);
        }
    }

    public ProductPageResponse findAll(
            String search,
            Boolean active,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        if (page < 0) {
            throw badRequest("Page must be zero or greater.");
        }

        if (size < 1 || size > 100) {
            throw badRequest("Size must be between 1 and 100.");
        }

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw badRequest(
                    "Unsupported sortBy field. Allowed fields: "
                            + ALLOWED_SORT_FIELDS
            );
        }

        Sort.Direction direction;

        try {
            direction = Sort.Direction.fromString(sortDir);
        } catch (IllegalArgumentException exception) {
            throw badRequest("sortDir must be 'asc' or 'desc'.");
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(direction, sortBy)
        );

        Specification<Product> specification =
                (root, query, criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<>();

                    if (search != null && !search.isBlank()) {
                        String pattern = "%"
                                + search.trim().toLowerCase(Locale.ROOT)
                                + "%";

                        predicates.add(
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(
                                                root.get("name")
                                        ),
                                        pattern
                                )
                        );
                    }

                    if (active != null) {
                        predicates.add(
                                criteriaBuilder.equal(
                                        root.get("active"),
                                        active
                                )
                        );
                    }

                    return criteriaBuilder.and(
                            predicates.toArray(new Predicate[0])
                    );
                };

        Page<Product> productPage =
                productRepository.findAll(specification, pageable);

        List<ProductResponse> content = productPage.getContent()
                .stream()
                .map(ProductResponse::from)
                .toList();

        return new ProductPageResponse(
                content,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.isFirst(),
                productPage.isLast()
        );
    }

    public ProductResponse findById(Long id) {
        return ProductResponse.from(getProduct(id));
    }

    @Transactional
    public ProductResponse update(
            Long id,
            UpdateProductRequest request) {

        Product product = getProduct(id);

        product.updateDetails(
                request.name().trim(),
                request.description(),
                request.price(),
                request.stockQuantity(),
                request.active()
        );

        Product updatedProduct = productRepository.saveAndFlush(product);

        return ProductResponse.from(updatedProduct);
    }

    @Transactional
    public void delete(Long id) {
        Product product = getProduct(id);
        productRepository.delete(product);
    }

    private Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    @Transactional
    public ProductResponse adjustStock(Long id,StockAdjustmentRequest request) {
        int adjustment = request.adjustment();
        if (adjustment == 0) {
            throw badRequest("Adjustment cannot be zero.");
        }
        Product product = productRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        long updatedQuantity =
                (long) product.getStockQuantity() + adjustment;

        if (updatedQuantity < 0) {
            throw badRequest(
                    "Insufficient stock. Current quantity: "
                            + product.getStockQuantity()
                            + ", requested adjustment: "
                            + adjustment
            );
        }

        if (updatedQuantity > Integer.MAX_VALUE) {
            throw badRequest("Stock quantity exceeds the supported maximum.");
        }

        product.updateStockQuantity((int) updatedQuantity);

        Product updatedProduct = productRepository.saveAndFlush(product);

        return ProductResponse.from(updatedProduct);
    }

}