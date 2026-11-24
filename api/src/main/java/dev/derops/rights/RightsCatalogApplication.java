package dev.derops.rights;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RightsCatalogApplication {

    public static void main(String[] args) {
        SpringApplication.run(RightsCatalogApplication.class, args);
    }
}
