package com.mondora;

import com.mondora.facebook.sending.Sender;
import com.mondora.facebook.sending.model.PusherThread;
import com.mondora.model.FBUser;
import com.mondora.model.Fattura;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.annotation.EnableJms;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;

@SpringBootApplication
@EnableJms
public class Application {

    public static void main(String[] args) {

        Objects.requireNonNull(System.getenv("BUS_USER"), "BUS_USER must not be null");
        Objects.requireNonNull(System.getenv("BUS_PASS"), "BUS_PASS must not be null");
        Objects.requireNonNull(System.getenv("BUS_HOST"), "BUS_HOST must not be null");
        Objects.requireNonNull(System.getenv("TOPIC_NAME"), "TOPIC_NAME must not be null");
        Objects.requireNonNull(System.getenv("SUBSCRIPTION_NAME"), "SUBSCRIPTION_NAME must not be null");
        Objects.requireNonNull(System.getenv("SUBSCRIPTION_NAME"), "SUBSCRIPTION_NAME must not be null");
        Objects.requireNonNull(System.getenv("FACEBOOK_PAGE_ACCESS_TOKEN"), "FACEBOOK_PAGE_ACCESS_TOKEN must not be null");

        startup();
        SpringApplication.run(Application.class, args);
    }

    static public void startup() {
        int day = 86400000;
        for( int i=0; i<7; i++) {
            Database.addFattura(
                    new Fattura(
                            new Date(System.currentTimeMillis() + ( i * day/3 ) * (i%2==0?-1:1)) ,
                            Database.randomCustomer(),
                            Math.random() * 500
                    )
            );
        }

        new Thread(new PusherThread()).run();
        Collection<FBUser> users = Database.listAllUsers();
        users.forEach(u -> {
            Sender.sendTextMessage(u.messenger_id, "Agyo bot is back.");
        });
    }

    @PreDestroy
    public void shutdown() {
        Collection<FBUser> users = Database.listAllUsers();
        users.forEach(u -> {
            Sender.sendTextMessage(u.messenger_id, "Agyo bot is shutting down.");
        });
    }

}