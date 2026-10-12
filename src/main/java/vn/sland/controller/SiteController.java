package vn.sland.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.sland.repository.AppRepository;
import vn.sland.service.ShopService;
import vn.sland.util.WebUtil;
import java.util.*;

@Controller
public class SiteController extends BaseController {

    private final ShopService shop;
    private final AppRepository repo;

    public SiteController(ShopService shop, AppRepository repo) {
        this.shop = shop;
        this.repo = repo;
    }

    @GetMapping({"/", "/index.php"})
    String home(Model m, HttpSession s, @RequestParam(required = false) String ref) {
        if (ref != null && !WebUtil.cleanRef(ref).isBlank())
            s.setAttribute("aff_ref", WebUtil.cleanRef(ref));
        base(m, s, "Trang chủ - Nội Thất GS LUXURY");
        m.addAttribute("products", shop.featured());
        m.addAttribute("reviews",  shop.featuredReviews(6));
        return "index";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRODUCTS — đầy đủ filter: category, price, sort, search, phân trang
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/products.php")
    String products(
            Model m, HttpSession s,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer cate,
            @RequestParam(required = false, name = "price") String price,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "1") int page) {

        base(m, s, "Sản phẩm - Nội Thất GS LUXURY");

        // Chuẩn hoá tham số
        Integer cateId   = (cate != null && cate > 0) ? cate : null;
        String priceRange = (price != null && !price.isBlank()) ? price : "";
        String sortKey    = (sort  != null && !sort.isBlank())  ? sort  : "";
        String keyword    = (search != null && !search.isBlank()) ? search.trim() : "";

        // Phân trang
        int pageSize   = 12;
        int total      = shop.countProducts(keyword.isEmpty() ? null : keyword, cateId, priceRange.isEmpty() ? null : priceRange);
        int totalPages = (int) Math.ceil((double) total / pageSize);
        int pageNo     = Math.max(1, Math.min(page, Math.max(1, totalPages)));
        int offset     = (pageNo - 1) * pageSize;

        var products = shop.products(
                keyword.isEmpty()    ? null : keyword,
                cateId,
                priceRange.isEmpty() ? null : priceRange,
                sortKey.isEmpty()    ? null : sortKey,
                pageSize, offset);

        // Query-string helper — giữ bộ lọc khi click pagination/category
        StringBuilder qsAll = new StringBuilder();
        if (!keyword.isEmpty())    qsAll.append("search=").append(keyword).append("&");
        if (cateId != null)        qsAll.append("cate=").append(cateId).append("&");
        if (!priceRange.isEmpty()) qsAll.append("price=").append(priceRange).append("&");
        if (!sortKey.isEmpty())    qsAll.append("sort=").append(sortKey).append("&");
        String qs = qsAll.toString().replaceAll("&$", "");

        // Không có cate / price trong querystring
        String qsNoPage  = qs;
        String qsNoCate  = qs.replaceAll("(^|&)cate=[^&]*", "").replaceAll("^&|&$", "");
        String qsNoPrice = qs.replaceAll("(^|&)price=[^&]*", "").replaceAll("^&|&$", "");

        m.addAttribute("products",    products);
        m.addAttribute("categories",  shop.categories());
        m.addAttribute("cateId",      cateId  != null ? cateId : 0);
        m.addAttribute("priceRange",  priceRange);
        m.addAttribute("sortKey",     sortKey);
        m.addAttribute("search",      keyword);
        m.addAttribute("total",       total);
        m.addAttribute("totalPages",  totalPages);
        m.addAttribute("pageNo",      pageNo);
        m.addAttribute("qsNoPage",    qsNoPage);
        m.addAttribute("qsNoCate",    qsNoCate);
        m.addAttribute("qsNoPrice",   qsNoPrice);
        return "products";
    }

