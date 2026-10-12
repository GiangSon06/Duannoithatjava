package vn.sland.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.sland.repository.AppRepository;
import vn.sland.service.StorageService;
import vn.sland.service.OrderService;
import vn.sland.util.WebUtil;
import java.util.*;

@Controller
public class AdminController extends BaseController {
    private final AppRepository repo;
    private final StorageService storage;
    private final OrderService orders;

    public AdminController(AppRepository repo, StorageService storage, OrderService orders) {
        this.repo = repo;
        this.storage = storage;
        this.orders = orders;
    }

    @Override
    protected void base(Model m, HttpSession s, String title) {
        super.base(m, s, title);
        m.addAttribute("pendingOrdersBadge", val("SELECT COUNT(*) n FROM orders WHERE status='Chờ xử lý'"));
    }

    private String guard(HttpSession s) {
        return WebUtil.admin(s) ? null : "redirect:/login.php";
    }

    @GetMapping({"/admin.php", "/admin", "/admin/"})
    public String dashboard(Model m, HttpSession s) {
        String g = guard(s);
        if (g != null) return g;
        base(m, s, "Admin Dashboard - GS LUXURY");

        // Thống kê tổng quan
        m.addAttribute("ordersCount", val("SELECT COUNT(*) n FROM orders"));
        m.addAttribute("productsCount", val("SELECT COUNT(*) n FROM products"));
        m.addAttribute("usersCount", val("SELECT COUNT(*) n FROM users WHERE role='user'"));
        m.addAttribute("revenue", val("SELECT COALESCE(SUM(total_money),0) n FROM orders WHERE status<>'Đã hủy'"));
        m.addAttribute("pending", val("SELECT COUNT(*) n FROM orders WHERE status='Chờ xử lý'"));
        m.addAttribute("lowStock", val("SELECT COUNT(*) n FROM products WHERE quantity>0 AND quantity<=5"));
        m.addAttribute("outOfStock", val("SELECT COUNT(*) n FROM products WHERE quantity=0"));
        m.addAttribute("newContacts", val("SELECT COUNT(*) n FROM contacts WHERE status='new'"));

        // Đơn hàng gần đây
        m.addAttribute("recentOrders", repo.list("SELECT * FROM orders ORDER BY created_at DESC LIMIT 8"));

        // Sản phẩm sắp hết hàng (≤5)
        m.addAttribute("lowProducts", repo.list("SELECT * FROM products WHERE quantity<=5 ORDER BY quantity LIMIT 5"));

        // Biểu đồ trạng thái đơn hàng
        m.addAttribute("statusChart", repo.list("SELECT status,COUNT(*) total FROM orders GROUP BY status"));

        // Top 5 sản phẩm bán chạy (không tính đơn đã hủy)
        m.addAttribute("topProducts", repo.list(
                "SELECT p.name,SUM(od.quantity) sold,SUM(od.price*od.quantity) revenue " +
                        "FROM order_details od " +
                        "JOIN products p ON p.id=od.product_id " +
                        "JOIN orders o ON o.id=od.order_id " +
                        "WHERE o.status<>'Đã hủy' " +
                        "GROUP BY p.id,p.name " +
                        "ORDER BY sold DESC LIMIT 5"));

        // Biểu đồ doanh thu 7 ngày gần nhất (trừ đơn đã hủy)
        List<Map<String, Object>> weekly = repo.list(
                "SELECT DATE(created_at) as date, SUM(total_money) as revenue " +
                        "FROM orders WHERE status<>'Đã hủy' " +
                        "GROUP BY DATE(created_at) " +
                        "ORDER BY date DESC LIMIT 7");
        Collections.reverse(weekly); // oldest first
        List<String> weeklyLabels = new ArrayList<>();
        List<Number> weeklyData = new ArrayList<>();
        for (Map<String, Object> row : weekly) {
            // date có dạng yyyy-MM-dd (java.sql.Date hoặc LocalDate đều toString ra dạng này) -> dd/MM
            String d = String.valueOf(row.get("date"));
            weeklyLabels.add(d.length() >= 10 ? d.substring(8, 10) + "/" + d.substring(5, 7) : d);
            weeklyData.add((Number) row.get("revenue"));
        }
        m.addAttribute("weeklyLabels", weeklyLabels);
        m.addAttribute("weeklyData", weeklyData);

        return "admin";
    }

    @GetMapping({"/admin_products.php", "/admin_product.php"})
    public String products(Model m, HttpSession s) {
        String g = guard(s);
        if (g != null) return g;
        base(m, s, "Quản lý sản phẩm");

        // Thống kê sản phẩm
        m.addAttribute("totalProducts", val("SELECT COUNT(*) n FROM products"));
        m.addAttribute("lowStock", val("SELECT COUNT(*) n FROM products WHERE quantity>0 AND quantity<=5"));
        m.addAttribute("outOfStock", val("SELECT COUNT(*) n FROM products WHERE quantity=0"));

        m.addAttribute("products", repo.list(
                "SELECT p.*,c.name category_name " +
                        "FROM products p LEFT JOIN categories c ON c.id=p.category_id " +
                        "ORDER BY p.id DESC"));
        return "admin_products";
    }

