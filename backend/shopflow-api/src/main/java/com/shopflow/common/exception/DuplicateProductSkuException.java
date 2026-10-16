
package com.shopflow.common.exception;

public class DuplicateProductSkuException extends RuntimeException {

    public DuplicateProductSkuException(String sku) {
        super("A product already exists with SKU: " + sku);
    }
}
