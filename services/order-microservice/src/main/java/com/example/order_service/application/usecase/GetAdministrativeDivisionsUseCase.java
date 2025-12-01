package com.example.order_service.application.usecase;

import com.example.order_service.application.dto.CommuneResponse;
import com.example.order_service.application.dto.ProvinceResponse;
import com.example.order_service.infrastructure.service.AddressKitClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetAdministrativeDivisionsUseCase {

    private static final String EFFECTIVE_DATE_LATEST = "latest";

    private final AddressKitClient addressKitClient;

    public List<ProvinceResponse> getProvinces() {
        return addressKitClient.getProvinces(EFFECTIVE_DATE_LATEST).stream()
                .map(p -> ProvinceResponse.builder()
                        .code(p.getCode())
                        .name(p.getName())
                        .shortName(p.getShortName())
                        .build())
                .toList();
    }

    public List<CommuneResponse> getCommunes(String provinceCode) {
        return addressKitClient.getCommunesForProvince(EFFECTIVE_DATE_LATEST, provinceCode).stream()
                .map(c -> CommuneResponse.builder()
                        .code(c.getCode())
                        .name(c.getName())
                        .districtName(c.getDistrictName())
                        .provinceCode(c.getProvinceCode())
                        .provinceName(c.getProvinceName())
                        .build())
                .toList();
    }
}



