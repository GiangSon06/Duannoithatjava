package vn.sland.controller;

import jakarta.servlet.http.HttpSession;import org.springframework.stereotype.Controller;import org.springframework.ui.Model;import org.springframework.web.bind.annotation.*;import vn.sland.repository.AppRepository;import vn.sland.service.ShopService;import vn.sland.util.WebUtil;

@Controller
public class SiteController extends BaseController {
    private final ShopService shop;private final AppRepository repo;public SiteController(ShopService shop,AppRepository repo){this.shop=shop;this.repo=repo;}
    @GetMapping({"/","/index.php"})String home(Model m,HttpSession s,@RequestParam(required=false)String ref){if(ref!=null&&!WebUtil.cleanRef(ref).isBlank())s.setAttribute("aff_ref",WebUtil.cleanRef(ref));base(m,s,"Trang chủ - Nội Thất SLAND");m.addAttribute("products",shop.featured());m.addAttribute("reviews",shop.reviews(null,3,0));return "index";}
    @GetMapping("/products.php")String products(Model m,HttpSession s,@RequestParam(required=false)String search,@RequestParam(required=false)Integer cate,@RequestParam(required=false,name="category")Integer category,@RequestParam(required=false)String sort){base(m,s,"Sản phẩm - Nội Thất SLAND");Integer c=cate!=null?cate:category;m.addAttribute("products",shop.products(search,c,sort,200,0));m.addAttribute("categories",shop.categories());m.addAttribute("selectedCate",c);m.addAttribute("search",search);return "products";}
    @GetMapping("/product_detail.php")String detail(@RequestParam int id,Model m,HttpSession s){var p=shop.product(id);if(p==null)return "redirect:/products.php";base(m,s,String.valueOf(p.get("name")));m.addAttribute("product",p);m.addAttribute("reviews",shop.reviews(id,20,0));m.addAttribute("related",shop.related(id,WebUtil.intv(p.get("category_id"))));return "product_detail";}
    @GetMapping("/promotion.php")String promotion(Model m,HttpSession s){base(m,s,"Khuyến mãi");m.addAttribute("products",shop.saleProducts());m.addAttribute("vouchers",repo.list("SELECT * FROM vouchers WHERE is_active=1 AND (expires_at IS NULL OR expires_at>=CURDATE()) ORDER BY id DESC"));return "promotion";}
    @GetMapping("/about.php")String about(Model m,HttpSession s){base(m,s,"Giới thiệu");return "about";}
    @GetMapping("/contact.php")String contact(Model m,HttpSession s,@RequestParam(required=false)Integer sent){base(m,s,"Liên hệ");m.addAttribute("sent",sent!=null);return "contact";}
    @PostMapping("/contact.php")String contactPost(@RequestParam(name="full_name")String name,@RequestParam String phone,@RequestParam(required=false)String email,@RequestParam String message){repo.update("INSERT INTO contacts(full_name,phone,message,created_at,status) VALUES(?,?,?,NOW(),'new')",name,phone,message);return "redirect:/contact.php?sent=1";}
}
