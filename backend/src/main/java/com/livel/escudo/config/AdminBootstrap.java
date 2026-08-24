package com.livel.escudo.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final JdbcTemplate jdbc; private final PasswordEncoder passwords;
    public AdminBootstrap(JdbcTemplate jdbc, PasswordEncoder passwords){this.jdbc=jdbc;this.passwords=passwords;}
    @Override @Transactional public void run(ApplicationArguments args) {
        String email=System.getenv("ADMIN_EMAIL"), password=System.getenv("ADMIN_PASSWORD");
        if(email==null||email.isBlank()||password==null||password.length()<12)return;
        String normalized=email.trim().toLowerCase();
        Integer count=jdbc.queryForObject("select count(*) from users where lower(email)=?",Integer.class,normalized);
        if(count!=null&&count>0)return;
        UUID id=UUID.randomUUID(); Timestamp now=Timestamp.from(Instant.now());
        jdbc.update("insert into users(id,email,password_hash,status,locale,created_at,updated_at) values(?,?,?,?,?,?,?)",id,normalized,passwords.encode(password),"ACTIVE","es-AR",now,now);
        jdbc.update("insert into user_roles(user_id,role_code) values(?,?)",id,"ROLE_USER");
        jdbc.update("insert into user_roles(user_id,role_code) values(?,?)",id,"ROLE_ADMIN");
        jdbc.update("insert into user_roles(user_id,role_code) values(?,?)",id,"ROLE_SUPER_ADMIN");
    }
}