    @GetMapping("/admin_add_product.php")
    public String add(Model m, HttpSession s) {
        String g = guard(s);
        if (g != null) return g;
        base(m, s, "Thêm sản phẩm");
        m.addAttribute("categories", repo.list("SELECT * FROM categories ORDER BY name"));
        return "admin_product_form";
    }

    @PostMapping("/admin_add_product.php")
    public String addPost(@RequestParam String name,
                          @RequestParam double price,
                          @RequestParam(name = "old_price", required = false) Double old,
                          @RequestParam(required = false, defaultValue = "") String description,
                          @RequestParam(name = "category_id") int category,
                          @RequestParam(defaultValue = "0") int quantity,
                          @RequestParam(required = false, defaultValue = "") String material,
                          @RequestParam(required = false, defaultValue = "") String dimensions,
                          @RequestParam(required = false, defaultValue = "") String warranty,
                          @RequestParam(required = false) MultipartFile image,
                          HttpSession s,
                          Model model) {
        String g = guard(s);
        if (g != null) return g;

        // Validation
        List<String> errors = new ArrayList<>();
        if (name == null || name.trim().isEmpty()) {
            errors.add("Tên sản phẩm không được để trống!");
        }
        if (price <= 0) {
            errors.add("Giá bán phải lớn hơn 0!");
        }
        if (old != null && old <= 0) {
            errors.add("Giá gốc phải lớn hơn 0!");
        }
        if (old != null && old <= price) {
            errors.add("Giá gốc phải lớn hơn giá bán để hiển thị giảm giá!");
        }
        if (quantity < 0) {
            errors.add("Số lượng tồn kho không được âm!");
        }
        // Image validation
        if (image == null || image.isEmpty()) {
            errors.add("Vui lòng chọn ảnh sản phẩm!");
        } else {
            String type = Optional.ofNullable(image.getContentType()).orElse("").toLowerCase(Locale.ROOT);
            if (!Set.of("image/jpeg", "image/png", "image/webp", "image/gif").contains(type)) {
                errors.add("Chỉ chấp nhận ảnh JPG, PNG, WEBP, GIF!");
            } else if (image.getSize() > 5 * 1024 * 1024L) {
                errors.add("Kích thước ảnh không vượt quá 5MB!");
            }
        }

        if (!errors.isEmpty()) {
            base(model, s, "Thêm sản phẩm");
            model.addAttribute("errors", errors);
            model.addAttribute("oldName", name);
            model.addAttribute("oldPrice", price);
            model.addAttribute("oldOldPrice", old);
            model.addAttribute("oldDescription", description);
            model.addAttribute("oldCategoryId", category);
            model.addAttribute("oldQuantity", quantity);
            model.addAttribute("oldMaterial", material);
            model.addAttribute("oldDimensions", dimensions);
            model.addAttribute("oldWarranty", warranty);
            model.addAttribute("oldImage", image != null ? image.getOriginalFilename() : null);
            model.addAttribute("categories", repo.list("SELECT * FROM categories ORDER BY name"));
            return "admin_product_form";
        }

        // Process upload
        String path = null;
        try {
            path = storage.saveImage(image, "products", 5 * 1024 * 1024L, "sp");
        } catch (Exception e) {
            errors.add("Lỗi lưu ảnh: " + e.getMessage());
            base(model, s, "Thêm sản phẩm");
            model.addAttribute("errors", errors);
            model.addAttribute("categories", repo.list("SELECT * FROM categories ORDER BY name"));
            return "admin_product_form";
        }

        repo.update(
                "INSERT INTO products(name,price,old_price,description,category_id,image_url,material,dimensions,warranty,quantity) " +
                        "VALUES(?,?,?,?,?,?,?,?,?,?)",
                name.trim(), price, old, description, category, path, material, dimensions, warranty, quantity);
        return "redirect:/admin_products.php";
    }

    @GetMapping("/admin_edit_product.php")
    public String edit(@RequestParam int id, Model m, HttpSession s) {
        String g = guard(s);
        if (g != null) return g;
        base(m, s, "Sửa sản phẩm");
        Map<String, Object> product = repo.one("SELECT * FROM products WHERE id=?", id);
        if (product == null) {
            return "redirect:/admin_products.php";
        }
        m.addAttribute("product", product);
        m.addAttribute("categories", repo.list("SELECT * FROM categories ORDER BY name"));
        return "admin_product_form";
    }

