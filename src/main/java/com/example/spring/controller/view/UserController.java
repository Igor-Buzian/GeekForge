package com.example.spring.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
//@RequestMapping("/api/v1/user")
public class UserController {
    @GetMapping("/")
    public String unauthorizedAllProducts(){
        return "main page/unauthorized-main-list";
    }

    @GetMapping("/api/v1/user/all-products")
    public String allProducts(){
        return "main page/products-list";
    }


}

