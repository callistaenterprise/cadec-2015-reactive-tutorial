package se.callista.cadec2015.tutorial.reactive.serviceprovider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@ComponentScan
@EnableAutoConfiguration
public class Application {

    @Bean(name="timerService")
    public ScheduledExecutorService getTimerService() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}