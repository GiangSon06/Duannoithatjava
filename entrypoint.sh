#!/bin/bash
set -e

echo "=== [1/3] Khởi động MariaDB Server tối ưu bộ nhớ ==="
service mariadb start

echo "=== [2/3] Kiểm tra và nạp dữ liệu database.sql ==="
mysql -e "CREATE DATABASE IF NOT EXISTS donoithat CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED BY 'Giangson@05'; FLUSH PRIVILEGES;" 2>/dev/null || true

# Import database nếu bảng products chưa có
TABLE_COUNT=$(mysql -u root -p'Giangson@05' -D donoithat -sse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='donoithat';" 2>/dev/null || echo "0")
if [ "$TABLE_COUNT" -eq "0" ]; then
    echo "Dang import database.sql vao CSDL donoithat..."
    mysql -u root -p'Giangson@05' donoithat < /app/database.sql
    echo "Import CSDL thanh cong!"
else
    echo "CSDL da ton tai ($TABLE_COUNT tables)."
fi

echo "=== [3/3] Khởi chạy Spring Boot trên cổng ${PORT:-8080} ==="
exec java -Xmx260m -Xss512k -XX:+UseSerialGC -Dserver.port=${PORT:-8080} -jar /app/app.jar
