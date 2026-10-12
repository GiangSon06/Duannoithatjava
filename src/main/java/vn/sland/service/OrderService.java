package vn.sland.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sland.repository.AppRepository;
import vn.sland.util.WebUtil;

import java.time.LocalDate;
import java.util.*;

@Service
public class OrderService {
    public record OrderResult(long orderId,double total,LocalDate estimated,String deliveryNote){}
    private final AppRepository repo; private final DeliveryService delivery; private final AffiliateService affiliate; private final EmailService email;
    public OrderService(AppRepository repo,DeliveryService delivery,AffiliateService affiliate,EmailService email){this.repo=repo;this.delivery=delivery;this.affiliate=affiliate;this.email=email;}

    public double cartTotal(List<Map<String,Object>> cart){return cart.stream().mapToDouble(x->WebUtil.dbl(x.get("price"))*WebUtil.intv(x.get("quantity"))).sum();}

    public Map<String,Object> validateVoucher(String code,double rawTotal){
        if(code==null||code.isBlank())return Map.of("ok",true,"discount",0d,"code","");String c=code.trim().toUpperCase(Locale.ROOT);Map<String,Object>v=repo.one("SELECT * FROM vouchers WHERE code=? AND is_active=1",c);if(v==null)return Map.of("ok",false,"message","Mã giảm giá không tồn tại hoặc đã bị vô hiệu hóa!");
        if(v.get("expires_at")!=null&&repo.one("SELECT 1 ok WHERE ?>=CURDATE()",v.get("expires_at"))==null)return Map.of("ok",false,"message","Mã giảm giá đã hết hạn!");
        if(WebUtil.intv(v.get("used_count"))>=WebUtil.intv(v.get("max_uses")))return Map.of("ok",false,"message","Mã giảm giá đã hết lượt sử dụng!");
        if(rawTotal<WebUtil.dbl(v.get("min_order")))return Map.of("ok",false,"message","Đơn hàng chưa đạt mức tối thiểu để dùng voucher.");
        double discount="percent".equals(String.valueOf(v.get("discount_type")))?Math.round(rawTotal*WebUtil.dbl(v.get("discount_value"))/100d):WebUtil.dbl(v.get("discount_value"));discount=Math.max(0,Math.min(discount,rawTotal));return Map.of("ok",true,"discount",discount,"code",c,"voucher",v);
    }

