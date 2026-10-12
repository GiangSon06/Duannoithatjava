package vn.sland.config;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tự thêm các cột còn thiếu (tương thích MySQL). Không xóa/đổi tên dữ liệu có sẵn.
 */
@Component
public class DatabaseCompatibilityConfig {
    private final JdbcTemplate jdbc;

    public DatabaseCompatibilityConfig(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void ensureCompatibilityColumns() {
        ensure("users", "email_verified",
                "ALTER TABLE users ADD COLUMN email_verified TINYINT(1) NOT NULL DEFAULT 1");
        ensure("users", "email_verify_token",
                "ALTER TABLE users ADD COLUMN email_verify_token VARCHAR(128) NULL");
        ensure("users", "email_verify_expire",
                "ALTER TABLE users ADD COLUMN email_verify_expire DATETIME NULL");
        ensure("orders", "shipping_fee",
                "ALTER TABLE orders ADD COLUMN shipping_fee DECIMAL(15,2) NOT NULL DEFAULT 0.00");
    }

    private void ensure(String table, String column, String ddl) {
        try {
            List<String> cols = jdbc.queryForList(
                    "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                    String.class, table);
            if (cols.stream().noneMatch(column::equalsIgnoreCase)) {
                jdbc.execute(ddl);
                System.out.println("[DbCompat] Added column " + table + "." + column);
            }
        } catch (Exception e) {
            System.err.println("[DbCompat] Skip " + table + "." + column + ": " + e.getMessage());
        }
    }
}