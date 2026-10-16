
package com.shopflow.product.service;

import com.shopflow.common.exception.DuplicateProductSkuException;
import com.shopflow.common.exception.ProductNotFoundException;
import com.shopflow.product.dto.CreateProductRequest;
import com.shopflow.product.dto.ProductResponse;
import com.shopflow.product.dto.UpdateProductRequest;
import com.shopflow.product.entity.Product;
import com.shopflow.product.repository.ProductRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

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
            return ProductResponse.from(productRepository.saveAndFlush(product));
        } catch (DataIntegrityViolationException exception) {
            // Also protects against concurrent requests for the same SKU.
            throw new DuplicateProductSkuException(sku);
        }
    }

    public List<ProductResponse> findAll() {
        return productRepository.findAllByOrderByIdAsc()
                .stream()
                .map(ProductResponse::from)
                .toList();
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
}
