package com.example.picturebackend.Exception;

import lombok.Getter;

/**
 * 自己定义的符合自己业务的异常类
 */
@Getter
public class BusinessException extends RuntimeException{
    /**
     * 错误码
     */
    private final int code;

    /**
     * 传入错误码和错误信息
     * @param code
     * @param message
     */
    public BusinessException(int code,String message){
        super(message);
        this.code =code;
    }

    /**
     * 仅通过定义的错误码枚举类传入(错误码 and 错误信息)
     * @param errorCode 枚举类中封装好了(错误码,错误信息)，使用时.get即可
     */
    public BusinessException(ErrorCode errorCode){
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 使用枚举类，自己传入错误信息
     * @param errorCode
     * @param message 可以传入更加具体的错误消息
     */
    public BusinessException(ErrorCode errorCode, String message){
        super(message);
        this.code = errorCode.getCode();
    }
}
