package com.example.picturebackend.Utils;

import com.example.picturebackend.Exception.BusinessException;
import com.example.picturebackend.Exception.ThrowExceptionUtils;
import com.example.picturebackend.Exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ThrowExceptionUtilsTest {

    @Test
    void shouldThrowBusinessExceptionWhenConditionIsTrue() {
        // 条件成立时，工具类应抛出调用方传入的业务异常。
        assertThrows(
                BusinessException.class,
                () -> ThrowExceptionUtils.throwIF(true, new BusinessException(ErrorCode.NOT_FOUND_ERROR))
        );
    }

    @Test
    void shouldNotThrowExceptionWhenConditionIsFalse() {
        // 条件不成立时，不应中断后续业务流程。
        assertDoesNotThrow(
                () -> ThrowExceptionUtils.throwIF(false, new BusinessException(ErrorCode.NOT_FOUND_ERROR))
        );
    }
}
