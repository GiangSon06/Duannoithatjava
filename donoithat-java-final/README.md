# SLAND Furniture — Java Final

Bản chuyển đổi cuối từ source PHP/MySQL sang **Java 17 + Spring Boot 3 + Thymeleaf + JDBC + MySQL**, dùng trực tiếp trong IntelliJ IDEA.

## Nguyên tắc chuyển đổi
- Giữ database MySQL hiện có, không đổi tên 14 bảng nghiệp vụ.
- Giữ các URL `.php` cũ (`index.php`, `products.php`, `checkout.php`, `admin.php`...) để CSS/JS/link/form cũ không bị phá.
- Giữ nguyên `style.css`, `script.js`, `review.js`, toàn bộ `picture/`.
- Backend được tách theo cấu trúc Java: `controller -> service -> repository -> database`.
- Session, giỏ hàng, đăng nhập, tồn kho, transaction, voucher, review, profile/address/avatar, admin, affiliate, chat, email, MoMo đều chạy bằng Java.
- Secret không hard-code; cấu hình qua environment variables.

## Cấu trúc
```text
src/main/java/vn/sland
├── config       # DB compatibility + static upload
├── controller   # MVC / REST endpoints, giữ route .php cũ
├── repository   # JdbcTemplate
├── service      # nghiệp vụ
└── util         # session/cart/money helpers
src/main/resources
├── templates    # Thymeleaf views
├── static       # CSS/JS/picture gốc
└── db            # SQL gốc + compatibility patch
```

## Chạy trong IntelliJ IDEA
1. Cài JDK 17 hoặc 21.
2. Import `src/main/resources/db/database.sql` vào MySQL, database tên `donoithat`.
3. Open thư mục project trong IntelliJ (Open as Maven Project).
4. Copy `.env.example` thành cấu hình Environment Variables trong Run Configuration, tối thiểu đặt `DB_USER`, `DB_PASS` nếu MySQL có mật khẩu.
5. Run `vn.sland.SlandApplication`.
6. Mở `http://localhost:8080/index.php`.

## Cấu hình dịch vụ ngoài
- SMTP: `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`.
- Gemini: `GEMINI_API_KEY`.
- MoMo sandbox/merchant: `MOMO_ACCESS_KEY`, `MOMO_SECRET_KEY`, và các URL callback tương ứng.

Nếu chưa cấu hình các dịch vụ ngoài, website và các luồng DB/local vẫn khởi động; riêng email/AI/MoMo sẽ báo chưa cấu hình thay vì chứa khóa bí mật trong source.

## Tương thích DB
PHP gốc sử dụng `email_verified`, `email_verify_token`, `email_verify_expire` nhưng file SQL gốc không chứa ba cột này. Java final có `DatabaseCompatibilityConfig` và `db/compatibility_patch.sql` để **chỉ thêm cột còn thiếu**, không xóa hay đổi dữ liệu hiện có.
