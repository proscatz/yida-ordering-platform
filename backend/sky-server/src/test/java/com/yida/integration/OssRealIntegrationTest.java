package com.yida.integration;

import com.yida.exception.OssStorageException;
import com.yida.utils.AliOssUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfSystemProperty(named = "oss.it", matches = "true")
class OssRealIntegrationTest {
    private static final byte[] PNG = new byte[]{
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a,
            0, 0, 0, 0
    };

    @Autowired
    private AliOssUtil aliOssUtil;

    @Test
    void uploadsOnlyToDedicatedDiagnosticPrefix() {
        String objectName = "diagnostic/" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                + "/integration-" + UUID.randomUUID().toString().replace("-", "") + ".png";
        try {
            String url = aliOssUtil.upload(PNG, objectName, "image/png");
            assertTrue(url.endsWith(objectName));
        } catch (OssStorageException ex) {
            fail("Real OSS diagnostic failed safely: " + ex.getReason());
        }
    }
}
