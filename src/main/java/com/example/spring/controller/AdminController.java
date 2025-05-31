package com.example.spring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/v1")
public class AdminController {

    @GetMapping("/create")
    public String create(Model model, @RequestParam(value = "error",required = false) String error){

       if(error!=null){
           String errorMessage = switch (error){
               case "productExist" -> "This product is exist!";
               default -> "Unknown error";
           };
           model.addAttribute("errorMessage", errorMessage);
       }

        return "/create-product";
    }

    @GetMapping("/update")
    public String update(Model model, @RequestParam(value = "error", required = false) String error){

        if(error!=null){
            String errorMessage = switch (error){
                case "productNotExist" -> "This product is not exist!";
                default -> "Unknown error";
            };
            model.addAttribute("errorMessage", errorMessage);
        }

        return "/update-product";
    }


    @GetMapping("/delete")
    public String delete(){
        return "/product-delete";
    }
}
