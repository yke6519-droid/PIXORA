package com.example.picturebackend.Exception;

import com.example.picturebackend.domain.request.BaseResponse;
import com.example.picturebackend.Utils.ResponseUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    /**
     * 自定义异常处理器
     * @param e BusinessException
     * @return
     */
    @ExceptionHandler(BusinessException.class) // 指定我们要捕获的目标异常的类型：这是捕获我们自己定义的异常
    public BaseResponse<?> businessExceptionHandler(BusinessException e){
        log.error("BusinessException",e);
        return ResponseUtils.error(e.getCode(),e.getMessage());
    }

    /**
     * 运行时异常处理器
     * @param e RuntimeException
     * @return
     */
    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> RuntimeExceptionHandler(RuntimeException e){
        log.error("RuntimeException",e);
        return ResponseUtils.error(ErrorCode.SYSTEM_ERROR);
    }
}
