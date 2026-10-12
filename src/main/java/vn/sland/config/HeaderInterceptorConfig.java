package vn.sland.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import vn.sland.repository.AppRepository;
import vn.sland.util.WebUtil;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Tương ứng phần logic đầu file header.php:
 *  1) Bắt ?ref=XXXX từ URL trên MỌI trang, lưu vào session "aff_ref" (chỉ lưu 1 lần, >= 4 ký tự).
 *  2) Với người đã đăng nhập: lấy avatar + full_name từ DB để header hiển thị
 *     (đưa vào model dưới tên headerAvatar / headerName).
 */
@Configuration
public class HeaderInterceptorConfig implements WebMvcConfigurer {

    private final AppRepository repo;

    public HeaderInterceptorConfig(AppRepository repo) {
        this.repo = repo;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {

            @Override
            public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
                String ref = req.getParameter("ref");
                if (ref != null && !ref.isBlank()) {
                    HttpSession s = req.getSession();
                    if (s.getAttribute("aff_ref") == null) {
                        String clean = WebUtil.cleanRef(ref);
                        if (clean.length() >= 4) s.setAttribute("aff_ref", clean);
                    }
                }
                return true;
            }

            @Override
            public void postHandle(HttpServletRequest req, HttpServletResponse res,
                                   Object handler, ModelAndView mv) {
                // Chỉ áp dụng cho request trả về view (bỏ qua @ResponseBody / redirect)
                if (mv == null) return;
                String view = mv.getViewName();
                if (view == null || view.startsWith("redirect:") || view.startsWith("forward:")) return;

                HttpSession s = req.getSession(false);
                if (s == null || !WebUtil.logged(s)) return;

                Map<String, Object> row = repo.one(
                        "SELECT avatar, full_name FROM users WHERE id=?",
                        WebUtil.longv(s.getAttribute("user_id")));

                String avatar = row == null || row.get("avatar") == null ? "" : String.valueOf(row.get("avatar")).trim();
                String full = row == null || row.get("full_name") == null ? "" : String.valueOf(row.get("full_name")).trim();
                String username = String.valueOf(s.getAttribute("username"));
                String name = full.isEmpty() ? username : full;

                // DB cũ lưu "picture/avatars/x.png", upload mới của Java lưu "/uploads/avatars/x.png"
                String src;
                if (!avatar.isEmpty()) {
                    src = avatar.startsWith("/") || avatar.startsWith("http") ? avatar : "/" + avatar;
                } else {
                    src = "https://ui-avatars.com/api/?name=" + URLEncoder.encode(name, StandardCharsets.UTF_8)
                            + "&background=e8352a&color=fff&size=80";
                }

                mv.addObject("headerAvatar", src);
                mv.addObject("headerName", name);
            }
        }).excludePathPatterns("/style.css", "/script.js", "/review.js",
                "/picture/**", "/uploads/**", "/error");
    }
}