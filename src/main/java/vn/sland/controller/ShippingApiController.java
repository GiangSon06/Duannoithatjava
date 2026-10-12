package vn.sland.controller;

import org.springframework.web.bind.annotation.*;
import vn.sland.service.GhnService;
import vn.sland.util.WebUtil;

import java.util.*;

@RestController
@RequestMapping("/api/shipping")
public class ShippingApiController {

    private final GhnService ghn;

    public ShippingApiController(GhnService ghn) {
        this.ghn = ghn;
    }

    @GetMapping("/provinces")
    public List<Map<String, Object>> provinces() {
        return ghn.getProvinces();
    }

    @GetMapping("/districts")
    public List<Map<String, Object>> districts(@RequestParam int province_id) {
        return ghn.getDistricts(province_id);
    }

    @GetMapping("/wards")
    public List<Map<String, Object>> wards(@RequestParam int district_id) {
        return ghn.getWards(district_id);
    }

    @PostMapping("/calculate-fee")
    public Map<String, Object> calculateFee(@RequestParam int district_id,
                                           @RequestParam String ward_code,
                                           @RequestParam(defaultValue = "2000") int weight,
                                           @RequestParam(defaultValue = "0") long total) {
        Map<String, Object> res = ghn.calculateShipping(district_id, ward_code, weight, total);
        if ("success".equals(res.get("status"))) {
            long ghnFee = WebUtil.longv(res.get("fee"));
            // Chính sách GS LUXURY: Miễn phí vận chuyển toàn quốc cho đơn từ 5 triệu
            boolean freeShip = total >= 5000000L;
            long finalFee = freeShip ? 0 : ghnFee;

            res.put("ghn_fee", ghnFee);
            res.put("final_fee", finalFee);
            res.put("is_free_shipping", freeShip);
            res.put("formatted_fee", finalFee == 0 ? "Miễn phí" : WebUtil.money(finalFee));
            if (freeShip) {
                res.put("promo_note", "Đơn hàng từ 5 triệu được miễn phí vận chuyển toàn quốc (tiết kiệm " + WebUtil.money(ghnFee) + ")");
            }
        }
        return res;
    }
}
