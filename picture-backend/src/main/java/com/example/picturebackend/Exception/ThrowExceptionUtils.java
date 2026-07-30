package com.example.picturebackend.Exception;

/**
 * 抛异常工具类
 */
public class ThrowExceptionUtils {
    /**
     * 判断if来抛异常
     * @param condition 判断条件
     * @param runtimeException 要抛的异常
     */
    public static void throwIF(boolean condition, RuntimeException runtimeException){
        if (condition){
            throw runtimeException;
        }
    }

    /**
     * 判断if来抛异常
     * @param condition 判断条件
     * @param errorCode 错误码
     */
    public static void throwIF(boolean condition, ErrorCode errorCode){
        throwIF(condition,new BusinessException(errorCode));
    }

    /**
     * 判断if来抛异常
     * @param condition 判断条件
     * @param errorCode 错误码
     * @param message 更具体的错误信息
     */
    public static void throwIF(boolean condition, ErrorCode errorCode, String message){
        throwIF(condition,new BusinessException(errorCode, message));
    }
}
