package com.springboot.demo.controller;

import com.red.model.Customer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.ArrayList;

@RestController("/customer")
public class CustomerController {

    @GetMapping("/list")
    public List<Customer> queryCustomer(){
        return new ArrayList<>();
    }
}
