package vn.sland.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.sland.repository.AppRepository;
import vn.sland.service.AuthService;
import vn.sland.service.StorageService;
import vn.sland.util.WebUtil;
import java.util.*;

@Controller
public class ProfileController extends BaseController {

    private final AppRepository repo;
    private final AuthService auth;
    private final StorageService storage;

    public ProfileController(AppRepository repo, AuthService auth, StorageService storage) {
        this.repo    = repo;
        this.auth    = auth;
        this.storage = storage;
    }

    @GetMapping("/profile.php")
    String profile(@RequestParam(defaultValue = "info") String tab,
                   Model m, HttpSession s) {
        if (!WebUtil.logged(s)) return "redirect:/login.php";
        long uid = WebUtil.longv(s.getAttribute("user_id"));
        base(m, s, "Hồ sơ cá nhân");
        m.addAttribute("tab",       tab);
        m.addAttribute("user",      repo.one("SELECT * FROM users WHERE id=?", uid));
        m.addAttribute("addresses", repo.list(
            "SELECT * FROM user_addresses WHERE user_id=? ORDER BY is_default DESC,id DESC", uid));
        m.addAttribute("orders",    repo.list(
            "SELECT * FROM orders WHERE user_id=? ORDER BY created_at DESC LIMIT 20", uid));
        m.addAttribute("affiliate", repo.one("SELECT * FROM affiliates WHERE user_id=?", uid));
        m.addAttribute("success",   s.getAttribute("profile_success"));
        m.addAttribute("error",     s.getAttribute("profile_error"));
        s.removeAttribute("profile_success");
        s.removeAttribute("profile_error");
        return "profile";
    }

    @PostMapping("/update_profile.php")
    String update(@RequestParam String action,
                  @RequestParam Map<String, String> p,
                  @RequestParam(name = "avatar", required = false) MultipartFile avatar,
                  HttpSession s) {
        if (!WebUtil.logged(s)) return "redirect:/login.php";
        long uid = WebUtil.longv(s.getAttribute("user_id"));
        try {
            switch (action) {
                case "update_info"         -> updateInfo(uid, p, avatar);
                case "change_password"     -> changePassword(uid, p);
                case "save_address"        -> saveAddress(uid, p);
                case "delete_address"      -> repo.update(
                    "DELETE FROM user_addresses WHERE id=? AND user_id=?",
                    Integer.parseInt(p.getOrDefault("addr_id", "0")), uid);
                case "set_default_address" -> {
                    repo.update("UPDATE user_addresses SET is_default=0 WHERE user_id=?", uid);
                    repo.update("UPDATE user_addresses SET is_default=1 WHERE id=? AND user_id=?",
                        Integer.parseInt(p.getOrDefault("addr_id", "0")), uid);
                }
                default -> throw new IllegalArgumentException("Thao tác không hợp lệ.");
            }
            s.setAttribute("profile_success", "Cập nhật thành công!");
        } catch (Exception e) {
            s.setAttribute("profile_error", e.getMessage());
        }
        String tab = switch (action) {
            case "change_password"                                   -> "password";
            case "save_address","delete_address","set_default_address" -> "address";
            default                                                  -> "info";
        };
        return "redirect:/profile.php?tab=" + tab;
    }

    private void updateInfo(long uid, Map<String, String> p, MultipartFile avatar) throws Exception {
        String full     = p.getOrDefault("full_name", "").trim();
        String phone    = p.getOrDefault("phone",     "").trim();
        String birthday = p.getOrDefault("birthday",  "").trim();
        String gender   = p.getOrDefault("gender",    "").trim();
        String avatarPath = storage.saveImage(avatar, "avatars", 2 * 1024 * 1024L, "avatar" + uid);
        if (avatarPath != null)
            repo.update("UPDATE users SET full_name=?,phone=?,birthday=NULLIF(?,''),gender=NULLIF(?,''),avatar=?,updated_at=NOW() WHERE id=?",
                full, phone, birthday, gender, avatarPath, uid);
        else
            repo.update("UPDATE users SET full_name=?,phone=?,birthday=NULLIF(?,''),gender=NULLIF(?,''),updated_at=NOW() WHERE id=?",
                full, phone, birthday, gender, uid);
    }

    private void changePassword(long uid, Map<String, String> p) {
        String cur = p.getOrDefault("current_password", "");
        String n   = p.getOrDefault("new_password",     "");
        String c   = p.getOrDefault("confirm_password", "");
        var u = repo.one("SELECT password FROM users WHERE id=?", uid);
        if (u == null || !auth.matches(cur, String.valueOf(u.get("password"))))
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng.");
        if (n.length() < 6 || n.length() > 26 || !n.matches(".*[A-Z].*") || !n.matches(".*[0-9].*"))
            throw new IllegalArgumentException("Mật khẩu mới phải 6–26 ký tự, có chữ hoa và số.");
        if (!n.equals(c))
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp.");
        if (auth.matches(n, String.valueOf(u.get("password"))))
            throw new IllegalArgumentException("Mật khẩu mới không được trùng mật khẩu hiện tại.");
        repo.update("UPDATE users SET password=? WHERE id=?", auth.hash(n), uid);
    }

    private void saveAddress(long uid, Map<String, String> p) {
        int id       = Integer.parseInt(p.getOrDefault("addr_id",        "0"));
        String name  = p.getOrDefault("addr_full_name", "").trim();
        String phone = p.getOrDefault("addr_phone",     "").trim();
        String prov  = p.getOrDefault("addr_province",  "").trim();
        String dist  = p.getOrDefault("addr_district",  "").trim();
        String ward  = p.getOrDefault("addr_ward",      "").trim();
        String line  = p.getOrDefault("addr_line",      "").trim();
        if (name.isBlank() || phone.isBlank() || prov.isBlank() ||
            dist.isBlank() || ward.isBlank()  || line.isBlank())
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ thông tin địa chỉ.");
        boolean def = p.containsKey("is_default");
        if (def) repo.update("UPDATE user_addresses SET is_default=0 WHERE user_id=?", uid);
        if (id > 0)
            repo.update("UPDATE user_addresses SET full_name=?,phone=?,province=?,district=?," +
                "ward=?,address_line=?,is_default=? WHERE id=? AND user_id=?",
                name, phone, prov, dist, ward, line, def ? 1 : 0, id, uid);
        else {
            if (repo.one("SELECT id FROM user_addresses WHERE user_id=? LIMIT 1", uid) == null) def = true;
            repo.update("INSERT INTO user_addresses(user_id,full_name,phone,province,district,ward,address_line,is_default,created_at)" +
                " VALUES(?,?,?,?,?,?,?,?,NOW())", uid, name, phone, prov, dist, ward, line, def ? 1 : 0);
        }
    }
}
