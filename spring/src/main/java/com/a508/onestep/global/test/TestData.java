package com.a508.onestep.global.test;

import lombok.Getter;

@Getter
public class TestData {

    private Long id;
    private String name;

    public TestData(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
