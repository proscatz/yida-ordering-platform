package com.yida.service.support;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yida.dto.AddressBookBaseDTO;
import com.yida.exception.AddressValidationException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
public class AdministrativeDivisionValidator {
    private final List<DivisionNode> provinces;

    public AdministrativeDivisionValidator(ObjectMapper objectMapper) {
        try {
            DivisionDocument document = objectMapper.readValue(
                    new ClassPathResource("administrative-divisions.json").getInputStream(),
                    DivisionDocument.class);
            this.provinces = document.levels == null ? Collections.emptyList() : document.levels;
        } catch (IOException ex) {
            throw new IllegalStateException("行政区划数据加载失败", ex);
        }
    }

    public void validate(AddressBookBaseDTO dto) {
        DivisionNode province = find(provinces, dto.getProvinceCode(), dto.getProvinceName());
        if (province == null) {
            throw new AddressValidationException("region", "省市区信息不匹配，请重新选择");
        }
        DivisionNode city = find(province.children, dto.getCityCode(), dto.getCityName());
        if (city == null) {
            throw new AddressValidationException("region", "省市区信息不匹配，请重新选择");
        }
        DivisionNode district = find(city.children, dto.getDistrictCode(), dto.getDistrictName());
        if (district == null) {
            throw new AddressValidationException("region", "省市区信息不匹配，请重新选择");
        }
    }

    private DivisionNode find(List<DivisionNode> nodes, String code, String name) {
        if (nodes == null) {
            return null;
        }
        return nodes.stream()
                .filter(node -> Objects.equals(code, node.value) && Objects.equals(name, node.text))
                .findFirst()
                .orElse(null);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class DivisionDocument {
        public List<DivisionNode> levels;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class DivisionNode {
        public String text;
        public String value;
        public List<DivisionNode> children;
    }
}
