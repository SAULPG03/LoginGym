package com.saul.loginGym;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.saul.loginGym.service.LoginGymService;

@SpringBootApplication
public class LoginGymApplication implements CommandLineRunner {

    @Autowired
    private LoginGymService loginGymService;

    public static void main(String[] args) {
        SpringApplication.run(LoginGymApplication.class, args);
    }

    @Override
    public void run(String... args) {
        loginGymService.loginTrainingGym();
    }
}


