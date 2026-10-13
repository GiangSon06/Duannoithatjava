package vn.sland.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Service
public class MomoService {
    private final String partnerCode;
    private final String accessKey;
    private final String secretKey;
    private final String endpoint;
    private final String redirectUrl;
    private final String ipnUrl;
    private final String defaultRequestType;

    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public MomoService(ObjectMapper mapper,
                       @Value("${app.momo.partner-code:MOMO}") String partnerCode,
                       @Value("${app.momo.access-key:}") String accessKey,
                       @Value("${app.momo.secret-key:}") String secretKey,
                       @Value("${app.momo.endpoint:https://test-payment.momo.vn/v2/gateway/api/create}") String endpoint,
                       @Value("${app.momo.redirect-url:http://localhost:8081/momo_return.php}") String redirectUrl,
                       @Value("${app.momo.ipn-url:http://localhost:8081/momo_ipn.php}") String ipnUrl,
                       @Value("${app.momo.request-type:payWithATM}") String defaultRequestType) {
        this.mapper = mapper;
        this.partnerCode = partnerCode;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.endpoint = endpoint;
        this.redirectUrl = redirectUrl;
        this.ipnUrl = ipnUrl;
        this.defaultRequestType = defaultRequestType;
    }

    public boolean configured() {
        return accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank();
    }

    public String getRedirectUrl() { return redirectUrl; }
    public String getIpnUrl() { return ipnUrl; }

    public Map<String, Object> createPayment(String orderId, long amount, String orderInfo, String extraData) {
        return createPayment(orderId, amount, orderInfo, extraData, this.redirectUrl, this.ipnUrl);
    }

    public Map<String, Object> createPayment(String orderId, long amount, String orderInfo, String extraData, String customRedirectUrl, String customIpnUrl) {
        if (!configured()) {
            return Map.of("resultCode", 99, "message", "MoMo chưa được cấu hình MOMO_ACCESS_KEY/MOMO_SECRET_KEY");
        }
        try {
            String targetRedirectUrl = (customRedirectUrl != null && !customRedirectUrl.isBlank()) ? customRedirectUrl : redirectUrl;
            String targetIpnUrl = (customIpnUrl != null && !customIpnUrl.isBlank()) ? customIpnUrl : ipnUrl;
            String requestId = orderId + "-" + System.currentTimeMillis();
            String requestType = (defaultRequestType != null && !defaultRequestType.isBlank()) ? defaultRequestType : "payWithATM";
            String raw = "accessKey=" + accessKey +
                    "&amount=" + amount +
                    "&extraData=" + (extraData == null ? "" : extraData) +
                    "&ipnUrl=" + targetIpnUrl +
                    "&orderId=" + orderId +
                    "&orderInfo=" + orderInfo +
                    "&partnerCode=" + partnerCode +
                    "&redirectUrl=" + targetRedirectUrl +
                    "&requestId=" + requestId +
                    "&requestType=" + requestType;
            String sig = hmac(raw);

            Map<String, Object> b = new LinkedHashMap<>();
            b.put("partnerCode", partnerCode);
            b.put("partnerName", "GS LUXURY");
            b.put("storeId", "GS LUXURY");
            b.put("requestId", requestId);
            b.put("amount", amount);
            b.put("orderId", orderId);
            b.put("orderInfo", orderInfo);
            b.put("redirectUrl", targetRedirectUrl);
            b.put("ipnUrl", targetIpnUrl);
            b.put("lang", "vi");
            b.put("requestType", requestType);
            b.put("autoCapture", true);
            b.put("extraData", extraData == null ? "" : extraData);
            b.put("signature", sig);

            HttpRequest req = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(b)))
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            return mapper.readValue(res.body(), new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of("resultCode", 98, "message", e.getMessage() == null ? "MoMo error" : e.getMessage());
        }
    }

    public boolean verifyCallback(Map<String, ?> p) {
        if (!configured()) return false;
        try {
            String raw = "accessKey=" + accessKey +
                    "&amount=" + p.get("amount") +
                    "&extraData=" + nv(p.get("extraData")) +
                    "&message=" + nv(p.get("message")) +
                    "&orderId=" + p.get("orderId") +
                    "&orderInfo=" + nv(p.get("orderInfo")) +
                    "&orderType=" + nv(p.get("orderType")) +
                    "&partnerCode=" + p.get("partnerCode") +
                    "&payType=" + nv(p.get("payType")) +
                    "&requestId=" + p.get("requestId") +
                    "&responseTime=" + p.get("responseTime") +
                    "&resultCode=" + p.get("resultCode") +
                    "&transId=" + p.get("transId");
            return hmac(raw).equalsIgnoreCase(nv(p.get("signature")));
        } catch (Exception e) {
            return false;
        }
    }

    private String nv(Object x) {
        return x == null ? "" : String.valueOf(x);
    }

    private String hmac(String raw) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] h = mac.doFinal(raw.getBytes(StandardCharsets.UTF_8));
        StringBuilder s = new StringBuilder();
        for (byte b : h) {
            s.append(String.format("%02x", b));
        }
        return s.toString();
    }
}
