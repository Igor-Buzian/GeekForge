package com.example.spring.controller;

import com.example.spring.dto.RegisterDtoValues;
import com.example.spring.service.AccountSecurityService;
import com.example.spring.service.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;


@Controller
@RequiredArgsConstructor
public class ViewController {
    private final LoginAttemptService loginAttemptService;
    private final AccountSecurityService accountSecurityService;
    @GetMapping("/login")
    public String login(Model model, @Value("${security.login.capthca_count}") byte captchaCount, @RequestParam(name = "error", required = false)String error,@RequestParam(name = "attempt",required = false)Byte attempt)
    {
        model.addAttribute("captchaThreshold",captchaCount);
        if(error != null){
            String errorMessage = switch (error)
            {
                case "ip_banned" -> "You is too much try to enter in this cite so you have ban for few time";
                case "incorrect" -> "Incorrect login or password "+ (attempt !=null ? attempt + " /"+loginAttemptService.getCount_attempts() :"");
                case "wrong" -> "Incorrect login or password "+ (attempt !=null ? attempt + " /"+accountSecurityService.getMax_failed_attempts() :"");
                case "account_banned" -> "Your account was banned";
                default -> "Authentication error";
            };
            model.addAttribute("errorMessage",errorMessage);
        }
        return "login-form";
    }
    @GetMapping("/register")
    public String register(Model model, RegisterDtoValues registerDtoValues,  @RequestParam(name = "error",required = false)String error)
    {
        // Добавляем пустой объект RegisterDtoValues в модель
        model.addAttribute("registerDtoValues", registerDtoValues);
        if(error!=null)
        {
            String errorMessage = switch (error){
                case "exist_mail" -> "This mail is exist. Please, Try Again!";
                case  "ip_banned" -> "You have a ban for few time. Please, wait!";
                case "different_password" -> "You have different passwords!";
                default -> "Register error";
            };
            model.addAttribute("errorMessage",errorMessage);
        }
        return "register-form";
    }


    @GetMapping("/success")
    public String success()
    {
        return "success";
    }

}
