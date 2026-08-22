package com.yida.integration;

import com.yida.dto.AddressBookCreateDTO;
import com.yida.exception.AddressValidationException;
import com.yida.service.support.AdministrativeDivisionValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.validation.Validator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.task.scheduling.enabled=false",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "yida.messaging.enabled=false"
})
@EnabledIfSystemProperty(named = "address.audit", matches = "true")
class AddressBookDataAuditTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private Validator validator;
    @Autowired
    private AdministrativeDivisionValidator divisionValidator;

    @Test
    void reportsHistoricalDirtyAddressCountsWithoutWritingData() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select id, consignee, phone, sex, province_code, province_name, city_code, city_name, " +
                        "district_code, district_name, detail, label, is_default from address_book");
        long fieldInvalid = 0;
        long regionMismatch = 0;
        Map<String, Long> fieldIssueCounts = new TreeMap<>();
        for (Map<String, Object> row : rows) {
            AddressBookCreateDTO dto = map(row);
            var violations = validator.validate(dto);
            if (!violations.isEmpty()) fieldInvalid++;
            violations.forEach(violation -> fieldIssueCounts.merge(
                    violation.getPropertyPath().toString(), 1L, Long::sum));
            try {
                divisionValidator.validate(dto);
            } catch (AddressValidationException ex) {
                regionMismatch++;
            }
        }
        Integer duplicateDefaultUsers = jdbcTemplate.queryForObject(
                "select count(*) from (select user_id from address_book where is_default = 1 " +
                        "group by user_id having count(*) > 1) duplicated", Integer.class);
        assertNotNull(duplicateDefaultUsers);
        System.out.printf("ADDRESS_AUDIT total=%d fieldInvalid=%d fieldIssues=%s regionMismatch=%d duplicateDefaultUsers=%d%n",
                rows.size(), fieldInvalid, fieldIssueCounts, regionMismatch, duplicateDefaultUsers);
    }

    private AddressBookCreateDTO map(Map<String, Object> row) {
        AddressBookCreateDTO dto = new AddressBookCreateDTO();
        dto.setConsignee(text(row, "consignee"));
        dto.setPhone(text(row, "phone"));
        dto.setSex(text(row, "sex"));
        dto.setProvinceCode(text(row, "province_code"));
        dto.setProvinceName(text(row, "province_name"));
        dto.setCityCode(text(row, "city_code"));
        dto.setCityName(text(row, "city_name"));
        dto.setDistrictCode(text(row, "district_code"));
        dto.setDistrictName(text(row, "district_name"));
        dto.setDetail(text(row, "detail"));
        dto.setLabel(text(row, "label"));
        Object defaultValue = row.get("is_default");
        dto.setIsDefault(defaultValue instanceof Number number ? number.intValue() : null);
        return dto;
    }

    private String text(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? null : value.toString();
    }
}
