package com.example.picturebackend.domain.MyEnums;

import lombok.Getter;

@Getter
public enum Gender {
    GENDER_MEN("男",1),
    GENDER_WOMEN("女",2);

    private final String description;
    private final Integer gender;
    Gender(String description, int gender) {
        this.description = description;
        this.gender = gender;
    }
}
