
package com.shopflow.customer.service;

import com.shopflow.common.dto.PageResponse;
import com.shopflow.customer.dto.CreateCustomerRequest;
import com.shopflow.customer.dto.CustomerResponse;
import com.shopflow.customer.dto.UpdateCustomerRequest;
import com.shopflow.customer.entity.Customer;
import com.shopflow.customer.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        String email = normalizeEmail(request.email());

        if (customerRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A customer with this email already exists"
            );
        }

        Customer customer = new Customer(
                request.name().trim(),
                email,
                normalizePhone(request.phone())
        );

        return toResponse(customerRepository.saveAndFlush(customer));
    }

    public CustomerResponse getCustomerById(Long id) {
        return toResponse(findCustomer(id));
    }

    public PageResponse<CustomerResponse> getCustomers(int page, int size) {
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
                Sort.by(Sort.Direction.ASC, "id")
        );

        Page<CustomerResponse> customerPage =
                customerRepository.findAll(pageable)
                        .map(this::toResponse);

        return PageResponse.from(customerPage);
    }

    @Transactional
    public CustomerResponse updateCustomer(
            Long id,
            UpdateCustomerRequest request
    ) {
        Customer customer = findCustomer(id);
        String email = normalizeEmail(request.email());

        if (customerRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Another customer already uses this email"
            );
        }

        customer.updateDetails(
                request.name().trim(),
                email,
                normalizePhone(request.phone())
        );

        return toResponse(customerRepository.saveAndFlush(customer));
    }

    private Customer findCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Customer not found with id: " + id
                ));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        return phone.trim();
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}