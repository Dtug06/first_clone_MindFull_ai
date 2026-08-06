package com.mindbridge;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.test.context.ActiveProfiles;

/**
 * G1-T03 smoke test: verify the Spring context can boot with the
 * `test` profile and that the JPA datasource / EntityManager wiring is
 * intact. Uses the in-memory H2 configured in `application-test.yml`,
 * so this test never touches a real PostgreSQL instance.
 *
 * <p>What we deliberately do <em>not</em> assert here:
 * <ul>
 *   <li>No JPA entities exist yet (T06+) — schema validation is
 *       therefore not part of this test.</li>
 *   <li>No {@code @Transactional} writes — there is nothing to write.</li>
 * </ul>
 *
 * <p>This test exists to make sure the configuration added in G1-T03
 * (datasource URL parsing, HikariCP startup, Hibernate dialect
 * resolution, UTC timezone setting) does not silently regress.
 */
@SpringBootTest
@ActiveProfiles("test")
class DatabaseContextSmokeTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private LocalContainerEntityManagerFactoryBean entityManagerFactory;

    @Test
    void contextBootsAndDataSourceIsWired() {
        assertNotNull(dataSource, "DataSource bean must be wired by Spring Boot");
        assertNotNull(entityManagerFactory, "EntityManagerFactory bean must be wired by Spring Boot");
    }
}