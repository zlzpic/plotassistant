package com.bdu.plotassistant;

import com.bdu.plotassistant.config.AiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AiProperties.class)
public class PlotassistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlotassistantApplication.class, args);
    }

}
