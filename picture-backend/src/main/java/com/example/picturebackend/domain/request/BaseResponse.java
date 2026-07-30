package com.example.picturebackend.domain.request;

import com.example.picturebackend.Exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

@Data
public class BaseResponse<T> implements Serializable { // 在不确定返回的数据的类型时，用T范型来指代
    private int code;
    private String message;
    private T data;

    /**
     * 返回结果
     * @param code 响应码
     * @param data 返回结果
     * @param message 描述
     */
    public BaseResponse(int code ,T data ,String message){
        this.code =code;
        this.data =data;
        this.message =message;
    }

    /**
     * 返回结果
     * @param code 响应码
     * @param data 返回结果
     */
    public BaseResponse(int code,T data){
        this(code,data,"");
    }

    /**
     * 返回错误结果
     * @param errorCode 错误码
     */
    public BaseResponse(ErrorCode errorCode){
        this(errorCode.getCode(),null,errorCode.getMessage());
    }
}
