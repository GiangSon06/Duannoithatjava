package vn.sland.controller;

import jakarta.servlet.http.HttpSession;import org.springframework.web.bind.annotation.*;import vn.sland.repository.AppRepository;import vn.sland.util.WebUtil;import java.util.*;

@RestController
public class CompatibilityController {
 private final AppRepository repo;public CompatibilityController(AppRepository repo){this.repo=repo;}
 @GetMapping("/load_more_products.php")List<Map<String,Object>> moreProducts(@RequestParam(defaultValue="0")int offset){return repo.list("SELECT * FROM products ORDER BY id DESC LIMIT 8 OFFSET ?",Math.max(0,offset));}
 @PostMapping("/admin_notify.php")Map<String,Object> notify(HttpSession s){if(!WebUtil.admin(s))return Map.of("status","error");Object pending=repo.one("SELECT COUNT(*) n FROM orders WHERE status='Chờ xử lý'").get("n");return Map.of("status","success","new_contacts",repo.one("SELECT COUNT(*) n FROM contacts WHERE status='new'").get("n"),"pending_orders",pending,"pending",pending,"count",0,"orders",List.of());}
 @PostMapping("/fix_db_images.php")Map<String,Object> fix(HttpSession s){if(!WebUtil.admin(s))return Map.of("status","error");int n=repo.update("UPDATE products SET image_url=REPLACE(image_url,'picture/','/picture/') WHERE image_url LIKE 'picture/%'");return Map.of("status","success","updated",n);}
}
