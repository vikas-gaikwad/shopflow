package com.shopflow.category.service;

import com.shopflow.category.dto.CategoryResponse;
import com.shopflow.category.dto.CreateCategoryRequest;
import com.shopflow.category.dto.UpdateCategoryRequest;
import com.shopflow.category.entity.Category;
import com.shopflow.category.repository.CategoryRepository;
import com.shopflow.common.exception.CategoryNotFoundException;
import com.shopflow.common.exception.DuplicateCategoryNameException;
import com.shopflow.product.repository.ProductRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(
            CategoryRepository categoryRepository,
            ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        String name = normalizeName(request.name());

        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateCategoryNameException(name);
        }

        Category category = new Category(name, request.description());

        try {
            return CategoryResponse.from(
                    categoryRepository.saveAndFlush(category));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateCategoryNameException(name);
        }
    }

    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll(
                        org.springframework.data.domain.Sort
                                .by("name").ascending())
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public CategoryResponse findById(Long id) {
        return CategoryResponse.from(getCategory(id));
    }

    @Transactional
    public CategoryResponse update(
            Long id,
            UpdateCategoryRequest request) {

        Category category = getCategory(id);
        String name = normalizeName(request.name());

        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateCategoryNameException(name);
        }

        category.updateDetails(name, request.description());

        try {
            return CategoryResponse.from(
                    categoryRepository.saveAndFlush(category));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateCategoryNameException(name);
        }
    }

    @Transactional
    public void delete(Long id) {
        Category category = getCategory(id);

        if (productRepository.existsByCategory_Id(id)) {
            throw new IllegalStateException(
                    "Cannot delete category while products are assigned to it.");
        }

        categoryRepository.delete(category);
        categoryRepository.flush();
    }

    private Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    private String normalizeName(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }
}