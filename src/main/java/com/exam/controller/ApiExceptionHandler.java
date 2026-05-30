package com.exam.controller;

import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({IllegalArgumentException.class, NoSuchElementException.class, MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public Map<String, String> badRequest(Exception ex) {
        return Map.of("message", ex.getMessage() == null ? "请求参数错误" : ex.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Map<String, String> dataIntegrity(DataIntegrityViolationException ex) {
        return Map.of("message", "该数据已被题目、考试、成绩或课程班使用，不能直接删除");
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public Map<String, String> serverError(Exception ex) {
        return Map.of("message", "系统执行失败：" + ex.getClass().getSimpleName());
    }
}
