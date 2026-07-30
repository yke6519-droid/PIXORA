package com.example.picturebackend.Utils;

import com.example.picturebackend.Exception.BusinessException;
import com.example.picturebackend.Exception.ThrowExceptionUtils;
import com.example.picturebackend.Exception.ErrorCode;
import org.junit.jupiter.api.Test;

class ThrowExceptionUtilsTest {

    @Test
    void throwIF() {
        ThrowExceptionUtils.throwIF(10>1, new BusinessException(ErrorCode.NOT_FOUND_ERROR));
    }
}