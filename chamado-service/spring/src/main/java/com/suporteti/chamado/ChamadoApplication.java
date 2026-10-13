package com.suporteti.chamado;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChamadoApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChamadoApplication.class, args);
    }
}
