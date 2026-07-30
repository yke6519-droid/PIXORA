package com.example.picturebackend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 应用生效范围
@Target(ElementType.METHOD)
// 运行时生效
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {
    /**
     * 必须具有某个角色
     * 即：用户必须登录了，才能进行某些操作
     * @return
     */
    String mustRole() default "";
}
