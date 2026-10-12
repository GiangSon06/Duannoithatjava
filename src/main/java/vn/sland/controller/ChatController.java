package vn.sland.controller;

import jakarta.servlet.http.HttpSession;import org.springframework.stereotype.Controller;import org.springframework.ui.Model;import org.springframework.web.bind.annotation.*;import vn.sland.repository.AppRepository;import vn.sland.service.GeminiService;import vn.sland.util.WebUtil;import java.util.*;

@Controller
public class ChatController extends BaseController {
 private final AppRepository repo;private final GeminiService gemini;public ChatController(AppRepository repo,GeminiService gemini){this.repo=repo;this.gemini=gemini;}
 @GetMapping("/admin_chat.php")String admin(@RequestParam(name="user_id",required=false)Integer uid,Model m,HttpSession s){if(!WebUtil.admin(s))return "redirect:/login.php";base(m,s,"Chat khách hàng");m.addAttribute("users",repo.list("SELECT u.id,u.username,(SELECT message FROM messages WHERE (sender_id=u.id AND receiver_id=0) OR (sender_id=0 AND receiver_id=u.id) ORDER BY created_at DESC LIMIT 1) last_msg,(SELECT created_at FROM messages WHERE (sender_id=u.id AND receiver_id=0) OR (sender_id=0 AND receiver_id=u.id) ORDER BY created_at DESC LIMIT 1) last_time FROM users u WHERE u.id IN (SELECT DISTINCT sender_id FROM messages WHERE receiver_id=0) OR u.id IN (SELECT DISTINCT receiver_id FROM messages WHERE sender_id=0) ORDER BY last_time DESC"));m.addAttribute("currentUid",uid);if(uid!=null){var uRow=repo.one("SELECT username FROM users WHERE id=?",uid);m.addAttribute("currentUsername",uRow!=null?uRow.get("username"):"Khách hàng");var msgs=repo.list("SELECT *,IF(sender_id=0,'admin','user') AS sender FROM messages WHERE (sender_id=? AND receiver_id=0) OR (sender_id=0 AND receiver_id=?) ORDER BY created_at",uid,uid);m.addAttribute("messages",msgs);}else{m.addAttribute("messages",List.of());m.addAttribute("currentUsername",null);}return "admin_chat";}
 @PostMapping("/chat_backend.php")@ResponseBody Map<String,Object> chat(@RequestParam(required=false)String action,@RequestParam(required=false)String message,@RequestParam(name="user_id",required=false)Integer userId,HttpSession s){
    if("load".equals(action)||"history".equals(action)){
        if(!WebUtil.logged(s))return Map.of("status","error");
        int uid=WebUtil.admin(s)&&userId!=null?userId:WebUtil.intv(s.getAttribute("user_id"));
        var rows=repo.list("SELECT * FROM messages WHERE (sender_id=? AND receiver_id=0) OR (sender_id=0 AND receiver_id=?) ORDER BY created_at",uid,uid);
        return Map.of("status","success","messages",rows,"history",rows);
    }
    if(message==null||message.trim().isBlank())return Map.of("status","error","message","Tin nhắn trống");
    if(WebUtil.admin(s)){
        if(userId==null)return Map.of("status","error");
        repo.update("INSERT INTO messages(sender_id,receiver_id,message,created_at) VALUES(0,?,?,NOW())",userId,message.trim());
        return Map.of("status","success");
    }
    int uid=WebUtil.intv(s.getAttribute("user_id"));
    if(uid>0){
        repo.update("INSERT INTO messages(sender_id,receiver_id,message,created_at) VALUES(?,0,?,NOW())",uid,message.trim());
    }
    
    // Xây dựng context sản phẩm từ database cho Gemini AI
    List<Map<String,Object>> prods = repo.list("SELECT name, price, material, dimensions, warranty, quantity FROM products LIMIT 25");
    StringBuilder sb = new StringBuilder();
    sb.append("Bạn là trợ lý tư vấn bán hàng của cửa hàng nội thất GS LUXURY.\n");
    sb.append("Nhiệm vụ: tư vấn sản phẩm, giải đáp giá cả, chất liệu, kích thước, bảo hành cho khách hàng một cách thân thiện, nhiệt tình, ngắn gọn bằng tiếng Việt.\n");
    sb.append("Nếu khách muốn đặt mua, hướng dẫn khách nhấn nút 'Thêm vào giỏ hàng' hoặc 'Mua ngay' trên trang sản phẩm.\n\n");
    sb.append("Danh sách sản phẩm hiện có tại GS LUXURY:\n");
    for (var p : prods) {
        sb.append("- ").append(p.get("name")).append(": ").append(WebUtil.money(p.get("price"))).append("đ");
        if (p.get("material") != null) sb.append(", Chất liệu: ").append(p.get("material"));
        if (p.get("dimensions") != null) sb.append(", Kích thước: ").append(p.get("dimensions"));
        if (p.get("warranty") != null) sb.append(", Bảo hành: ").append(p.get("warranty"));
        if (WebUtil.intv(p.get("quantity")) <= 0) sb.append(" (Tạm hết)");
        sb.append("\n");
    }
    sb.append("\nThông tin cửa hàng GS LUXURY:\n- Hotline: 0326976832\n- Chính sách: Miễn phí vận chuyển toàn quốc cho đơn từ 5 triệu, bảo hành chính hãng.\n\n");
    sb.append("Khách hàng hỏi: ").append(message.trim());

    String ai = gemini.reply(sb.toString());
    if(uid>0){
        repo.update("INSERT INTO messages(sender_id,receiver_id,message,created_at) VALUES(0,?,?,NOW())", uid, ai);
    }
    return Map.of("status","success","ai_reply",ai,"reply",ai);
 }
}
