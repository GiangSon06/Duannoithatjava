package vn.sland.controller;

import jakarta.servlet.http.HttpSession;import org.springframework.web.bind.annotation.*;import vn.sland.repository.AppRepository;import vn.sland.service.OrderService;import vn.sland.service.ShopService;import vn.sland.util.WebUtil;import java.util.*;

@RestController
public class CartApiController {
    private final AppRepository repo;private final ShopService shop;private final OrderService orders;public CartApiController(AppRepository repo,ShopService shop,OrderService orders){this.repo=repo;this.shop=shop;this.orders=orders;}
    @PostMapping(value="/add_to_cart.php",produces="text/plain;charset=UTF-8")String add(@RequestParam int id,@RequestParam(name="quantity_add",defaultValue="1")int q,HttpSession s){Map<String,Object>p=repo.one("SELECT id,name,price,image_url,quantity FROM products WHERE id=?",id);if(p==null)return "0";int stock=WebUtil.intv(p.get("quantity"));if(stock<=0)return String.valueOf(count(s));List<Map<String,Object>>c=WebUtil.cart(s);Map<String,Object>x=c.stream().filter(i->WebUtil.intv(i.get("id"))==id).findFirst().orElse(null);if(x==null){x=new LinkedHashMap<>();x.put("id",id);x.put("name",p.get("name"));x.put("price",p.get("price"));x.put("image",p.get("image_url"));x.put("quantity",Math.min(Math.max(q,1),stock));c.add(x);}else x.put("quantity",Math.min(WebUtil.intv(x.get("quantity"))+Math.max(q,1),stock));return String.valueOf(count(s));}
    @PostMapping(value="/remove_from_cart.php",produces="text/plain;charset=UTF-8")String remove(@RequestParam int id,HttpSession s){WebUtil.cart(s).removeIf(x->WebUtil.intv(x.get("id"))==id);return String.valueOf(count(s));}
    @RequestMapping(value="/clear_cart.php",method={RequestMethod.POST,RequestMethod.GET},produces="text/plain;charset=UTF-8")String clear(HttpSession s){s.setAttribute("cart",new ArrayList<>());return "success";}
    @PostMapping({"/check_inventory.php","/check_inventory_ajax.php"})Map<String,Object> inventory(@RequestParam int id,@RequestParam(name="quantity",required=false)Integer qty,@RequestParam(name="quantity_add",required=false)Integer qtyAdd,HttpSession s){int requestedQty=qty!=null?qty:(qtyAdd!=null?qtyAdd:1);Map<String,Object>p=repo.one("SELECT quantity FROM products WHERE id=?",id);if(p==null)return Map.of("status","error","message","Sản phẩm không tồn tại.");int stock=WebUtil.intv(p.get("quantity")),inCart=WebUtil.cart(s).stream().filter(x->WebUtil.intv(x.get("id"))==id).mapToInt(x->WebUtil.intv(x.get("quantity"))).sum();if(stock<=0)return Map.of("status","error","message","Sản phẩm này đã hết hàng.");if(inCart+Math.max(1,requestedQty)>stock)return Map.of("status","error","message","Xin lỗi, số lượng tồn kho chỉ còn "+stock+" sản phẩm.");return Map.of("status","success","stock",stock);}
    @PostMapping("/apply_voucher.php")Map<String,Object> voucher(@RequestParam String code,@RequestParam double total){Map<String,Object>v=orders.validateVoucher(code,total);if(Boolean.FALSE.equals(v.get("ok")))return Map.of("status","error","message",v.get("message"));double d=WebUtil.dbl(v.get("discount"));return Map.of("status","success","message","Áp dụng mã thành công!","discount",d,"final",Math.max(0,total-d),"code",v.get("code"));}
    @GetMapping("/get_reviews.php")
    Map<String,Object> reviews(@RequestParam(name="product_id",required=false)Integer id,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="4")int limit){
        int total = shop.countReviews(id);
        List<Map<String,Object>> list = shop.reviews(id, Math.min(50, Math.max(1, limit)), Math.max(0, offset));
        int nextOffset = offset + list.size();
        boolean hasMore = nextOffset < total;
        int remaining = Math.max(0, total - nextOffset);
        return Map.of("reviews", list, "has_more", hasMore, "remaining", remaining, "total", total);
    }
    @PostMapping({"/submit_review.php","/process_review.php"})
    Map<String,Object> review(@RequestParam String content,@RequestParam(defaultValue="5")int rating,@RequestParam(name="product_id",required=false)Integer product,HttpSession s){
        if(!WebUtil.logged(s))return Map.of("status","error","message","Vui lòng đăng nhập");
        if(content==null||content.trim().length()<2)return Map.of("status","error","message","Nội dung đánh giá quá ngắn");
        int uid=WebUtil.intv(s.getAttribute("user_id"));
        repo.update("INSERT INTO reviews(user_id,product_id,content,rating,created_at) VALUES(?,?,?,?,NOW())",uid,product,content.trim(),Math.max(1,Math.min(5,rating)));
        Map<String,Object> rv=repo.one("SELECT r.*,u.username FROM reviews r JOIN users u ON u.id=r.user_id WHERE r.user_id=? AND r.product_id<=>? ORDER BY r.created_at DESC LIMIT 1",uid,product);
        Map<String,Object> resRv = new LinkedHashMap<>();
        resRv.put("username", rv != null && rv.get("username") != null ? rv.get("username") : s.getAttribute("username"));
        resRv.put("rating", rating);
        resRv.put("content", content.trim());
        resRv.put("created_at", "Vừa xong");
        return Map.of("status","success","message","Đánh giá của bạn đã được gửi thành công!","review", resRv);
    }
    private int count(HttpSession s){return WebUtil.cart(s).stream().mapToInt(x->WebUtil.intv(x.get("quantity"))).sum();}
}
