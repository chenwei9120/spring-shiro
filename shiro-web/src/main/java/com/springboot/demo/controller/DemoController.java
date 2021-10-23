package com.springboot.demo.controller;

import com.red.Jardemo;
import com.red.common.Hello;
import com.red.model.Address;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo")
public class DemoController {
    @Autowired
    private Hello hello;

    @Autowired
    private Jardemo jardemo;

    @Autowired
    private Address address;

    @GetMapping("/test")
    public String Test() {
        System.out.println(hello.sayHello("I am way"));
        System.out.println(jardemo.echo("I am way"));
        return "test method.";
    }

    @GetMapping("/address")
    public String GetAddress() {
        return address.toString();
    }
}
