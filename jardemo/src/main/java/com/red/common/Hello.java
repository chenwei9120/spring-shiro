package com.red.common;

import org.springframework.stereotype.Component;

@Component
public class Hello {

    public String sayHello(String content) {
        return "hello " + content;
    }
}
