package com.shopflow.common.exception;

public class DuplicateCategoryNameException extends RuntimeException {

    public DuplicateCategoryNameException(String name) {
        super("Category already exists with name: " + name);
    }
}