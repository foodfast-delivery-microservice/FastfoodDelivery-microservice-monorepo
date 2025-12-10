package com.example.demo.domain.exception;

public class MerchantHasActiveOrdersException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "Cannot delete merchant with active orders. All orders must be DELIVERED or CANCELLED.";

    public MerchantHasActiveOrdersException() {
        super(DEFAULT_MESSAGE);
    }

    public MerchantHasActiveOrdersException(String message) {
        super(message);
    }

    public MerchantHasActiveOrdersException(long activeOrderCount, String activeStatuses) {
        super(String.format("Cannot delete merchant. Found %d active orders with statuses: %s. " +
                "All orders must be DELIVERED or CANCELLED before deletion.",
                activeOrderCount, activeStatuses));
    }
}
