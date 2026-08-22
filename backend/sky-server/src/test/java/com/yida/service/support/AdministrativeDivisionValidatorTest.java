package com.yida.service.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yida.dto.AddressBookCreateDTO;
import com.yida.exception.AddressValidationException;
import org.junit.jupiter.api.Test;

import static com.yida.dto.AddressBookDtoValidationTest.valid;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdministrativeDivisionValidatorTest {
    private final AdministrativeDivisionValidator validator = new AdministrativeDivisionValidator(new ObjectMapper());

    @Test
    void validProvinceCityDistrictCombinationPasses() {
        assertDoesNotThrow(() -> validator.validate(valid()));
    }

    @Test
    void mismatchedProvinceCityDistrictCombinationIsRejected() {
        AddressBookCreateDTO dto = valid();
        dto.setDistrictCode("120101");
        dto.setDistrictName("和平区");
        AddressValidationException exception = assertThrows(AddressValidationException.class,
                () -> validator.validate(dto));
        assertEquals("region", exception.getField());
    }

    @Test
    void arbitrarySixDigitCodeIsRejected() {
        AddressBookCreateDTO dto = valid();
        dto.setProvinceCode("999999");
        assertThrows(AddressValidationException.class, () -> validator.validate(dto));
    }
}
