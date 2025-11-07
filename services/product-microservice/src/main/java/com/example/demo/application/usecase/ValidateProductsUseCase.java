package com.example.demo.application.usecase;

import com.example.demo.domain.model.Product;
import com.example.demo.domain.repository.ProductRepository;
import com.example.demo.interfaces.rest.dto.ProductValidationRequest;
import com.example.demo.interfaces.rest.dto.ProductValidationResponse;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ValidateProductsUseCase {
    private final ProductRepository productRepository;

    /*
     * Nhận vào một danh sách các item cần kiểm tra
     * Trả về một danh sách kết quả tương ứng
     */
    public List<ProductValidationResponse> validate (List<ProductValidationRequest> requests){
        List<ProductValidationResponse> results = new ArrayList<>();

        for (ProductValidationRequest item : requests) {
            // tìm sản phẩm trong dtb
            Optional<Product> productOpt = productRepository.findById(item.getProductId());

            if(productOpt.isEmpty()){
                // sản phẩm không tồn tại
                results.add(new ProductValidationResponse(
                        item.getProductId(),
                        false, // isSuccess = false
                        "Product not found",
                        null
                ));
            } else {
                // sản phẩm tồn tại
                Product product = productOpt.get();

                // kiem tra so luong ton kho

                if (product.getStock() >= item.getQuantity()){
                    // du hang
                    results.add(new ProductValidationResponse(
                            product.getId(),
                            true, // isSuccess
                            product.getName(),
                            product.getPrice()
                    ));
                }else{
                    // khong du hang
                    results.add(new ProductValidationResponse(
                            product.getId(),
                            false, // isSuccess
                            "not enough stock",
                            product.getPrice()

                    ));
                }
            }

        }
        return results;
    }

}
