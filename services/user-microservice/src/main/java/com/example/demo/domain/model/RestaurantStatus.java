package com.example.demo.domain.model;

/**
 * Restaurant status for managing deletion lifecycle in Saga pattern.
 * 
 * ACTIVE - Restaurant is operational
 * DELETE_PENDING - Deletion initiated, awaiting validation from Order Service
 * DELETED - Restaurant has been deleted (soft delete)
 */
public enum RestaurantStatus {
    ACTIVE,
    DELETE_PENDING,
    DELETED
}
