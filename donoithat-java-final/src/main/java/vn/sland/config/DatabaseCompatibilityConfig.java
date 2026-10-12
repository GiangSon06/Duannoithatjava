package vn.sland.config;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The PHP source references verification columns that are absent from the supplied SQL dump.
 * This compatibility initializer only adds missing columns; it never drops/renames existing data.
 */
@Component
public class DatabaseCompatibilityConfig {
    private final JdbcTemplate jdbc;
    public DatabaseCompatibilityConfig(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @PostConstruct
    public void ensureCompatibilityColumns() {
        try {
            List<String> cols = jdbc.queryForList(
                    "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='users'",
                    String.class);
            add(cols, "email_verified", "ALTER TABLE users ADD COLUMN email_verified TINYINT(1) NOT NULL DEFAULT 1 AFTER is_locked");
            add(cols, "email_verify_token", "ALTER TABLE users ADD COLUMN email_verify_token VARCHAR(128) NULL AFTER email_verified");
            add(cols, "email_verify_expire", "ALTER TABLE users ADD COLUMN email_verify_expire DATETIME NULL AFTER email_verify_token");
        } catch (Exception ignored) {
            // The app may start before the user imports the database. No destructive action is taken.
        }
    }

    private void add(List<String> cols, String name, String ddl) {
        if (cols.stream().noneMatch(name::equalsIgnoreCase)) jdbc.execute(ddl);
    }
}