    @PostMapping("/admin_edit_product.php")
    public String editPost(@RequestParam int id,
                           @RequestParam String name,
                           @RequestParam double price,
                           @RequestParam(name = "old_price", required = false) Double old,
                           @RequestParam(required = false, defaultValue = "") String description,
                           @RequestParam(name = "category_id") int category,
                           @RequestParam(defaultValue = "0") int quantity,
                           @RequestParam(required = false, defaultValue = "") String material,
                           @RequestParam(required = false, defaultValue = "") String dimensions,
                           @RequestParam(required = false, defaultValue = "") String warranty,
                           @RequestParam(required = false) MultipartFile image,
                           HttpSession s,
                           Model model) {
        String g = guard(s);
        if (g != null) return g;

        // Validation (similar to add)
        List<String> errors = new ArrayList<>();
        if (name == null || name.trim().isEmpty()) {
            errors.add("Tên sản phẩm không được để trống!");
        }
        if (price <= 0) {
            errors.add("Giá bán phải lớn hơn 0!");
        }
        if (old != null && old <= 0) {
            errors.add("Giá gốc phải lớn hơn 0!");
        }
        if (old != null && old <= price) {
            errors.add("Giá gốc phải lớn hơn giá bán để hiển thị giảm giá!");
        }
        if (quantity < 0) {
            errors.add("Số lượng tồn kho không được âm!");
        }
        // Image validation (only if new image provided)
        if (image != null && !image.isEmpty()) {
            String type = Optional.ofNullable(image.getContentType()).orElse("").toLowerCase(Locale.ROOT);
            if (!Set.of("image/jpeg", "image/png", "image/webp", "image/gif").contains(type)) {
                errors.add("Chỉ chấp nhận ảnh JPG, PNG, WEBP, GIF!");
            } else if (image.getSize() > 5 * 1024 * 1024L) {
                errors.add("Kích thước ảnh không vượt quá 5MB!");
            }
        }

        if (!errors.isEmpty()) {
            base(model, s, "Sửa sản phẩm");
            model.addAttribute("errors", errors);
            Map<String, Object> product = repo.one("SELECT * FROM products WHERE id=?", id);
            model.addAttribute("product", product);
            model.addAttribute("categories", repo.list("SELECT * FROM categories ORDER BY name"));
            // Keep old values for repopulation
            model.addAttribute("oldName", name);
            model.addAttribute("oldPrice", price);
            model.addAttribute("oldOldPrice", old);
            model.addAttribute("oldDescription", description);
            model.addAttribute("oldCategoryId", category);
            model.addAttribute("oldQuantity", quantity);
            model.addAttribute("oldMaterial", material);
            model.addAttribute("oldDimensions", dimensions);
            model.addAttribute("oldWarranty", warranty);
            model.addAttribute("oldImage", image != null ? image.getOriginalFilename() : null);
            return "admin_product_form";
        }

        String path = null;
        boolean hasNewImage = image != null && !image.isEmpty();
        if (hasNewImage) {
            try {
                path = storage.saveImage(image, "products", 5 * 1024 * 1024L, "sp");
            } catch (Exception e) {
                errors.add("Lỗi lưu ảnh: " + e.getMessage());
                base(model, s, "Sửa sản phẩm");
                model.addAttribute("errors", errors);
                Map<String, Object> product = repo.one("SELECT * FROM products WHERE id=?", id);
                model.addAttribute("product", product);
                model.addAttribute("categories", repo.list("SELECT * FROM categories ORDER BY name"));
                return "admin_product_form";
            }
        }

        if (hasNewImage) {
            repo.update(
                    "UPDATE products SET name=?,price=?,old_price=?,description=?,category_id=?,image_url=?,material=?,dimensions=?,warranty=?,quantity=? WHERE id=?",
                    name.trim(), price, old, description, category, path, material, dimensions, warranty, quantity, id);
        } else {
            repo.update(
                    "UPDATE products SET name=?,price=?,old_price=?,description=?,category_id=?,material=?,dimensions=?,warranty=?,quantity=? WHERE id=?",
                    name.trim(), price, old, description, category, material, dimensions, warranty, quantity, id);
        }
        return "redirect:/admin_products.php";
    }

    @GetMapping("/admin_delete_product.php")
    public String delete(@RequestParam int id, HttpSession s) {
        String g = guard(s);
        if (g != null) return g;
        repo.update("DELETE FROM products WHERE id=?", id);
        return "redirect:/admin_products.php?deleted=1";
    }

    @GetMapping({"/admin_users.php", "/admin_user.php"})
    public String users(Model m, HttpSession s,
                        @RequestParam(required = false) String q,
                        @RequestParam(name = "toggle_lock", required = false) Long toggleLockId) {
        String g = guard(s);
        if (g != null) return g;
        if (toggleLockId != null) {
            repo.update("UPDATE users SET is_locked = IF(COALESCE(is_locked,0)=1, 0, 1) WHERE id=? AND role<>'admin'", toggleLockId);
            return "redirect:/admin_users.php?updated=1";
        }
        base(m, s, "Quản lý người dùng");

        // Thống kê người dùng
        long totalUsers = ((Number) val("SELECT COUNT(*) n FROM users")).longValue();
        long newToday = ((Number) val("SELECT COUNT(*) n FROM users WHERE DATE(created_at)=CURDATE()")).longValue();
        long lockedCount = ((Number) val("SELECT COUNT(*) n FROM users WHERE is_locked=1")).longValue();

        m.addAttribute("totalUsers", totalUsers);
        m.addAttribute("newToday", newToday);
        m.addAttribute("lockedCount", lockedCount);

        // Danh sách người dùng với thông tin bổ sung (bao gồm cả tài khoản admin)
        String sql = "SELECT u.*, (CASE WHEN u.is_locked = 1 THEN 1 ELSE 0 END) AS is_locked, u.full_name AS fullname, " +
                "(SELECT COUNT(*) FROM orders WHERE user_id=u.id) AS order_count, " +
                "(SELECT COALESCE(SUM(total_money),0) FROM orders WHERE user_id=u.id AND status<>'Đã hủy') AS total_spent, " +
                "(SELECT MAX(created_at) FROM orders WHERE user_id=u.id) AS last_order " +
                "FROM users u WHERE 1=1";
        if (q != null && !q.trim().isEmpty()) {
            sql += " AND (username LIKE ? OR email LIKE ?)";
        }
        sql += " ORDER BY u.created_at DESC";
        List<Map<String, Object>> users;
        if (q != null && !q.trim().isEmpty()) {
            String like = "%" + q.trim() + "%";
            users = repo.list(sql, like, like);
        } else {
            users = repo.list(sql);
        }
        m.addAttribute("users", users);
        return "admin_users";
    }

