package com.yida.controller.admin;

import com.yida.result.Result;
import com.yida.service.support.ImageFileValidator;
import com.yida.service.support.ImageFileValidator.ValidatedImage;
import com.yida.utils.AliOssUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/common")
public class CommonController {
    static final long MAX_UPLOAD_BYTES = ImageFileValidator.MAX_UPLOAD_BYTES;
    private static final DateTimeFormatter OBJECT_DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private final AliOssUtil aliOssUtil;

    public CommonController(AliOssUtil aliOssUtil) {
        this.aliOssUtil = aliOssUtil;
    }

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam(value = "scope", defaultValue = "catalog") String scope) {
        if (!"catalog".equals(scope) && !"diagnostic".equals(scope)) {
            throw new com.yida.exception.ImageValidationException(
                    com.yida.exception.ImageValidationException.Reason.UNSUPPORTED_FORMAT,
                    "图片上传用途不正确");
        }
        ValidatedImage image = ImageFileValidator.validate(file);
        String objectName = scope + "/" + LocalDate.now().format(OBJECT_DATE) + "/"
                + UUID.randomUUID().toString().replace("-", "") + image.extension();
        try {
            return Result.success(aliOssUtil.upload(file.getBytes(), objectName, image.contentType()));
        } catch (java.io.IOException ex) {
            throw new com.yida.exception.ImageValidationException(
                    com.yida.exception.ImageValidationException.Reason.READ_FAILED,
                    "图片读取失败，请重新选择文件", ex);
        }
    }

    Result<String> upload(MultipartFile file) {
        return upload(file, "catalog");
    }

    @GetMapping("/storage/health")
    public Result<Map<String, String>> storageHealth() {
        aliOssUtil.checkHealth();
        return Result.success(Map.of("provider", "ALIYUN_OSS", "status", "UP"));
    }
}
