package vn.sland.controller;

import java.util.Map;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.sland.repository.AppRepository;
import vn.sland.util.WebUtil;

@Controller
public class AdminAffiliateController extends BaseController {
    private final AppRepository repo;

    public AdminAffiliateController(AppRepository repo) {
        this.repo = repo;
    }

    @GetMapping({"/admin_affiliate.php", "/admin_affiliates.php"})
    public String page(@RequestParam(defaultValue = "affiliates") String tab, Model m, HttpSession s) {
        if (!WebUtil.admin(s)) return "redirect:/login.php";
        repo.update("UPDATE affiliates a LEFT JOIN (SELECT affiliate_id,COALESCE(SUM(amount),0) sumw FROM affiliate_withdrawals WHERE status='approved' GROUP BY affiliate_id) w ON a.id=w.affiliate_id SET a.total_withdrawn=COALESCE(w.sumw,0)");
        base(m, s, "Quản lý Affiliate");
        m.addAttribute("tab", tab);
        m.addAttribute("affiliates", repo.list("SELECT a.*,u.username,u.email FROM affiliates a JOIN users u ON u.id=a.user_id ORDER BY a.created_at DESC"));
        m.addAttribute("withdrawals", repo.list("SELECT w.*,a.ref_code,u.username FROM affiliate_withdrawals w JOIN affiliates a ON a.id=w.affiliate_id JOIN users u ON u.id=a.user_id ORDER BY w.created_at DESC"));
        m.addAttribute("commissions", repo.list("SELECT ac.*,a.ref_code,u.username aff_name,o.fullname buyer_name FROM affiliate_commissions ac JOIN affiliates a ON a.id=ac.affiliate_id JOIN users u ON u.id=a.user_id LEFT JOIN orders o ON o.id=ac.order_id ORDER BY ac.created_at DESC"));
        return "admin_affiliate";
    }

    @PostMapping({"/admin_affiliate.php", "/admin_affiliates.php"})
    public String action(@RequestParam String action, @RequestParam Map<String, String> p, HttpSession s) {
        if (!WebUtil.admin(s)) return "redirect:/login.php";
        String redirectTab = "affiliates";
        switch (action) {
            case "approve_aff" -> {
                repo.update("UPDATE affiliates SET status='active' WHERE id=?", Integer.parseInt(p.get("aff_id")));
                redirectTab = "affiliates";
            }
            case "ban_aff" -> {
                repo.update("UPDATE affiliates SET status='banned' WHERE id=?", Integer.parseInt(p.get("aff_id")));
                redirectTab = "affiliates";
            }
            case "update_rate" -> {
                repo.update("UPDATE affiliates SET commission_rate=? WHERE id=?", Math.max(1, Math.min(50, Double.parseDouble(p.getOrDefault("rate", "5")))), Integer.parseInt(p.get("aff_id")));
                redirectTab = "affiliates";
            }
            case "approve_withdrawal" -> {
                redirectTab = "withdrawals";
                int id = Integer.parseInt(p.get("wd_id"));
                var w = repo.one("SELECT * FROM affiliate_withdrawals WHERE id=?", id);
                if (w != null && "pending".equals(w.get("status"))) {
                    repo.update("UPDATE affiliate_withdrawals SET status='approved',admin_note=?,processed_at=NOW() WHERE id=?", p.getOrDefault("admin_note", ""), id);
                    repo.update("UPDATE affiliates SET total_withdrawn=total_withdrawn+? WHERE id=?", w.get("amount"), w.get("affiliate_id"));
                }
            }
            case "reject_withdrawal" -> {
                redirectTab = "withdrawals";
                int id = Integer.parseInt(p.get("wd_id"));
                var w = repo.one("SELECT * FROM affiliate_withdrawals WHERE id=?", id);
                if (w != null && "pending".equals(w.get("status"))) {
                    repo.update("UPDATE affiliate_withdrawals SET status='rejected',admin_note=?,processed_at=NOW() WHERE id=?", p.getOrDefault("admin_note", "Không đủ điều kiện"), id);
                    repo.update("UPDATE affiliates SET balance=balance+? WHERE id=?", w.get("amount"), w.get("affiliate_id"));
                }
            }
            case "confirm_commission" -> {
                redirectTab = "commissions";
                int id = Integer.parseInt(p.get("comm_id"));
                var c = repo.one("SELECT * FROM affiliate_commissions WHERE id=?", id);
                if (c != null && "pending".equals(c.get("status"))) {
                    repo.update("UPDATE affiliate_commissions SET status='confirmed' WHERE id=?", id);
                    repo.update("UPDATE affiliates SET balance=balance+?,total_earned=total_earned+? WHERE id=?", c.get("commission_amt"), c.get("commission_amt"), c.get("affiliate_id"));
                }
            }
            default -> {}
        }
        return "redirect:/admin_affiliate.php?tab=" + redirectTab;
    }
}
