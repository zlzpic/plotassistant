package com.bdu.plotassistant.utils;

import com.bdu.plotassistant.dto.ApiResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(com.bdu.plotassistant.utils.BizException.class)
    public ApiResult<Void> handleBiz(com.bdu.plotassistant.utils.BizException e) {
        return new ApiResult<>(400, e.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleOther(Exception e) {

        e.printStackTrace();
        return new ApiResult<>(500, "系统异常", null);
    }
}
