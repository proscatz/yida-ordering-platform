package com.yida.dto;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class AddressBookDtoValidationTest {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void closeFactory() {
        FACTORY.close();
    }

    @Test
    void legalAddressPassesValidationAndTrimsText() {
        AddressBookCreateDTO dto = valid();
        dto.setConsignee("  张三  ");
        assertTrue(VALIDATOR.validate(dto).isEmpty());
        assertEquals("张三", dto.getConsignee());
    }

    @Test
    void blankConsigneeIsRejected() {
        AddressBookCreateDTO dto = valid();
        dto.setConsignee("  ");
        assertField(dto, "consignee");
    }

    @Test
    void illegalPhoneIsRejected() {
        AddressBookCreateDTO dto = valid();
        dto.setPhone("123456");
        assertField(dto, "phone");
    }

    @Test
    void illegalSexIsRejected() {
        AddressBookCreateDTO dto = valid();
        dto.setSex("2");
        assertField(dto, "sex");
    }

    @Test
    void missingAdministrativeDivisionIsRejected() {
        AddressBookCreateDTO dto = valid();
        dto.setDistrictCode(null);
        dto.setDistrictName(null);
        assertField(dto, "districtCode");
        assertField(dto, "districtName");
    }

    @Test
    void detailTooShortOrTooLongIsRejected() {
        AddressBookCreateDTO dto = valid();
        dto.setDetail("短址");
        assertField(dto, "detail");
        dto.setDetail("地".repeat(201));
        assertField(dto, "detail");
    }

    @Test
    void illegalDefaultFlagIsRejected() {
        AddressBookCreateDTO dto = valid();
        dto.setIsDefault(2);
        assertField(dto, "isDefault");
    }

    @Test
    void controlCharactersAreRejected() {
        AddressBookCreateDTO dto = valid();
        dto.setDetail("北京市东城区\u0000一号楼");
        assertField(dto, "detail");
    }

    @Test
    void clientCannotPopulateUserIdBecauseDtosDoNotExposeIt() {
        assertFalse(hasField(AddressBookCreateDTO.class, "userId"));
        assertFalse(hasField(AddressBookUpdateDTO.class, "userId"));
        assertFalse(hasField(AddressBookDefaultDTO.class, "userId"));
    }

    public static AddressBookCreateDTO valid() {
        AddressBookCreateDTO dto = new AddressBookCreateDTO();
        dto.setConsignee("张三");
        dto.setPhone("13800138000");
        dto.setSex("1");
        dto.setProvinceCode("110000");
        dto.setProvinceName("北京市");
        dto.setCityCode("110100");
        dto.setCityName("北京市");
        dto.setDistrictCode("110101");
        dto.setDistrictName("东城区");
        dto.setDetail("东华门街道一号院");
        dto.setLabel("家");
        dto.setIsDefault(0);
        return dto;
    }

    private void assertField(Object dto, String field) {
        Set<ConstraintViolation<Object>> violations = VALIDATOR.validate(dto);
        assertTrue(violations.stream().anyMatch(v -> field.equals(v.getPropertyPath().toString())),
                () -> "Expected violation for " + field + ", actual=" + violations);
    }

    private boolean hasField(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (name.equals(field.getName())) return true;
            }
        }
        return false;
    }
}