    @GetMapping("/product_detail.php")
    String detail(@RequestParam int id, Model m, HttpSession s) {
        var p = shop.product(id);
        if (p == null) return "redirect:/products.php";
        base(m, s, String.valueOf(p.get("name")));
        m.addAttribute("product",  p);
        
        int totalReviews = shop.countReviews(id);
        var reviewsFirst = shop.reviews(id, 4, 0);
        m.addAttribute("reviews", reviewsFirst);
        m.addAttribute("reviewsFirst", reviewsFirst);
        m.addAttribute("rvTotal", totalReviews);
        m.addAttribute("rvHasMore", totalReviews > 4);
        m.addAttribute("rvRemaining", Math.max(0, totalReviews - 4));

        double avg = 5.0;
        List<Map<String, Object>> starRows = new ArrayList<>();
        String[] colors = {"#27ae60", "#2ecc71", "#f1c40f", "#e67e22", "#e74c3c"};
        if (totalReviews > 0) {
            var avgRow = repo.one("SELECT AVG(rating) av, " +
                    "SUM(CASE WHEN rating=5 THEN 1 ELSE 0 END) s5, " +
                    "SUM(CASE WHEN rating=4 THEN 1 ELSE 0 END) s4, " +
                    "SUM(CASE WHEN rating=3 THEN 1 ELSE 0 END) s3, " +
                    "SUM(CASE WHEN rating=2 THEN 1 ELSE 0 END) s2, " +
                    "SUM(CASE WHEN rating=1 THEN 1 ELSE 0 END) s1 " +
                    "FROM reviews WHERE product_id=?", id);
            if (avgRow != null && avgRow.get("av") != null) {
                avg = ((Number) avgRow.get("av")).doubleValue();
                avg = Math.round(avg * 10.0) / 10.0;
            }
            for (int star = 5; star >= 1; star--) {
                int c = (avgRow != null && avgRow.get("s" + star) != null) ? ((Number) avgRow.get("s" + star)).intValue() : 0;
                int pct = totalReviews > 0 ? (int) Math.round((double) c * 100 / totalReviews) : 0;
                starRows.add(Map.of("star", star, "count", c, "pct", pct, "color", colors[5 - star]));
            }
        } else {
            for (int star = 5; star >= 1; star--) {
                starRows.add(Map.of("star", star, "count", 0, "pct", 0, "color", colors[5 - star]));
            }
        }
        m.addAttribute("rvAvg", avg);
        m.addAttribute("rvAvgText", String.format(Locale.US, "%.1f", avg));
        m.addAttribute("starRows", starRows);
        m.addAttribute("related",  shop.related(id, WebUtil.intv(p.get("category_id"))));
        return "product_detail";
    }

    @GetMapping("/promotion.php")
    String promotion(Model m, HttpSession s) {
        base(m, s, "Khuyến mãi đặc biệt - Nội Thất GS LUXURY");
        m.addAttribute("products", shop.saleProducts());
        m.addAttribute("categories", shop.categories());
        m.addAttribute("vouchers", repo.list(
            "SELECT * FROM vouchers WHERE is_active=1 AND (expires_at IS NULL OR expires_at>=CURDATE()) ORDER BY id DESC"));
        return "promotion";
    }

    @GetMapping("/about.php")
    String about(Model m, HttpSession s) {
        base(m, s, "Giới thiệu");
        return "about";
    }

    @GetMapping("/contact.php")
    String contact(Model m, HttpSession s, @RequestParam(required = false) Integer sent) {
        base(m, s, "Liên hệ");
        m.addAttribute("sent", sent != null);
        return "contact";
    }

    @PostMapping("/contact.php")
    String contactPost(
            @RequestParam(name = "full_name") String name,
            @RequestParam String phone,
            @RequestParam(required = false) String email,
            @RequestParam String message) {
        repo.update("INSERT INTO contacts(full_name,phone,message,created_at,status) VALUES(?,?,?,NOW(),'new')",
                name, phone, message);
        return "redirect:/contact.php?sent=1";
    }

    @GetMapping("/api/search-suggestions")
    @ResponseBody
    public List<Map<String, Object>> searchSuggestions(@RequestParam(required = false, defaultValue = "") String q) {
        String kw = q.trim();
        if (kw.isEmpty()) return Collections.emptyList();
        String like = "%" + kw + "%";
        return repo.list("SELECT id, name, price, image_url FROM products WHERE name LIKE ? ORDER BY id DESC LIMIT 6", like);
    }
}
