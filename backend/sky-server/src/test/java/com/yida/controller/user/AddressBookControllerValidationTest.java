package com.yida.controller.user;

import com.yida.handler.GlobalExceptionHandler;
import com.yida.json.JacksonObjectMapper;
import com.yida.service.AddressBookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AddressBookControllerValidationTest {
    private AddressBookService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(AddressBookService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AddressBookController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(
                        new JacksonObjectMapper()))
                .build();
    }

    @Test
    void legalCreateReturnsNewAddressIdAndIgnoresForgedUserId() throws Exception {
        when(service.save(any())).thenReturn(66L);
        mvc.perform(post("/user/addressBook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson("\"userId\":999,")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1)))
                .andExpect(jsonPath("$.data", is(66)));
        verify(service).save(any());
    }

    @Test
    void blankConsigneeReturnsFieldError() throws Exception {
        assertFieldError(validJson("").replace("\"consignee\":\"张三\"", "\"consignee\":\" \""),
                "consignee");
    }

    @Test
    void illegalPhoneReturnsFieldError() throws Exception {
        assertFieldError(validJson("").replace("13800138000", "123456"), "phone");
    }

    @Test
    void missingAdministrativeDivisionReturnsFieldError() throws Exception {
        assertFieldError(validJson("").replace("\"districtCode\":\"110101\",", ""), "districtCode");
    }

    @Test
    void illegalDefaultFlagReturnsFieldError() throws Exception {
        assertFieldError(validJson("").replace("\"isDefault\":0", "\"isDefault\":2"), "isDefault");
    }

    @Test
    void updateRequiresAddressId() throws Exception {
        mvc.perform(put("/user/addressBook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson("")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.id").exists());
        verify(service, never()).update(any());
    }

    private void assertFieldError(String json, String field) throws Exception {
        mvc.perform(post("/user/addressBook").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.data." + field).exists());
        verify(service, never()).save(any());
    }

    private String validJson(String extra) {
        return "{" + extra +
                "\"consignee\":\"张三\"," +
                "\"phone\":\"13800138000\"," +
                "\"sex\":\"1\"," +
                "\"provinceCode\":\"110000\"," +
                "\"provinceName\":\"北京市\"," +
                "\"cityCode\":\"110100\"," +
                "\"cityName\":\"北京市\"," +
                "\"districtCode\":\"110101\"," +
                "\"districtName\":\"东城区\"," +
                "\"detail\":\"东华门街道一号院\"," +
                "\"label\":\"家\"," +
                "\"isDefault\":0}";
    }
}
