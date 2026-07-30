package com.example.picturebackend.constant;

import lombok.Data;

@Data
public class UserConstant {
    public static final String CURRENT_USER_SESSION_KEY = "CurrentUser";
    public static final String DEFAULT_ROLE = "user";
    public static final String ADMIN_ROLE = "admin";
    public static final String VIP_ROLE = "vip";
}
