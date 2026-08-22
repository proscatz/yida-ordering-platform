package com.yida.service.support;

import com.yida.exception.ImageValidationException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

public final class ImageFileValidator {
    public static final long MAX_UPLOAD_BYTES = 5L * 1024 * 1024;
    private static final Map<String, String> EXTENSION_MIME = Map.of(
            ".jpg", "image/jpeg",
            ".jpeg", "image/jpeg",
            ".png", "image/png",
            ".webp", "image/webp",
            ".gif", "image/gif"
    );

    private ImageFileValidator() {
    }

    public static ValidatedImage validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ImageValidationException(ImageValidationException.Reason.EMPTY, "不能上传空图片");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new ImageValidationException(ImageValidationException.Reason.TOO_LARGE, "图片超过 5MB");
        }

        String extension = extensionOf(file.getOriginalFilename());
        String expectedMime = EXTENSION_MIME.get(extension);
        String declaredMime = normalizeMime(file.getContentType());
        if (expectedMime == null || declaredMime == null) {
            throw new ImageValidationException(ImageValidationException.Reason.UNSUPPORTED_FORMAT,
                    "图片格式不受支持，仅支持 JPG、PNG、WEBP 和 GIF");
        }

        String detectedMime = detectMime(readHeader(file));
        if (!expectedMime.equals(declaredMime) || !expectedMime.equals(detectedMime)) {
            throw new ImageValidationException(ImageValidationException.Reason.CONTENT_MISMATCH,
                    "图片内容与扩展名不一致");
        }
        return new ValidatedImage(extension, detectedMime);
    }

    private static String extensionOf(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        if (dot < 1 || dot == filename.length() - 1) return "";
        return filename.substring(dot).toLowerCase(Locale.ROOT);
    }

    private static String normalizeMime(String contentType) {
        if (contentType == null) return null;
        String value = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return value.isEmpty() ? null : value;
    }

    private static byte[] readHeader(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            return input.readNBytes(12);
        } catch (IOException ex) {
            throw new ImageValidationException(ImageValidationException.Reason.READ_FAILED,
                    "图片读取失败，请重新选择文件", ex);
        }
    }

    static String detectMime(byte[] bytes) {
        if (startsWith(bytes, new int[]{0xFF, 0xD8, 0xFF})) return "image/jpeg";
        if (startsWith(bytes, new int[]{0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A})) return "image/png";
        if (bytes.length >= 6) {
            String signature = new String(Arrays.copyOf(bytes, 6), java.nio.charset.StandardCharsets.US_ASCII);
            if ("GIF87a".equals(signature) || "GIF89a".equals(signature)) return "image/gif";
        }
        if (bytes.length >= 12 && asciiEquals(bytes, 0, "RIFF") && asciiEquals(bytes, 8, "WEBP")) {
            return "image/webp";
        }
        return null;
    }

    private static boolean startsWith(byte[] bytes, int[] signature) {
        if (bytes.length < signature.length) return false;
        for (int i = 0; i < signature.length; i++) {
            if ((bytes[i] & 0xFF) != signature[i]) return false;
        }
        return true;
    }

    private static boolean asciiEquals(byte[] bytes, int offset, String value) {
        for (int i = 0; i < value.length(); i++) {
            if (bytes[offset + i] != (byte) value.charAt(i)) return false;
        }
        return true;
    }

    public record ValidatedImage(String extension, String contentType) {
    }
}
