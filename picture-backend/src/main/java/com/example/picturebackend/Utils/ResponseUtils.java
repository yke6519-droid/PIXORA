package com.example.picturebackend.Utils;

import com.example.picturebackend.Exception.ErrorCode;
import com.example.picturebackend.domain.request.BaseResponse;
import lombok.Data;

/**
 * 返回工具类
 */
@Data
public class ResponseUtils {
    /**
     * 成功返回
     * @param data 返回数据
     * @param message 返回信息
     * @return
     * @param <T>
     */
    public static <T> BaseResponse<T> success(T data, String message){
        return new BaseResponse<>(200,data,message);
    }

    /**
     * 成功返回
     * @param data
     * @return
     * @param <T>
     */
    public static <T>BaseResponse<T> success(T data){
        return new BaseResponse<>(200,data,"");
    }

    /**
     * 成功返回
     * @param code 响应码
     * @param data 返回数据
     * @return
     * @param <T>
     */
    public static <T>BaseResponse<T> success(int code, T data){
        return new BaseResponse<>(code,data);
    }

    /**
     * 错误返回
     * @param errorCode 错误码
     * @return
     * @param <T>
     */
    public static <T>BaseResponse<T> error(ErrorCode errorCode){
        return new BaseResponse<>(errorCode);
    }

    /**
     * 返回异常
     * @param code 错误码
     * @param message 错误信息
     * @return
     * @param <T>
     */
    public static <T>BaseResponse<T> error(int code, String message){
        return new BaseResponse<>(code,null,message);
    }
}
