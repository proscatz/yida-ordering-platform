package com.yida.controller.admin;

import com.yida.exception.ImageValidationException;
import com.yida.result.Result;
import com.yida.utils.AliOssUtil;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CommonControllerSecurityTest {
    private final AliOssUtil oss = mock(AliOssUtil.class);
    private final CommonController controller = new CommonController(oss);

    @Test
    void emptyFileIsRejected() {
        assertReason(ImageValidationException.Reason.EMPTY,
                new MockMultipartFile("file", "a.png", "image/png", new byte[0]));
    }

    @Test
    void unsupportedExtensionIsRejected() {
        assertReason(ImageValidationException.Reason.UNSUPPORTED_FORMAT,
                new MockMultipartFile("file", "a.svg", "image/svg+xml", pngBytes(32)));
    }

    @Test
    void declaredMimeMustMatchExtension() {
        assertReason(ImageValidationException.Reason.CONTENT_MISMATCH,
                new MockMultipartFile("file", "a.jpg", "image/png", pngBytes(32)));
    }

    @Test
    void forgedImageContentIsRejected() {
        assertReason(ImageValidationException.Reason.CONTENT_MISMATCH,
                new MockMultipartFile("file", "a.png", "image/png", "not-image".getBytes()));
    }

    @Test
    void oneAndHalfMegabyteImageCanEnterController() {
        assertAccepted(1536 * 1024);
    }

    @Test
    void fourPointNineMegabyteImageIsAllowed() {
        assertAccepted((int) (4.9 * 1024 * 1024));
    }

    @Test
    void fileOverFiveMegabytesIsRejected() {
        assertReason(ImageValidationException.Reason.TOO_LARGE,
                new MockMultipartFile("file", "a.png", "image/png",
                        pngBytes((int) CommonController.MAX_UPLOAD_BYTES + 1)));
    }

    @Test
    void allowedImageUsesSafeDatedObjectNameAndDetectedMime() {
        when(oss.upload(any(), matches("catalog/\\d{4}/\\d{2}/\\d{2}/[0-9a-f]{32}\\.png"), eq("image/png")))
                .thenReturn("https://static.example.test/catalog/image.png");
        Result<String> result = controller.upload(
                new MockMultipartFile("file", "../a.png", "image/png", pngBytes(64)));
        assertEquals(1, result.getCode());
        assertEquals("https://static.example.test/catalog/image.png", result.getData());
    }

    private void assertAccepted(int size) {
        when(oss.upload(any(), any(), eq("image/png"))).thenReturn("https://static.example.test/image.png");
        Result<String> result = controller.upload(
                new MockMultipartFile("file", "a.png", "image/png", pngBytes(size)));
        assertEquals(1, result.getCode());
        verify(oss).upload(any(), any(), eq("image/png"));
    }

    private void assertReason(ImageValidationException.Reason reason, MockMultipartFile file) {
        ImageValidationException exception = assertThrows(ImageValidationException.class,
                () -> controller.upload(file));
        assertEquals(reason, exception.getReason());
        verifyNoInteractions(oss);
    }

    private static byte[] pngBytes(int size) {
        byte[] bytes = new byte[Math.max(size, 12)];
        byte[] signature = new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        System.arraycopy(signature, 0, bytes, 0, signature.length);
        Arrays.fill(bytes, signature.length, bytes.length, (byte) 1);
        return bytes;
    }
}
