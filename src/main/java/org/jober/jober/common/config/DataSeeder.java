package org.jober.jober.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Replaces the 'CHANGE_ME' placeholder hashes from schema.sql with a real BCrypt hash. */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String PLACEHOLDER = "CHANGE_ME";

    private final JdbcClient jdbc;
    private final PasswordEncoder passwordEncoder;
    private final String defaultPassword;

    public DataSeeder(JdbcClient jdbc, PasswordEncoder passwordEncoder,
                      @Value("${app.seed.default-password}") String defaultPassword) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.defaultPassword = defaultPassword;
    }

    @Override
    public void run(String... args) {
        String hash = passwordEncoder.encode(defaultPassword);
        int students = jdbc.sql("UPDATE students SET password_hash = ? WHERE password_hash = ?")
                .params(hash, PLACEHOLDER).update();
        int companies = jdbc.sql("UPDATE companies SET password_hash = ? WHERE password_hash = ?")
                .params(hash, PLACEHOLDER).update();
        if (students + companies > 0) {
            log.info("Seed accounts ready ({} students, {} companies), password: {}", students, companies, defaultPassword);
        }
    }
}