    @PostMapping({"/admin_users.php", "/admin_user.php"})
    public String usersPost(@RequestParam String action,
                            @RequestParam int id,
                            HttpSession s) {
        String g = guard(s);
        if (g != null) return g;
        if ("toggle_lock".equals(action)) {
            repo.update("UPDATE users SET is_locked = IF(COALESCE(is_locked,0)=1, 0, 1) WHERE id=? AND role<>'admin'", id);
            return "redirect:/admin_users.php?updated=1";
        }
        return "redirect:/admin_users.php";
    }

    @GetMapping("/admin_vouchers.php")
    public String vouchers(@RequestParam(name = "delete", required = false) Long deleteId,
                           @RequestParam(name = "toggle", required = false) Long toggleId,
                           Model m, HttpSession s) {
        String g = guard(s);
        if (g != null) return g;
        if (deleteId != null) {
            repo.update("DELETE FROM vouchers WHERE id=?", deleteId);
            return "redirect:/admin_vouchers.php?deleted=1";
        }
        if (toggleId != null) {
            repo.update("UPDATE vouchers SET is_active = !is_active WHERE id=?", toggleId);
            return "redirect:/admin_vouchers.php";
        }
        base(m, s, "Voucher");

        // Sắp xếp theo created_at DESC (mới nhất trước)
        m.addAttribute("vouchers", repo.list("SELECT * FROM vouchers ORDER BY created_at DESC"));
        return "admin_vouchers";
    }

    @PostMapping("/admin_vouchers.php")
    public String vouchersPost(@RequestParam String action,
                               @RequestParam Map<String, String> p,
                               HttpSession s,
                               Model model) {
        String g = guard(s);
        if (g != null) return g;
        if ("delete".equals(action)) {
            repo.update("DELETE FROM vouchers WHERE id=?", Integer.parseInt(p.get("id")));
            return "redirect:/admin_vouchers.php?deleted=1";
        } else if ("toggle".equals(action)) {
            // Toggle is_active
            int vid = Integer.parseInt(p.get("id"));
            repo.update("UPDATE vouchers SET is_active = !is_active WHERE id=?", vid);
            return "redirect:/admin_vouchers.php";
        } else {
            // Thêm voucher mới với validation
            String code = p.getOrDefault("code", "").toUpperCase(Locale.ROOT);
            String type = p.getOrDefault("discount_type", "percent");
            String valueStr = p.getOrDefault("discount_value", "0");
            String minStr = p.getOrDefault("min_order", "0");
            String maxStr = p.getOrDefault("max_uses", "100");
            String expires = p.getOrDefault("expires_at", "");

            List<String> errors = new ArrayList<>();
            if (code == null || code.isEmpty()) {
                errors.add("Mã voucher không được để trống!");
            }
            if (!type.equals("percent") && !type.equals("fixed")) {
                errors.add("Loại giảm giá không hợp lệ!");
            }
            double value = 0;
            try {
                value = Double.parseDouble(valueStr);
                if (value <= 0) {
                    errors.add("Giá trị giảm phải lớn hơn 0!");
                }
            } catch (NumberFormatException e) {
                errors.add("Giá trị giảm phải là số!");
            }
            double min = 0;
            try {
                min = Double.parseDouble(minStr);
                if (min < 0) {
                    errors.add("Đơn hàng tối thiểu không được âm!");
                }
            } catch (NumberFormatException e) {
                errors.add("Đơn hàng tối thiểu phải là số!");
            }
            int max = 0;
            try {
                max = Integer.parseInt(maxStr);
                if (max < 1) {
                    errors.add("Số lần dùng tối đa phải ít nhất 1!");
                }
            } catch (NumberFormatException e) {
                errors.add("Số lần dùng tối đa phải là số nguyên!");
            }

            if (!errors.isEmpty()) {
                base(model, s, "Voucher");
                model.addAttribute("errors", errors);
                // Giữ lại giá trị cũ để hiển thị lại trong form
                model.addAttribute("oldCode", code);
                model.addAttribute("oldType", type);
                model.addAttribute("oldValue", valueStr);
                model.addAttribute("oldMin", minStr);
                model.addAttribute("oldMax", maxStr);
                model.addAttribute("oldExpires", expires);
                model.addAttribute("vouchers", repo.list("SELECT * FROM vouchers ORDER BY created_at DESC"));
                return "admin_vouchers";
            }

            // Thêm voucher mới
            repo.update(
                    "INSERT INTO vouchers(code,discount_type,discount_value,min_order,max_uses,expires_at,is_active,used_count,created_at) " +
                            "VALUES(?,?,?,?,?,NULLIF(?,''),1,0,NOW())",
                    code, type, value, min, max, expires);
            return "redirect:/admin_vouchers.php?added=1";
        }
    }