    @Transactional
    public OrderResult create(long userId,String fullname,String phone,String address,String payment,String voucherCode,List<Map<String,Object>> sessionCart,String affRef,long shippingFee){
        if(sessionCart==null||sessionCart.isEmpty())throw new IllegalArgumentException("Giỏ hàng đang trống.");
        List<Map<String,Object>> verified=new ArrayList<>();
        for(Map<String,Object> item:sessionCart){int id=WebUtil.intv(item.get("id")),q=Math.max(1,WebUtil.intv(item.get("quantity")));Map<String,Object>p=repo.one("SELECT id,name,price,image_url,quantity FROM products WHERE id=? FOR UPDATE",id);if(p==null)throw new IllegalArgumentException("Có sản phẩm không còn tồn tại.");int stock=WebUtil.intv(p.get("quantity"));if(q>stock)throw new IllegalArgumentException("Sản phẩm '"+p.get("name")+"' chỉ còn "+stock+" sản phẩm.");Map<String,Object>x=new LinkedHashMap<>(p);x.put("quantity_buy",q);verified.add(x);}
        double raw=verified.stream().mapToDouble(x->WebUtil.dbl(x.get("price"))*WebUtil.intv(x.get("quantity_buy"))).sum();Map<String,Object>voucher=validateVoucher(voucherCode,raw);if(Boolean.FALSE.equals(voucher.get("ok")))throw new IllegalArgumentException(String.valueOf(voucher.get("message")));double discount=WebUtil.dbl(voucher.get("discount")),total=Math.max(0,raw-discount+shippingFee);
        LocalDate estimated=delivery.estimatedDate(address);DeliveryService.DeliveryInfo di=delivery.forAddress(address);String note=di.emoji()+" "+di.note()+(di.km()>0?" (~"+di.km()+" km)":"");
        long orderId=repo.insert("INSERT INTO orders(user_id,fullname,phone,address,payment_method,voucher_code,discount_amount,shipping_fee,total_money,status,estimated_delivery,delivery_note,created_at) VALUES(?,?,?,?,?,?,?,?,?, 'Chờ xử lý',?,?,NOW())",userId,fullname,phone,address,payment,voucher.get("code"),discount,shippingFee,total,estimated,note).longValue();
        for(Map<String,Object>x:verified){int id=WebUtil.intv(x.get("id")),q=WebUtil.intv(x.get("quantity_buy"));repo.update("INSERT INTO order_details(order_id,product_id,product_name,price,quantity) VALUES(?,?,?,?,?)",orderId,id,x.get("name"),x.get("price"),q);repo.update("UPDATE products SET quantity=quantity-? WHERE id=?",q,id);}
        if(!String.valueOf(voucher.get("code")).isBlank())repo.update("UPDATE vouchers SET used_count=used_count+1 WHERE code=?",voucher.get("code"));repo.update("INSERT INTO order_status_history(order_id,status,note,created_at) VALUES(?,'Chờ xử lý','Đơn hàng vừa được tạo',NOW())",orderId);affiliate.recordOrderCommission(userId,orderId,total,affRef);sendConfirmation(orderId);return new OrderResult(orderId,total,estimated,note);
    }

    public void sendConfirmation(long orderId){Map<String,Object>o=repo.one("SELECT o.*,u.email FROM orders o JOIN users u ON u.id=o.user_id WHERE o.id=?",orderId);if(o==null)return;List<Map<String,Object>>d=repo.list("SELECT * FROM order_details WHERE order_id=?",orderId);StringBuilder rows=new StringBuilder();for(var x:d)rows.append("<tr><td>").append(x.get("product_name")).append("</td><td>").append(x.get("quantity")).append("</td><td>").append(WebUtil.money(x.get("price"))).append("</td></tr>");email.sendHtml(String.valueOf(o.get("email")),"Xác nhận đơn hàng #"+orderId,"<h2>GS LUXURY - Đặt hàng thành công</h2><p>Mã đơn: <b>#"+orderId+"</b></p><table border='1' cellpadding='8'>"+rows+"</table><p>Tổng: <b>"+WebUtil.money(o.get("total_money"))+"</b></p>");}

    @Transactional public String cancel(long orderId,long userId,boolean admin){Map<String,Object>o=admin?repo.one("SELECT * FROM orders WHERE id=? FOR UPDATE",orderId):repo.one("SELECT * FROM orders WHERE id=? AND user_id=? FOR UPDATE",orderId,userId);if(o==null)return "Không tìm thấy đơn hàng.";String status=String.valueOf(o.get("status"));if(status.equals("Đã hủy"))return "Đơn hàng đã hủy.";if(status.equals("Đang giao")||status.equals("Đã giao hàng")||status.equals("Đã giao"))return "Không thể hủy đơn ở trạng thái hiện tại.";for(var d:repo.list("SELECT product_id,quantity FROM order_details WHERE order_id=?",orderId))repo.update("UPDATE products SET quantity=quantity+? WHERE id=?",d.get("quantity"),d.get("product_id"));repo.update("UPDATE orders SET status='Đã hủy' WHERE id=?",orderId);repo.update("INSERT INTO order_status_history(order_id,status,note,created_at) VALUES(?,'Đã hủy','Khách hàng hủy đơn',NOW())",orderId);repo.update("UPDATE affiliate_commissions SET status='cancelled' WHERE order_id=? AND status='pending'",orderId);return null;}
}
