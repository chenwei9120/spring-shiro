package com.springboot.demo.controller;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/user")
public class UserController {

    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    public String logout() {
        Subject subject = SecurityUtils.getSubject();
        subject.logout();
        return "logout method";
    }

    @PostMapping(value = "/login")
    @ResponseBody
    public String login(String username, String password, HttpServletRequest request) {
//        String username = request.getParameter("username");
//        String password = request.getParameter("password");
        //获取主题对象
        Subject subject = SecurityUtils.getSubject();
        try {
            subject.login(new UsernamePasswordToken(username, password));
        } catch (UnknownAccountException e) {
            e.printStackTrace();
            return "用户错误";
        } catch (IncorrectCredentialsException e) {
            return "密码错误";
        }
        return "";
    }
}

