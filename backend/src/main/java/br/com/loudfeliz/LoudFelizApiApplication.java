package br.com.loudfeliz;

import br.com.loudfeliz.config.BancoUrl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LoudFelizApiApplication {

    public static void main(String[] args) {
        BancoUrl.aplicar(System.getenv("DATABASE_URL"), System.getenv("DB_NAME"));
        SpringApplication.run(LoudFelizApiApplication.class, args);
    }
}
