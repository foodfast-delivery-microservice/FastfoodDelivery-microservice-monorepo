package com.example.order_service.application.dto;


// DTO này đại diện cho 1 item trong giỏ hàng cần kiểm tra
public record ProductValidationRequest(
        Long productId,
        int quantity
) {
    // record đã tự tạo constructor, getters, equals, hashCode, toString
}