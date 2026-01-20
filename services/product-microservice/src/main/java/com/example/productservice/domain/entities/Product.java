package com.example.productservice.domain.entities;

import com.example.productservice.domain.valueobjects.Price;
import com.example.productservice.domain.valueobjects.Stock;
import lombok.*;

/**
 * Pure domain entity representing a Product.
 * Contains business logic and domain rules, independent of persistence framework.
 * No JPA annotations - this is a pure business object.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = { "id" })
public class Product {

    private Long id;
    private String name;
    private String description;
    private Price price;
    private Stock stock;
    private Category category;
    private Long merchantId;
    private boolean active = true;
    private String imageUrl;

    public enum Category {
        DRINK,
        FOOD
    }

    // ==================== Business Logic Methods ====================

    /**
     * Check if product can be purchased
     * Product must be active and have available stock
     */
    public boolean canBePurchased() {
        return active && stock != null && stock.isAvailable();
    }

    /**
     * Check if product can be purchased with specified quantity
     */
    public boolean canBePurchased(int quantity) {
        if (!active) {
            return false;
        }
        if (stock == null) {
            return false;
        }
        return stock.canDeduct(quantity);
    }

    /**
     * Deduct stock by specified quantity
     * 
     * @param quantity The quantity to deduct
     * @throws IllegalArgumentException if insufficient stock
     */
    public void deductStock(int quantity) {
        if (stock == null) {
            throw new IllegalStateException("Product stock is null");
        }
        this.stock = stock.deduct(quantity);
    }

    /**
     * Restore stock by specified quantity
     * 
     * @param quantity The quantity to restore
     */
    public void restoreStock(int quantity) {
        if (stock == null) {
            this.stock = new Stock(quantity);
        } else {
            this.stock = stock.restore(quantity);
        }
    }

    /**
     * Calculate total price for a given quantity
     * 
     * @param quantity The quantity to calculate price for
     * @return Price object representing total
     */
    public Price calculateTotalPrice(int quantity) {
        if (price == null) {
            throw new IllegalStateException("Product price is null");
        }
        return price.multiply(quantity);
    }

    /**
     * Check if product belongs to a merchant
     */
    public boolean belongsToMerchant(Long merchantId) {
        return this.merchantId != null && this.merchantId.equals(merchantId);
    }
}
