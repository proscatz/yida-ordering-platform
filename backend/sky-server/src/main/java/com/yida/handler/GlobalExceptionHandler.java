package com.yida.handler;

import com.yida.constant.MessageConstant;
import com.yida.exception.BaseException;
import com.yida.exception.AddressValidationException;
import com.yida.exception.EmployeeStatusConflictException;
import com.yida.exception.ForbiddenOperationException;
import com.yida.exception.ImageValidationException;
import com.yida.exception.OssStorageException;
import com.yida.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

import javax.validation.ConstraintViolationException;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Map<String, String>>> methodArgumentNotValidHandler(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(validationResult(errors));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Map<String, String>>> bindHandler(BindException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(validationResult(errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Map<String, String>>> constraintViolationHandler(
            ConstraintViolationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String path = violation.getPropertyPath().toString();
            String field = path.substring(path.lastIndexOf('.') + 1);
            errors.putIfAbsent(field, violation.getMessage());
        });
        return ResponseEntity.badRequest().body(validationResult(errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Map<String, String>>> messageNotReadableHandler(
            HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(validationResult(
                Map.of("request", "请求参数格式不正确")));
    }

    @ExceptionHandler(AddressValidationException.class)
    public ResponseEntity<Result<Map<String, String>>> addressValidationHandler(
            AddressValidationException ex) {
        return ResponseEntity.badRequest().body(validationResult(Map.of(ex.getField(), ex.getMessage())));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Object>> maxUploadSizeHandler(MaxUploadSizeExceededException ex) {
        log.warn("图片上传被容器拒绝 type={}", ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Result.error("图片超过 5MB"));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Result<Object>> multipartHandler(MultipartException ex) {
        log.warn("Multipart 请求解析失败 type={}", ex.getClass().getSimpleName());
        return ResponseEntity.badRequest().body(Result.error("图片上传请求格式不正确"));
    }

    @ExceptionHandler(ImageValidationException.class)
    public ResponseEntity<Result<Object>> imageValidationHandler(ImageValidationException ex) {
        HttpStatus status = ex.getReason() == ImageValidationException.Reason.TOO_LARGE
                ? HttpStatus.PAYLOAD_TOO_LARGE : HttpStatus.BAD_REQUEST;
        log.warn("图片校验失败 type={}", ex.getReason());
        return ResponseEntity.status(status).body(Result.error(ex.getMessage()));
    }

    @ExceptionHandler(OssStorageException.class)
    public ResponseEntity<Result<Object>> ossStorageHandler(OssStorageException ex) {
        HttpStatus status = switch (ex.getReason()) {
            case CONFIGURATION, AUTHENTICATION, BUCKET_NOT_FOUND, TIMEOUT -> HttpStatus.SERVICE_UNAVAILABLE;
            case SERVICE_UNAVAILABLE, UNKNOWN -> HttpStatus.BAD_GATEWAY;
        };
        log.error("OSS 存储异常 type={} code={} requestId={} object={}",
                ex.getReason(), ex.getErrorCode(), ex.getRequestId(), ex.getObjectName());
        return ResponseEntity.status(status).body(Result.error(ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result forbiddenHandler(ForbiddenOperationException ex) {
        log.warn("员工权限校验失败：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler(EmployeeStatusConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result employeeStatusConflictHandler(EmployeeStatusConflictException ex) {
        log.warn("员工状态冲突：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }
    @ExceptionHandler
    public Result exceptionHandler(SQLIntegrityConstraintViolationException ex) {
        String msg = ex.getMessage();

        if (msg.contains("PRIMARY")) {
            String[] split = msg.split("'");
            String id = split[1];
            return Result.error("ID[" + id + "]已存在");
        }

        else if (msg.contains("idx_username")) {
            String[] split = msg.split("'");
            String username = split[1];
            return Result.error(username + "已存在");
        }

        return Result.error(MessageConstant.UNKNOWN_ERROR);
    }

    private Result<Map<String, String>> validationResult(Map<String, String> errors) {
        String message = errors.values().stream().findFirst().orElse("请求参数不正确");
        Result<Map<String, String>> result = Result.error(message);
        result.setData(errors);
        return result;
    }

}
