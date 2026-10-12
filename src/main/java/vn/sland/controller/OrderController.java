package vn.sland.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.sland.repository.AppRepository;
import vn.sland.service.*;
import vn.sland.util.WebUtil;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
public class OrderController extends BaseController {

    private final AppRepository repo;
    private final OrderService orders;
    private final MomoService momo;
    private final DeliveryService delivery;

    public OrderController(AppRepository repo, OrderService orders,
                           MomoService momo, DeliveryService delivery) {
        this.repo = repo;
        this.orders = orders;
        this.momo = momo;
        this.delivery = delivery;
    }

    @GetMapping("/checkout.php")
    String checkout(Model m, HttpSession s) {
        if (!WebUtil.logged(s)) return "redirect:/login.php";
        base(m, s, "Thanh toán");
        long uid = WebUtil.longv(s.getAttribute("user_id"));
        m.addAttribute("cart",         WebUtil.cart(s));
        m.addAttribute("rawTotal",     orders.cartTotal(WebUtil.cart(s)));
        m.addAttribute("user",         repo.one("SELECT * FROM users WHERE id=?", uid));
        m.addAttribute("addresses",    repo.list(
            "SELECT * FROM user_addresses WHERE user_id=? ORDER BY is_default DESC,id DESC", uid));
        m.addAttribute("provinceKmJson", delivery.provinceKmJson());
        return "checkout";
    }

    @PostMapping("/process_order.php")
    String process(@RequestParam String fullname,
                   @RequestParam String phone,
                   @RequestParam String address,
                   @RequestParam(name = "payment_method") String payment,
                   @RequestParam(name = "voucher_code",   required = false) String voucher,
                   @RequestParam(name = "shipping_fee",   defaultValue = "0") long shippingFee,
                   Model m, HttpSession s) {
        if (!WebUtil.logged(s)) return "redirect:/login.php";
        if ("momo".equals(payment)) {
            s.setAttribute("pending_order", new LinkedHashMap<>(Map.of(
                "fullname",     fullname,
                "phone",        phone,
                "address",      address,
                "voucher",      voucher == null ? "" : voucher,
                "shipping_fee", shippingFee)));
            return "redirect:/momo_payment.php";
        }
        return createNow(fullname, phone, address, payment, voucher, shippingFee, m, s);
    }

    private String createNow(String fullname, String phone, String address,
                              String payment, String voucher, long shippingFee, Model m, HttpSession s) {
        try {
            // Fix: kiểm tra null thật trước khi String.valueOf để tránh chuỗi "null"
            Object affRefObj = s.getAttribute("aff_ref");
            String affRef = (affRefObj != null) ? String.valueOf(affRefObj) : "";
            var result = orders.create(
                WebUtil.longv(s.getAttribute("user_id")),
                fullname, phone, address, payment, voucher,
                WebUtil.cart(s), affRef, shippingFee);
            s.setAttribute("cart", new ArrayList<>());
            s.removeAttribute("aff_ref");
            base(m, s, "Đặt hàng thành công");
            m.addAttribute("result",  result);
            m.addAttribute("payment", payment);
            // Delivery estimate
            var di = delivery.forAddress(address);
            LocalDate est = delivery.estimatedDate(address);
            m.addAttribute("estimatedDisplay",
                est.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                " (" + di.emoji() + " " + di.days() + " ngày làm việc)");
            return "order_success";
        } catch (Exception e) {
            base(m, s, "Lỗi đặt hàng");
            m.addAttribute("error", e.getMessage());
            return "order_error";
        }
    }

    @GetMapping("/momo_payment.php")
    String momoPayment(Model m, HttpSession s) {
        if (!WebUtil.logged(s)) return "redirect:/login.php";
        @SuppressWarnings("unchecked")
        Map<String, Object> p = (Map<String, Object>) s.getAttribute("pending_order");
        if (p == null) return "redirect:/checkout.php";
        double raw = orders.cartTotal(WebUtil.cart(s));
        var v = orders.validateVoucher(String.valueOf(p.get("voucher")), raw);
        if (Boolean.FALSE.equals(v.get("ok"))) {
            base(m, s, "MoMo"); m.addAttribute("error", v.get("message")); return "momo_payment";
        }
        long pendingShip = WebUtil.longv(p.get("shipping_fee"));
        long amount = Math.max(1000L, Math.round(raw - WebUtil.dbl(v.get("discount")) + pendingShip));
        String momoOrder = "GSLUXURY" + System.currentTimeMillis();
        var resp = momo.createPayment(momoOrder, amount, "Thanh toan don hang GS LUXURY", "");
        if (WebUtil.intv(resp.get("resultCode")) == 0 && resp.get("payUrl") != null) {
            s.setAttribute("momo_order_id", momoOrder);
            return "redirect:" + resp.get("payUrl");
        }
        base(m, s, "MoMo"); m.addAttribute("error", resp.get("message")); return "momo_payment";
    }

