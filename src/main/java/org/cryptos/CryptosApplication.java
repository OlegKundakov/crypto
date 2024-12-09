package org.cryptos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Currencies application.
 * This class starts the Spring Boot application and starts the embedded server.
 */
@SpringBootApplication
public class CryptosApplication {

    /**
     * Main method to launch the Currencies application.
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(CryptosApplication.class, args);
    }

}
