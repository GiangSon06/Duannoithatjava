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
    private final AppRepository repo; private final EmailService email; private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
    private final String baseUrl; private static final Pattern USER = Pattern.compile("^[A-Za-z0-9_]{4,20}$");
    private static final Pattern MAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    public AuthService(AppRepository repo, EmailService email, @Value("${app.base-url:http://localhost:8080}") String baseUrl) { this.repo=repo; this.email=email; this.baseUrl=baseUrl; }

    public String validateRegistration(String username,String mail,String pass,String confirm) {
        if (username==null||username.isBlank()) return "Tên đăng nhập không được để trống!";
        if (!USER.matcher(username).matches()) return "Tên đăng nhập phải 4–20 ký tự và chỉ gồm chữ, số, dấu gạch dưới (_)!";
        if (mail==null||!MAIL.matcher(mail).matches()) return "Email không đúng định dạng!";
        if (pass==null||pass.length()<6) return "Mật khẩu phải có ít nhất 6 ký tự!";
        if (pass.length()>26) return "Mật khẩu không được vượt quá 26 ký tự!";
        if (!pass.matches(".*[A-Z].*")) return "Mật khẩu phải có ít nhất 1 chữ cái viết hoa!";
        if (!pass.matches(".*[0-9].*")) return "Mật khẩu phải có ít nhất 1 chữ số!";
        if (!Objects.equals(pass,confirm)) return "Mật khẩu nhập lại không khớp!";
        if (repo.one("SELECT id FROM users WHERE username=? OR email=? LIMIT 1",username,mail)!=null) return "Tên đăng nhập hoặc Email đã tồn tại!";
        return null;
    }

    public long register(String username,String mail,String pass,String ref) {
        long id = repo.insert("INSERT INTO users(username,email,password,role,email_verified,created_at) VALUES(?,?,?,'user',0,NOW())",username,mail,bcrypt.encode(pass)).longValue();
        String clean = WebUtil.cleanRef(ref);
        if (!clean.isBlank() && repo.one("SELECT id FROM affiliates WHERE ref_code=? AND status='active'",clean)!=null) repo.update("UPDATE users SET referred_by=? WHERE id=?",clean,id);
        String token = token();
        repo.update("UPDATE users SET email_verify_token=?,email_verify_expire=DATE_ADD(NOW(),INTERVAL 24 HOUR) WHERE id=?",token,id);
        String link = baseUrl + "/verify_email.php?uid=" + id + "&token=" + token;
        email.sendHtml(mail,"Xác minh email tài khoản SLAND","<h2>Chào mừng đến với SLAND!</h2><p>Nhấn liên kết để xác minh email:</p><p><a href='"+link+"'>Xác minh Email</a></p><p>Link hiệu lực 24 giờ.</p>");
        return id;
    }

    public Map<String,Object> authenticate(String identity,String pass) {
        Map<String,Object> u=repo.one("SELECT * FROM users WHERE username=? OR email=? LIMIT 1",identity,identity); if(u==null) return null;
        if(WebUtil.intv(u.get("is_locked"))==1) return Map.of("error","Tài khoản đã bị khóa.");
        String hash=String.valueOf(u.get("password")); boolean ok=false;
        try { if(hash.startsWith("$2y$")) hash="$2a$"+hash.substring(4); ok=bcrypt.matches(pass,hash); } catch(Exception ignored) {}
        if(!ok) return null;
        if(!"admin".equals(String.valueOf(u.get("role"))) && WebUtil.intv(u.get("email_verified"))==0) return Map.of("error","Vui lòng xác minh email trước khi đăng nhập.");
        return u;
    }

    public String verify(long uid,String token) {
        Map<String,Object> u=repo.one("SELECT * FROM users WHERE id=? AND email_verify_token=? LIMIT 1",uid,token); if(u==null) return "Link xác minh không đúng hoặc đã được sử dụng.";
        if(WebUtil.intv(u.get("email_verified"))==1) return "Email của bạn đã được xác minh trước đó.";
        Map<String,Object> alive=repo.one("SELECT 1 ok WHERE ? >= NOW()",u.get("email_verify_expire")); if(alive==null) return "Link xác minh đã hết hạn (24 giờ).";
        repo.update("UPDATE users SET email_verified=1,email_verify_token=NULL,email_verify_expire=NULL WHERE id=?",uid);
        String ref=String.valueOf(u.getOrDefault("referred_by",""));
        if(!ref.isBlank()&&!"null".equals(ref)) {
            Map<String,Object> a=repo.one("SELECT id FROM affiliates WHERE ref_code=? AND status='active'",ref);
            if(a!=null && repo.one("SELECT id FROM affiliate_commissions WHERE affiliate_id=? AND buyer_id=? AND type='referral' LIMIT 1",a.get("id"),uid)==null)
                repo.update("INSERT INTO affiliate_commissions(affiliate_id,order_id,buyer_id,order_amount,commission_rate,commission_amt,type,status,note) VALUES(?,0,?,0,0,10000,'referral','confirmed','Thưởng giới thiệu thành viên xác minh email')",a.get("id"),uid);
            if(a!=null) repo.update("UPDATE affiliates SET balance=balance+10000,total_earned=total_earned+10000 WHERE id=?",a.get("id"));
        }
        return "Xác minh email thành công. Bạn có thể đăng nhập.";
    }

    public boolean requestReset(String mail) {
        Map<String,Object> u=repo.one("SELECT id,email FROM users WHERE email=? LIMIT 1",mail); if(u==null) return false;
        String token=token(); repo.update("UPDATE users SET reset_token=?,reset_expire=DATE_ADD(NOW(),INTERVAL 1 HOUR) WHERE id=?",token,u.get("id"));
        String link=baseUrl+"/datlaimatkhau.php?token="+token;
        email.sendHtml(mail,"Đặt lại mật khẩu SLAND","<p>Bạn đã yêu cầu đặt lại mật khẩu.</p><p><a href='"+link+"'>Đặt lại mật khẩu</a></p><p>Link hiệu lực 1 giờ.</p>"); return true;
    }

    public boolean validResetToken(String token) { return repo.one("SELECT id FROM users WHERE reset_token=? AND reset_expire>=NOW() LIMIT 1",token)!=null; }
    public String resetPassword(String token,String pass,String confirm) {
        if(!validResetToken(token)) return "Token không hợp lệ hoặc đã hết hạn.";
        if(pass==null||pass.length()<6||pass.length()>26||!pass.matches(".*[A-Z].*")||!pass.matches(".*[0-9].*")) return "Mật khẩu phải 6–26 ký tự, có chữ hoa và số.";
        if(!Objects.equals(pass,confirm)) return "Mật khẩu xác nhận không khớp.";
        repo.update("UPDATE users SET password=?,reset_token=NULL,reset_expire=NULL WHERE reset_token=?",bcrypt.encode(pass),token); return null;
    }

    public boolean matches(String raw,String hash){ try {if(hash.startsWith("$2y$")) hash="$2a$"+hash.substring(4); return bcrypt.matches(raw,hash);}catch(Exception e){return false;} }
    public String hash(String raw){return bcrypt.encode(raw);} private String token(){byte[] b=new byte[32];new SecureRandom().nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
}
