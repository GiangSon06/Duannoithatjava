#!/bin/bash
set -e

echo "=== [1/3] Khởi động MariaDB Server ==="
service mariadb start

# Đợi MariaDB socket sẵn sàng
for i in {1..30}; do
    if mysqladmin ping --silent 2>/dev/null; then
        echo "MariaDB da san sang!"
        break
    fi
    echo "Dang cho MariaDB khoi dong ($i)..."
    sleep 1
done

echo "=== [2/3] Cau hinh User & Import CSDL ==="
# Thay thế collation MySQL 8 sang MariaDB (tránh lỗi 1273)
sed -i 's/utf8mb4_0900_ai_ci/utf8mb4_unicode_ci/g' /app/database.sql 2>/dev/null || true

# Tạo database
mysql -e "CREATE DATABASE IF NOT EXISTS donoithat CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null || true

# Cấu hình tài khoản root có mật khẩu Giangson@05 cho kết nối TCP từ Java
mysql -e "
CREATE USER IF NOT EXISTS 'root'@'127.0.0.1' IDENTIFIED BY 'Giangson@05';
CREATE USER IF NOT EXISTS 'root'@'localhost' IDENTIFIED BY 'Giangson@05';
SET PASSWORD FOR 'root'@'localhost' = PASSWORD('Giangson@05');
SET PASSWORD FOR 'root'@'127.0.0.1' = PASSWORD('Giangson@05');
GRANT ALL PRIVILEGES ON *.* TO 'root'@'localhost' WITH GRANT OPTION;
GRANT ALL PRIVILEGES ON *.* TO 'root'@'127.0.0.1' WITH GRANT OPTION;
FLUSH PRIVILEGES;
" 2>/dev/null || true

# Import CSDL trực tiếp qua socket quyền nội bộ
echo "Dang nap du lieu database.sql vao CSDL donoithat..."
mysql donoithat < /app/database.sql 2>/dev/null || mysql -u root -p'Giangson@05' donoithat < /app/database.sql 2>/dev/null || true
echo "Nap du lieu hoan tat!"

echo "=== [3/3] Khoi chay GS LUXURY Spring Boot tren cong ${PORT:-8080} ==="
exec java -Xmx260m -Xss512k -XX:+UseSerialGC -Dserver.port=${PORT:-8080} -jar /app/app.jar