    @GetMapping({"/momo_return.php", "/momo_callback.php"})
    String momoReturn(@RequestParam Map<String, String> p, Model m, HttpSession s) {
        if (!WebUtil.logged(s)) return "redirect:/login.php";
        if (!"0".equals(p.get("resultCode")) || !momo.verifyCallback(p)) {
            base(m, s, "Thanh toán MoMo");
            m.addAttribute("error", "Thanh toán MoMo không thành công hoặc chữ ký không hợp lệ.");
            return "momo_payment";
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> pending = (Map<String, Object>) s.getAttribute("pending_order");
        if (pending == null) return "redirect:/my_orders.php";
        long pendingShip = WebUtil.longv(pending.get("shipping_fee"));
        String r = createNow(
            String.valueOf(pending.get("fullname")), String.valueOf(pending.get("phone")),
            String.valueOf(pending.get("address")), "momo",
            String.valueOf(pending.get("voucher")), pendingShip, m, s);
        if ("order_success".equals(r)) {
            if (m.asMap().get("result") instanceof OrderService.OrderResult or)
                repo.update("UPDATE orders SET momo_order_id=? WHERE id=?", p.get("orderId"), or.orderId());
            s.removeAttribute("pending_order");
        }
        return r;
    }

    @GetMapping("/my_orders.php")
    String mine(Model m, HttpSession s) {
        if (!WebUtil.logged(s)) return "redirect:/login.php";
        base(m, s, "Đơn hàng của tôi");
        m.addAttribute("orders", repo.list(
            "SELECT * FROM orders WHERE user_id=? ORDER BY created_at DESC",
            s.getAttribute("user_id")));
        return "my_orders";
    }

    @GetMapping("/order_detail.php")
    String detail(@RequestParam long id, Model m, HttpSession s) {
        if (!WebUtil.logged(s)) return "redirect:/login.php";
        var o = WebUtil.admin(s)
            ? repo.one("SELECT * FROM orders WHERE id=?", id)
            : repo.one("SELECT * FROM orders WHERE id=? AND user_id=?", id, s.getAttribute("user_id"));
        if (o == null) return "redirect:/my_orders.php";
        base(m, s, "Chi tiết đơn hàng #" + id);
        m.addAttribute("order",   o);
        m.addAttribute("details", repo.list(
            "SELECT od.*, p.image_url FROM order_details od LEFT JOIN products p ON od.product_id = p.id WHERE od.order_id=?", id));
        m.addAttribute("history", repo.list(
            "SELECT * FROM order_status_history WHERE order_id=? ORDER BY created_at", id));
        m.addAttribute("isAdmin", WebUtil.admin(s));

        String address = String.valueOf(o.getOrDefault("address", ""));
        if (!address.isBlank()) {
            var di = delivery.forAddress(address);
            m.addAttribute("deliveryKm", di.km() > 0 ? di.km() : null);
            LocalDate est = delivery.estimatedDate(address);
            m.addAttribute("estimatedDisplay",
                est.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                " (" + di.emoji() + " " + di.days() + " ngày)");
        }
        return "order_detail";
    }

    @GetMapping("/print_order.php")
    String print(@RequestParam long id, Model m, HttpSession s) {
        String r = detail(id, m, s);
        return r.equals("order_detail") ? "print_order" : r;
    }

    @PostMapping("/cancel_order.php")
    String cancel(@RequestParam(name = "order_id") long id, HttpSession s) {
        if (!WebUtil.logged(s)) return "redirect:/login.php";
        orders.cancel(id, WebUtil.longv(s.getAttribute("user_id")), WebUtil.admin(s));
        return "redirect:/order_detail.php?id=" + id;
    }

    @GetMapping("/load_more_orders.php")
    @ResponseBody
    List<Map<String, Object>> more(@RequestParam(defaultValue = "0") int offset, HttpSession s) {
        if (!WebUtil.logged(s)) return List.of();
        return repo.list(
            "SELECT * FROM orders WHERE user_id=? ORDER BY created_at DESC LIMIT 5 OFFSET ?",
            s.getAttribute("user_id"), Math.max(0, offset));
    }
}