    @GetMapping("/admin_reviews.php")
    public String reviews(Model m, HttpSession s) {
        String g = guard(s);
        if (g != null) return g;
        base(m, s, "Đánh giá");
        m.addAttribute("reviews", repo.list(
                "SELECT r.*,u.username,p.name product_name " +
                        "FROM reviews r " +
                        "JOIN users u ON u.id=r.user_id " +
                        "LEFT JOIN products p ON p.id=r.product_id " +
                        "ORDER BY r.created_at DESC"));
        return "admin_reviews";
    }

    @PostMapping("/admin_reviews.php")
    public String reviewsPost(@RequestParam String action,
                              @RequestParam int id,
                              HttpSession s) {
        String g = guard(s);
        if (g != null) return g;
        if ("delete".equals(action)) {
            repo.update("DELETE FROM reviews WHERE id=?", id);
        }
        return "redirect:/admin_reviews.php";
    }

    @GetMapping("/admin_contacts.php")
    public String contacts(Model m, HttpSession s) {
        String g = guard(s);
        if (g != null) return g;
        base(m, s, "Liên hệ");
        m.addAttribute("contacts", repo.list("SELECT * FROM contacts ORDER BY created_at DESC"));
        return "admin_contacts";
    }

    @PostMapping("/admin_contacts.php")
    public String contactsPost(@RequestParam int id,
                               @RequestParam(defaultValue = "read") String status,
                               HttpSession s) {
        String g = guard(s);
        if (g != null) return g;
        repo.update("UPDATE contacts SET status=? WHERE id=?", status, id);
        return "redirect:/admin_contacts.php";
    }

