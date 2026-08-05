package com.example.picturebackend.constant;

import lombok.Data;

@Data
public class SpaceConstant {
    public static final Integer NORMAL_LEVEL = 0;
    public static final Long NORMAL_MAX_SIZE = (long) (100*1024*1024);
    public static final Long NORMAL_MAX_Count = 50L;
    public static final Integer Professional_LEVEL = 1;
    public static final Long Professional_MAX_SIZE = (long) (500 * 1024 *1024);
    public static final Long Professional_MAX_Count = 100L;
    public static final Integer Expert_LEVEL = 2;
    public static final Long Expert_MAX_SIZE = (long) (1000 * 1024 *1024);
    public static final Long Expert_MAX_Count = 200L;
}
