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
            ProductValidationResponse response = new ProductValidationResponse();
            Long productId = item.getProductId();

            if (productId == null) {
                response.setSuccess(false);
                response.setMessage("ProductId is required");
                results.add(response);
                continue;
            }

            // tìm sản phẩm trong dtb
            Optional<Product> productOpt = productRepository.findById(productId);

            if(productOpt.isEmpty()){
                // sản phẩm không tồn tại
                response.setProductId(productId);
                response.setSuccess(false);
                response.setMessage("Product not found");
            } else {
                // sản phẩm tồn tại
                Product product = productOpt.get();
                response.setProductId(product.getId());
                response.setProductName(product.getName());
                response.setUnitPrice(product.getPrice());
                response.setMerchantId(product.getMerchantId());

                if (!product.isActive()) {
                    response.setSuccess(false);
                    response.setMessage("Product inactive");
                } else if (product.getStock() >= item.getQuantity()){
                    // du hang
                    response.setSuccess(true);
                    response.setMessage(null);
                }else{
                    // khong du hang
                    response.setSuccess(false);
                    response.setMessage("Not enough stock");
                }
            }

            results.add(response);

        }
        return results;
    }

}