    @GetMapping("/admin_stats.php")
    public String stats(Model m, HttpSession s,
                        @RequestParam(required = false) Integer month,
                        @RequestParam(required = false) Integer year) {
        String g = guard(s);
        if (g != null) return g;
        base(m, s, "Thống kê nâng cao");
        java.time.LocalDate now = java.time.LocalDate.now();
        int mo = (month != null && month >= 1 && month <= 12) ? month : now.getMonthValue();
        int yr = (year != null && year >= 2023) ? year : now.getYear();
        m.addAttribute("month", mo);
        m.addAttribute("year", yr);

        // Years dropdown
        List<Integer> years = new ArrayList<>();
        for (int y = now.getYear(); y >= 2023; y--) years.add(y);
        m.addAttribute("years", years);

        // Stat this month
        var revRow = repo.one("SELECT COALESCE(SUM(total_money),0) n FROM orders WHERE MONTH(created_at)=? AND YEAR(created_at)=? AND status<>'Đã hủy'", mo, yr);
        var ordRow = repo.one("SELECT COUNT(*) n FROM orders WHERE MONTH(created_at)=? AND YEAR(created_at)=? AND status<>'Đã hủy'", mo, yr);
        var canRow = repo.one("SELECT COUNT(*) n FROM orders WHERE MONTH(created_at)=? AND YEAR(created_at)=?", mo, yr);
        var usrRow = repo.one("SELECT COUNT(*) n FROM users WHERE MONTH(created_at)=? AND YEAR(created_at)=?", mo, yr);
        var canOnlyRow = repo.one("SELECT COUNT(*) n FROM orders WHERE MONTH(created_at)=? AND YEAR(created_at)=? AND status='Đã hủy'", mo, yr);
        long revThis = revRow == null ? 0 : ((Number) revRow.get("n")).longValue();
        int ordThis = ordRow == null ? 0 : ((Number) ordRow.get("n")).intValue();
        int canAll = canRow == null ? 0 : ((Number) canRow.get("n")).intValue();
        int newUsr = usrRow == null ? 0 : ((Number) usrRow.get("n")).intValue();
        int canOnly = canOnlyRow == null ? 0 : ((Number) canOnlyRow.get("n")).intValue();
        double cancelRate = canAll > 0 ? Math.round(canOnly * 1000.0 / canAll) / 10.0 : 0;

        // Prev month stat for % change
        int pmo = mo == 1 ? 12 : mo - 1;
        int pyr = mo == 1 ? yr - 1 : yr;
        var prevRevRow = repo.one("SELECT COALESCE(SUM(total_money),0) n FROM orders WHERE MONTH(created_at)=? AND YEAR(created_at)=? AND status<>'Đã hủy'", pmo, pyr);
        var prevOrdRow = repo.one("SELECT COUNT(*) n FROM orders WHERE MONTH(created_at)=? AND YEAR(created_at)=? AND status<>'Đã hủy'", pmo, pyr);
        long prevRev = prevRevRow == null ? 0 : ((Number) prevRevRow.get("n")).longValue();
        int prevOrd = prevOrdRow == null ? 0 : ((Number) prevOrdRow.get("n")).intValue();
        Double revChange = prevRev > 0 ? Math.round((revThis - prevRev) * 1000.0 / prevRev) / 10.0 : null;
        Double ordChange = prevOrd > 0 ? Math.round((ordThis - prevOrd) * 1000.0 / prevOrd) / 10.0 : null;

        m.addAttribute("revThisMonth", revThis);
        m.addAttribute("ordThisMonth", ordThis);
        m.addAttribute("cancelRate", cancelRate);
        m.addAttribute("newUsers", newUsr);
        m.addAttribute("revChange", revChange);
        m.addAttribute("ordChange", ordChange);

        // Daily chart: labels + values
        int daysInMonth = java.time.YearMonth.of(yr, mo).lengthOfMonth();
        List<String> dailyLabels = new ArrayList<>();
        List<Long> dailyValues = new ArrayList<>();
        Map<String, Long> dayMap = new java.util.LinkedHashMap<>();
        for (int d = 1; d <= daysInMonth; d++) dayMap.put(String.valueOf(d), 0L);
        for (var row : repo.list("SELECT DAY(created_at) d, COALESCE(SUM(total_money),0) rev FROM orders WHERE MONTH(created_at)=? AND YEAR(created_at)=? AND status<>'Đã hủy' GROUP BY DAY(created_at)", mo, yr)) {
            int day = ((Number) row.get("d")).intValue();
            String key = String.valueOf(day);
            dayMap.put(key, ((Number) row.get("rev")).longValue());
        }
        dayMap.forEach((k, v) -> {
            dailyLabels.add(k);
            dailyValues.add(v);
        });
        m.addAttribute("dailyLabels", dailyLabels);
        m.addAttribute("dailyValues", dailyValues);

        // Yearly chart: 12 months
        List<Long> yearlyValues = new ArrayList<>(java.util.Collections.nCopies(12, 0L));
        for (var row : repo.list("SELECT MONTH(created_at) m, COALESCE(SUM(total_money),0) rev FROM orders WHERE YEAR(created_at)=? AND status<>'Đã hủy' GROUP BY MONTH(created_at)", yr)) {
            int idx = ((Number) row.get("m")).intValue() - 1;
            yearlyValues.set(idx, ((Number) row.get("rev")).longValue());
        }
        m.addAttribute("yearlyValues", yearlyValues);

        // Payment method chart
        List<String> pmLabels = new ArrayList<>();
        List<Integer> pmValues = new ArrayList<>();
        List<String> pmColors = new ArrayList<>();
        Map<String, String[]> pmMap = Map.of(
                "cod", new String[]{"💵 COD", "#f39c12"},
                "banking", new String[]{"🏦 Banking", "#3498db"},
                "momo", new String[]{"💜 MoMo", "#a50064"}
        );
        for (var row : repo.list("SELECT payment_method, COUNT(*) cnt FROM orders WHERE MONTH(created_at)=? AND YEAR(created_at)=? GROUP BY payment_method", mo, yr)) {
            String pm = String.valueOf(row.get("payment_method"));
            String[] info = pmMap.getOrDefault(pm, new String[]{pm, "#95a5a6"});
            pmLabels.add(info[0]);
            pmValues.add(((Number) row.get("cnt")).intValue());
            pmColors.add(info[1]);
        }
        m.addAttribute("pmLabels", pmLabels);
        m.addAttribute("pmValues", pmValues);
        m.addAttribute("pmColors", pmColors);

        // Top products this month (limit 5)
        var topProds = repo.list(
                "SELECT p.name product_name, SUM(od.quantity) qty, SUM(od.price*od.quantity) revenue " +
                        "FROM order_details od " +
                        "JOIN products p ON p.id=od.product_id " +
                        "JOIN orders o ON o.id=od.order_id " +
                        "WHERE MONTH(o.created_at)=? AND YEAR(o.created_at)=? AND o.status<>'Đã hủy' " +
                        "GROUP BY p.id,p.name " +
                        "ORDER BY qty DESC LIMIT 5", mo, yr);
        long topMax = topProds.isEmpty() ? 1 : ((Number) topProds.get(0).get("qty")).longValue();
        m.addAttribute("topProducts", topProds);
        m.addAttribute("topMaxQty", topMax);

        return "admin_stats";
    }

