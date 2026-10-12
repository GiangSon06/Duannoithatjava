package vn.sland.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import vn.sland.repository.AppRepository;
import vn.sland.util.WebUtil;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class AuthService {
    private final AppRepository repo;
    private final EmailService email;
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
    private final String baseUrl;
    private static final Pattern USER = Pattern.compile("^[A-Za-z0-9_.@+-]{3,50}$");
    private static final Pattern MAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public AuthService(AppRepository repo, EmailService email,
            @Value("${app.base-url:http://localhost:8081}") String baseUrl) {
        this.repo = repo;
        this.email = email;
        this.baseUrl = baseUrl;
    }

    public String validateRegistration(String username, String mail, String pass, String confirm) {
        if (username == null || username.isBlank())
            return "Tên đăng nhập không được để trống!";
        if (!USER.matcher(username).matches())
            return "Tên đăng nhập phải từ 3–50 ký tự (chữ cái, chữ số, dấu _, ., hoặc @)!";
        if (mail == null || !MAIL.matcher(mail).matches())
            return "Email không đúng định dạng!";
        if (pass == null || pass.length() < 6)
            return "Mật khẩu phải có ít nhất 6 ký tự!";
        if (pass.length() > 50)
            return "Mật khẩu không được vượt quá 50 ký tự!";
        if (!pass.matches(".*[A-Z].*"))
            return "Mật khẩu cần có ít nhất 1 chữ hoa!";
        if (!Objects.equals(pass, confirm))
            return "Mật khẩu nhập lại không khớp!";
        if (repo.one("SELECT id FROM users WHERE username=? OR email=? LIMIT 1", username, mail) != null)
            return "Tên đăng nhập hoặc Email đã tồn tại!";
        return null;
    }

    public long register(String username, String mail, String pass, String ref) {
        // Tài khoản mới luôn cần xác minh qua email
        int verified = 0;
        long id = repo.insert(
                "INSERT INTO users(username,email,password,role,email_verified,created_at) VALUES(?,?,?,'user',?,NOW())",
                username, mail, bcrypt.encode(pass), verified).longValue();
        // Fix: kiểm tra null thật trước khi xử lý ref
        String clean = (ref != null && !"null".equals(ref)) ? WebUtil.cleanRef(ref) : "";
        if (!clean.isBlank()
                && repo.one("SELECT id FROM affiliates WHERE ref_code=? AND status='active'", clean) != null)
            repo.update("UPDATE users SET referred_by=? WHERE id=?", clean, id);

        String token = token();
        repo.update(
                "UPDATE users SET email_verify_token=?,email_verify_expire=DATE_ADD(NOW(),INTERVAL 24 HOUR) WHERE id=?",
                token, id);
        String link = baseUrl + "/verify_email.php?uid=" + id + "&token=" + token;
        System.out.println("==================================================");
        System.out.println("[AUTH] Đăng ký thành công user: " + username + " (Email: " + mail + ")");
        System.out.println("[AUTH] LINK XÁC MINH EMAIL: " + link);
        System.out.println("==================================================");

        if (email.configured()) {
            boolean sent = email.sendHtml(mail, "Xác minh email tài khoản GS LUXURY",
                    "<div style='font-family:Arial,sans-serif;max-width:560px;margin:0 auto;padding:24px;border:1px solid #eee;border-radius:12px;background:#fff;'>"
                            + "<h2 style='color:#bfa15f;margin-top:0;'>Chào mừng bạn đến với GS LUXURY!</h2>"
                            + "<p style='color:#333;font-size:15px;line-height:1.6;'>Cảm ơn bạn đã đăng ký tài khoản tại <b>Nội Thất GS LUXURY</b>. Vui lòng bấm vào nút bên dưới để xác minh địa chỉ email và kích hoạt tài khoản của bạn:</p>"
                            + "<div style='text-align:center;margin:32px 0;'>"
                            + "<a href='" + link + "' style='display:inline-block;padding:14px 32px;background:linear-gradient(135deg,#bfa15f,#d4b870);color:#fff;text-decoration:none;font-weight:bold;border-radius:28px;font-size:15px;box-shadow:0 4px 12px rgba(191,161,95,0.3);'>✅ Xác Minh Email Ngay</a>"
                            + "</div>"
                            + "<p style='color:#777;font-size:13px;line-height:1.5;'>Hoặc copy đường link sau vào trình duyệt:<br><a href='" + link + "' style='color:#bfa15f;word-break:break-all;'>" + link + "</a></p>"
                            + "<p style='color:#999;font-size:12px;border-top:1px solid #eee;padding-top:16px;margin-top:24px;'>Liên kết này có hiệu lực trong vòng 24 giờ. Nếu bạn không đăng ký tài khoản này, vui lòng bỏ qua email.</p>"
                            + "</div>");
            if (!sent) {
                System.err.println("[AUTH] Gửi email thất bại tới: " + mail);
            }
        } else {
            System.err.println("[AUTH] EmailService chưa được cấu hình SMTP username/password!");
        }
        return id;
    }

    public Map<String, Object> authenticate(String identity, String pass) {
        Map<String, Object> u = repo.one("SELECT * FROM users WHERE username=? OR email=? LIMIT 1", identity, identity);
        if (u == null)
            return null;
        if (WebUtil.boolv(u.get("is_locked")))
            return Map.of("error", "Tài khoản đã bị khóa.");
        String hash = String.valueOf(u.get("password"));
        boolean ok = false;
        try {
            if (hash.startsWith("$2y$"))
                hash = "$2a$" + hash.substring(4);
            ok = bcrypt.matches(pass, hash);
        } catch (Exception ignored) {
        }
        if (!ok)
            return null;
        if (!"admin".equals(String.valueOf(u.get("role"))) && !WebUtil.boolv(u.get("email_verified")))
            return Map.of("error", "Vui lòng xác minh email trước khi đăng nhập.");
        return u;
    }

    public String verify(long uid, String token) {
        Map<String, Object> u = repo.one("SELECT * FROM users WHERE id=? LIMIT 1", uid);
        if (u == null)
            return "Tài khoản không tồn tại.";
        if (WebUtil.boolv(u.get("email_verified")))
            return "Xác minh email thành công! Tài khoản của bạn đã được kích hoạt. Bạn có thể đăng nhập ngay.";
        if (token == null || token.isBlank() || !token.equals(String.valueOf(u.get("email_verify_token"))))
            return "Link xác minh không đúng hoặc đã hết hạn.";
        Map<String, Object> alive = repo.one("SELECT 1 ok WHERE ? >= NOW()", u.get("email_verify_expire"));
        if (alive == null)
            return "Link xác minh đã hết hạn (24 giờ). Vui lòng yêu cầu link mới.";
        repo.update("UPDATE users SET email_verified=1,email_verify_token=NULL,email_verify_expire=NULL WHERE id=?",
                uid);
        String ref = String.valueOf(u.getOrDefault("referred_by", ""));
        if (!ref.isBlank() && !"null".equals(ref)) {
            Map<String, Object> a = repo.one("SELECT id FROM affiliates WHERE ref_code=? AND status='active'", ref);
            if (a != null && repo.one(
                    "SELECT id FROM affiliate_commissions WHERE affiliate_id=? AND buyer_id=? AND type='referral' LIMIT 1",
                    a.get("id"), uid) == null)
                repo.update(
                        "INSERT INTO affiliate_commissions(affiliate_id,order_id,buyer_id,order_amount,commission_rate,commission_amt,type,status,note) VALUES(?,0,?,0,0,10000,'referral','confirmed','Thưởng giới thiệu thành viên xác minh email')",
                        a.get("id"), uid);
            if (a != null)
                repo.update("UPDATE affiliates SET balance=balance+10000,total_earned=total_earned+10000 WHERE id=?",
                        a.get("id"));
        }
        return "Xác minh email thành công! Bạn có thể đăng nhập ngay.";
    }

    public boolean requestReset(String mail) {
        Map<String, Object> u = repo.one("SELECT id,email FROM users WHERE email=? LIMIT 1", mail);
        if (u == null)
            return false;
        String token = token();
        repo.update("UPDATE users SET reset_token=?,reset_expire=DATE_ADD(NOW(),INTERVAL 1 HOUR) WHERE id=?", token,
                u.get("id"));
        String link = baseUrl + "/datlaimatkhau.php?token=" + token;
        email.sendHtml(mail, "Đặt lại mật khẩu GS LUXURY", "<p>Bạn đã yêu cầu đặt lại mật khẩu.</p><p><a href='" + link
                + "'>Đặt lại mật khẩu</a></p><p>Link hiệu lực 1 giờ.</p>");
        return true;
    }

    public boolean validResetToken(String token) {
        return repo.one("SELECT id FROM users WHERE reset_token=? AND reset_expire>=NOW() LIMIT 1", token) != null;
    }

    public String resetPassword(String token, String pass, String confirm) {
        if (!validResetToken(token))
            return "Token không hợp lệ hoặc đã hết hạn.";
        if (pass == null || pass.length() < 6 || pass.length() > 26 || !pass.matches(".*[A-Z].*")
                || !pass.matches(".*[0-9].*"))
            return "Mật khẩu phải 6–26 ký tự, có chữ hoa và số.";
        if (!Objects.equals(pass, confirm))
            return "Mật khẩu xác nhận không khớp.";
        repo.update("UPDATE users SET password=?,reset_token=NULL,reset_expire=NULL WHERE reset_token=?",
                bcrypt.encode(pass), token);
        return null;
    }

    public boolean matches(String raw, String hash) {
        try {
            if (hash.startsWith("$2y$"))
                hash = "$2a$" + hash.substring(4);
            return bcrypt.matches(raw, hash);
        } catch (Exception e) {
            return false;
        }
    }

    public String hash(String raw) {
        return bcrypt.encode(raw);
    }

    private String token() {
        byte[] b = new byte[32];
        new SecureRandom().nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}
