package br.com.reinaldodev.smartbilling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmartBillingApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartBillingApplication.class, args);
    }
}