    @GetMapping({"/admin_orders.php", "/admin_orders"})
    public String adminOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String payment,
            @RequestParam(name = "from_date", required = false) String fromDate,
            @RequestParam(name = "to_date", required = false) String toDate,
            @RequestParam(defaultValue = "newest") String sort,
            Model m, HttpSession s) {
        String g = guard(s);
        if (g != null) return g;
        base(m, s, "Quản lý đơn hàng — GS LUXURY Admin");

        // Stat counts for overview & filter tabs
        long totalOrdersAll = WebUtil.longv(val("SELECT COUNT(*) n FROM orders"));
        long pendingOrders = WebUtil.longv(val("SELECT COUNT(*) n FROM orders WHERE status='Chờ xử lý'"));
        long preparingOrders = WebUtil.longv(val("SELECT COUNT(*) n FROM orders WHERE status='Đang chuẩn bị'"));
        long deliveringOrders = WebUtil.longv(val("SELECT COUNT(*) n FROM orders WHERE status='Đang giao' OR status='Đang giao hàng'"));
        long completedOrders = WebUtil.longv(val("SELECT COUNT(*) n FROM orders WHERE status='Đã giao hàng' OR status='Đã giao'"));
        long canceledOrders = WebUtil.longv(val("SELECT COUNT(*) n FROM orders WHERE status='Đã hủy'"));
        long totalRevenue = WebUtil.longv(val("SELECT COALESCE(SUM(total_money), 0) n FROM orders WHERE status<>'Đã hủy'"));

        m.addAttribute("totalOrdersAll", totalOrdersAll);
        m.addAttribute("pendingOrders", pendingOrders);
        m.addAttribute("preparingOrders", preparingOrders);
        m.addAttribute("deliveringOrders", deliveringOrders);
        m.addAttribute("completedOrders", completedOrders);
        m.addAttribute("canceledOrders", canceledOrders);
        m.addAttribute("totalRevenue", totalRevenue);

        // Build WHERE clause
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            if ("Đang giao".equals(status)) {
                where.append(" AND (o.status='Đang giao' OR o.status='Đang giao hàng') ");
            } else if ("Đã giao hàng".equals(status)) {
                where.append(" AND (o.status='Đã giao hàng' OR o.status='Đã giao') ");
            } else {
                where.append(" AND o.status=? ");
                params.add(status.trim());
            }
        }

        if (payment != null && !payment.isBlank() && !"all".equalsIgnoreCase(payment)) {
            where.append(" AND o.payment_method=? ");
            params.add(payment.trim());
        }

        if (fromDate != null && !fromDate.isBlank()) {
            where.append(" AND DATE(o.created_at) >= ? ");
            params.add(fromDate.trim());
        }

        if (toDate != null && !toDate.isBlank()) {
            where.append(" AND DATE(o.created_at) <= ? ");
            params.add(toDate.trim());
        }

        if (q != null && !q.isBlank()) {
            String cleanQ = q.trim();
            String numericId = cleanQ.replaceAll("[^0-9]", "");
            if (!numericId.isEmpty() && cleanQ.matches("^#?\\d+$")) {
                where.append(" AND (o.id = ? OR o.phone LIKE ? OR o.fullname LIKE ?) ");
                params.add(Long.parseLong(numericId));
                params.add("%" + cleanQ + "%");
                params.add("%" + cleanQ + "%");
            } else {
                where.append(" AND (o.fullname LIKE ? OR o.phone LIKE ? OR o.address LIKE ? OR o.voucher_code LIKE ? OR u.email LIKE ?) ");
                params.add("%" + cleanQ + "%");
                params.add("%" + cleanQ + "%");
                params.add("%" + cleanQ + "%");
                params.add("%" + cleanQ + "%");
                params.add("%" + cleanQ + "%");
            }
        }

        // Count total matching
        String countSql = "SELECT COUNT(*) n FROM orders o LEFT JOIN users u ON u.id=o.user_id " + where;
        var countMap = repo.one(countSql, params.toArray());
        long totalRecords = countMap == null ? 0 : WebUtil.longv(countMap.get("n"));

        // Pagination calculations
        int safePage = Math.max(1, page);
        int safeSize = (size <= 0 || size > 100) ? 10 : size;
        int totalPages = (int) Math.ceil((double) totalRecords / safeSize);
        if (totalPages <= 0) totalPages = 1;
        if (safePage > totalPages) safePage = totalPages;
        int offset = (safePage - 1) * safeSize;

        // Sorting
        String orderBy = switch (sort) {
            case "oldest" -> " ORDER BY o.created_at ASC ";
            case "total_desc" -> " ORDER BY o.total_money DESC ";
            case "total_asc" -> " ORDER BY o.total_money ASC ";
            default -> " ORDER BY o.created_at DESC ";
        };

        // Query paginated list
        List<Object> listParams = new ArrayList<>(params);
        listParams.add(safeSize);
        listParams.add(offset);
        String listSql = "SELECT o.*, u.email as user_email, u.username as account_name " +
                "FROM orders o " +
                "LEFT JOIN users u ON u.id=o.user_id " +
                where + orderBy + " LIMIT ? OFFSET ?";
        List<Map<String, Object>> ordersList = repo.list(listSql, listParams.toArray());

        // Batch load order items and product images
        if (!ordersList.isEmpty()) {
            List<Object> orderIds = ordersList.stream().map(order -> order.get("id")).toList();
            String inPlaceholders = String.join(",", Collections.nCopies(orderIds.size(), "?"));
            String detailsSql = "SELECT od.*, p.image_url FROM order_details od " +
                    "LEFT JOIN products p ON od.product_id = p.id " +
                    "WHERE od.order_id IN (" + inPlaceholders + ") ORDER BY od.id ASC";
            List<Map<String, Object>> allDetails = repo.list(detailsSql, orderIds.toArray());

            Map<Long, List<Map<String, Object>>> detailsByOrderId = new LinkedHashMap<>();
            for (Map<String, Object> d : allDetails) {
                long oid = WebUtil.longv(d.get("order_id"));
                detailsByOrderId.computeIfAbsent(oid, k -> new ArrayList<>()).add(d);
            }

            for (Map<String, Object> order : ordersList) {
                long oid = WebUtil.longv(order.get("id"));
                List<Map<String, Object>> items = detailsByOrderId.getOrDefault(oid, List.of());
                order.put("items", items);
                int totalQuantity = items.stream().mapToInt(it -> WebUtil.intv(it.get("quantity"))).sum();
                order.put("total_items_count", items.size());
                order.put("total_quantity", totalQuantity);
            }
        }

        m.addAttribute("orders", ordersList);
        m.addAttribute("currentPage", safePage);
        m.addAttribute("pageSize", safeSize);
        m.addAttribute("totalPages", totalPages);
        m.addAttribute("totalRecords", totalRecords);
        m.addAttribute("startRecord", totalRecords == 0 ? 0 : offset + 1);
        m.addAttribute("endRecord", Math.min(offset + safeSize, totalRecords));

        // Generate pagination numbers
        List<Integer> pageNumbers = new ArrayList<>();
        int startP = Math.max(1, safePage - 2);
        int endP = Math.min(totalPages, safePage + 2);
        for (int i = startP; i <= endP; i++) {
            pageNumbers.add(i);
        }
        m.addAttribute("pageNumbers", pageNumbers);

        // Retain filters
        m.addAttribute("paramStatus", status != null ? status : "");
        m.addAttribute("paramQ", q != null ? q : "");
        m.addAttribute("paramPayment", payment != null ? payment : "");
        m.addAttribute("paramFromDate", fromDate != null ? fromDate : "");
        m.addAttribute("paramToDate", toDate != null ? toDate : "");
        m.addAttribute("paramSort", sort);

        return "admin_orders";
    }

    @PostMapping("/update_order_status.php")
    public String status(@RequestParam(name = "order_id") long id,
                         @RequestParam String status,
                         @RequestParam(required = false, defaultValue = "") String note,
                         @RequestParam(name = "estimated_delivery", required = false, defaultValue = "") String delivery,
                         @RequestParam(name = "delivery_note", required = false, defaultValue = "") String dnote,
                         @RequestParam(name = "redirect_to", required = false) String redirectTo,
                         HttpSession s) {
        String g = guard(s);
        if (g != null) return g;
        var curOrder = repo.one("SELECT status FROM orders WHERE id=?", id);
        if (curOrder != null) {
            String cur = String.valueOf(curOrder.get("status"));
            if (("Đang giao".equals(cur) || "Đã giao hàng".equals(cur)) && "Chờ xử lý".equals(status)) {
                if (redirectTo != null && !redirectTo.isBlank()) {
                    return "redirect:" + redirectTo + (redirectTo.contains("?") ? "&" : "?") + "error=cannot_revert";
                }
                return "redirect:/admin.php#orders-section";
            }
        }
        repo.update("UPDATE orders SET status=?,estimated_delivery=COALESCE(NULLIF(?,''),estimated_delivery),delivery_note=COALESCE(NULLIF(?,''),delivery_note) WHERE id=?", status, delivery, dnote, id);
        repo.update("INSERT INTO order_status_history(order_id,status,note,created_at) VALUES(?,?,?,NOW())", id, status, note.isBlank() ? "Admin cập nhật trạng thái: " + status : note);
        // Trạng thái trong DB là "Đã giao hàng" (trước đây so sánh nhầm với "Đã giao")
        if ("Đã giao hàng".equals(status)) {
            for (var c : repo.list("SELECT id,affiliate_id,commission_amt FROM affiliate_commissions WHERE order_id=? AND status='pending'", id)) {
                repo.update("UPDATE affiliate_commissions SET status='confirmed' WHERE id=?", c.get("id"));
                repo.update("UPDATE affiliates SET balance=balance+?,total_earned=total_earned+? WHERE id=?", c.get("commission_amt"), c.get("commission_amt"), c.get("affiliate_id"));
            }
        }
        if (redirectTo != null && !redirectTo.isBlank()) {
            return "redirect:" + redirectTo + (redirectTo.contains("?") ? "&" : "?") + "updated=true";
        }
        return "redirect:/admin.php#orders-section";
    }

    @PostMapping("/admin_cancel_order.php")
    public String adminCancelOrder(@RequestParam(name = "order_id") long id,
                                   @RequestParam(name = "redirect_to", required = false) String redirectTo,
                                   HttpSession s) {
        String g = guard(s);
        if (g != null) return g;
        String err = orders.cancel(id, 0, true);
        if (redirectTo != null && !redirectTo.isBlank()) {
            if (err != null) {
                return "redirect:" + redirectTo + (redirectTo.contains("?") ? "&" : "?") + "error=" + java.net.URLEncoder.encode(err, java.nio.charset.StandardCharsets.UTF_8);
            }
            return "redirect:" + redirectTo + (redirectTo.contains("?") ? "&" : "?") + "cancelled=true";
        }
        return "redirect:/admin_orders.php?cancelled=true";
    }

    private Object val(String sql) {
        var x = repo.one(sql);
        return x == null ? 0 : x.get("n");
    }
}