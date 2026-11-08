package sn.senetech.intervit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Point d'entrée de l'API IntervIT (plateforme de gestion des interventions techniques).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class IntervitApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntervitApplication.class, args);
    }
}
