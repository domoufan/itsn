package sn.senetech.itsn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Point d'entrée de l'API ITSN (plateforme de gestion des interventions techniques).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class ItsnApplication {

    public static void main(String[] args) {
        SpringApplication.run(ItsnApplication.class, args);
    }
}
