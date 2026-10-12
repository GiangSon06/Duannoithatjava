package vn.sland.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GhnService {

    private final String apiUrl;
    private final String token;
    private final int shopId;
    private final int fromDistrictId;
    private final String fromWardCode;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper;

    // Cache danh mục để tải cực nhanh
    private List<Map<String, Object>> cachedProvinces = null;
    private final Map<Integer, List<Map<String, Object>>> cachedDistricts = new ConcurrentHashMap<>();
    private final Map<Integer, List<Map<String, Object>>> cachedWards = new ConcurrentHashMap<>();

    public GhnService(ObjectMapper mapper,
                      @Value("${app.ghn.api-url:https://dev-online-gateway.ghn.vn/shiip/public-api/}") String apiUrl,
                      @Value("${app.ghn.token:2fc7d720-a764-11f1-a973-aee5264794df}") String token,
                      @Value("${app.ghn.shop-id:216518}") int shopId,
                      @Value("${app.ghn.from-district-id:1482}") int fromDistrictId,
                      @Value("${app.ghn.from-ward-code:11007}") String fromWardCode) {
        this.mapper = mapper;
        this.apiUrl = apiUrl.endsWith("/") ? apiUrl : apiUrl + "/";
        this.token = token;
        this.shopId = shopId;
        this.fromDistrictId = fromDistrictId;
        this.fromWardCode = fromWardCode;
    }

    public List<Map<String, Object>> getProvinces() {
        if (cachedProvinces != null && !cachedProvinces.isEmpty()) {
            return cachedProvinces;
        }
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(apiUrl + "master-data/province"))
                    .header("Token", token)
                    .GET()
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                JsonNode root = mapper.readTree(res.body());
                List<Map<String, Object>> list = new ArrayList<>();
                for (JsonNode n : root.path("data")) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", n.path("ProvinceID").asInt());
                    m.put("name", n.path("ProvinceName").asText());
                    list.add(m);
                }
                list.sort(Comparator.comparing(a -> String.valueOf(a.get("name"))));
                cachedProvinces = list;
                return list;
            }
        } catch (Exception e) {
            System.err.println("[GHN] getProvinces error: " + e.getMessage());
        }
        return Collections.emptyList();
    }

    public List<Map<String, Object>> getDistricts(int provinceId) {
        if (cachedDistricts.containsKey(provinceId)) {
            return cachedDistricts.get(provinceId);
        }
        try {
            String body = "{\"province_id\":" + provinceId + "}";
            HttpRequest req = HttpRequest.newBuilder(URI.create(apiUrl + "master-data/district"))
                    .header("Token", token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                JsonNode root = mapper.readTree(res.body());
                List<Map<String, Object>> list = new ArrayList<>();
                for (JsonNode n : root.path("data")) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", n.path("DistrictID").asInt());
                    m.put("name", n.path("DistrictName").asText());
                    list.add(m);
                }
                list.sort(Comparator.comparing(a -> String.valueOf(a.get("name"))));
                cachedDistricts.put(provinceId, list);
                return list;
            }
        } catch (Exception e) {
            System.err.println("[GHN] getDistricts error: " + e.getMessage());
        }
        return Collections.emptyList();
    }

    public List<Map<String, Object>> getWards(int districtId) {
        if (cachedWards.containsKey(districtId)) {
            return cachedWards.get(districtId);
        }
        try {
            String body = "{\"district_id\":" + districtId + "}";
            HttpRequest req = HttpRequest.newBuilder(URI.create(apiUrl + "master-data/ward?district_id=" + districtId))
                    .header("Token", token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                JsonNode root = mapper.readTree(res.body());
                List<Map<String, Object>> list = new ArrayList<>();
                for (JsonNode n : root.path("data")) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("code", n.path("WardCode").asText());
                    m.put("name", n.path("WardName").asText());
                    list.add(m);
                }
                list.sort(Comparator.comparing(a -> String.valueOf(a.get("name"))));
                cachedWards.put(districtId, list);
                return list;
            }
        } catch (Exception e) {
            System.err.println("[GHN] getWards error: " + e.getMessage());
        }
        return Collections.emptyList();
    }

    public Map<String, Object> calculateShipping(int toDistrictId, String toWardCode, int weightGram, long orderValue) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "error");

        try {
            // 1. Tìm gói dịch vụ khả dụng giữa 2 quận huyện
            int serviceId = 53321; // mặc định chuẩn GHN
            int serviceTypeId = 2;
            String serviceName = "Giao hàng chuẩn (GHN)";

            try {
                String sBody = "{\"shop_id\":" + shopId + ",\"from_district\":" + fromDistrictId + ",\"to_district\":" + toDistrictId + "}";
                HttpRequest sReq = HttpRequest.newBuilder(URI.create(apiUrl + "v2/shipping-order/available-services"))
                        .header("Token", token)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(sBody))
                        .build();
                HttpResponse<String> sRes = http.send(sReq, HttpResponse.BodyHandlers.ofString());
                if (sRes.statusCode() == 200) {
                    JsonNode sNode = mapper.readTree(sRes.body());
                    JsonNode arr = sNode.path("data");
                    if (arr.isArray() && arr.size() > 0) {
                        serviceId = arr.get(0).path("service_id").asInt(53321);
                        serviceTypeId = arr.get(0).path("service_type_id").asInt(2);
                        serviceName = arr.get(0).path("short_name").asText("Giao hàng chuẩn (GHN)");
                    }
                }
            } catch (Exception ignored) {}

            // 2. Tính phí vận chuyển GHN
            int weight = Math.max(1000, weightGram);
            String feeBody = "{"
                    + "\"from_district_id\":" + fromDistrictId + ","
                    + "\"service_id\":" + serviceId + ","
                    + "\"service_type_id\":" + serviceTypeId + ","
                    + "\"to_district_id\":" + toDistrictId + ","
                    + "\"to_ward_code\":\"" + toWardCode + "\","
                    + "\"height\":20,"
                    + "\"length\":30,"
                    + "\"width\":20,"
                    + "\"weight\":" + weight + ","
                    + "\"insurance_value\":" + Math.min(5000000L, Math.max(0, orderValue)) + ","
                    + "\"coupon\":null"
                    + "}";

            HttpRequest fReq = HttpRequest.newBuilder(URI.create(apiUrl + "v2/shipping-order/fee"))
                    .header("Token", token)
                    .header("ShopId", String.valueOf(shopId))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(feeBody))
                    .build();
            HttpResponse<String> fRes = http.send(fReq, HttpResponse.BodyHandlers.ofString());

            if (fRes.statusCode() == 200) {
                JsonNode fNode = mapper.readTree(fRes.body());
                long fee = fNode.path("data").path("total").asLong(0);

                result.put("status", "success");
                result.put("fee", fee);
                result.put("service_name", serviceName);

                // 3. Dự kiến ngày giao hàng (Leadtime)
                try {
                    String ltBody = "{\"from_district_id\":" + fromDistrictId + ",\"from_ward_code\":\"" + fromWardCode + "\",\"to_district_id\":" + toDistrictId + ",\"to_ward_code\":\"" + toWardCode + "\",\"service_id\":" + serviceId + "}";
                    HttpRequest ltReq = HttpRequest.newBuilder(URI.create(apiUrl + "v2/shipping-order/leadtime"))
                            .header("Token", token)
                            .header("ShopId", String.valueOf(shopId))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(ltBody))
                            .build();
                    HttpResponse<String> ltRes = http.send(ltReq, HttpResponse.BodyHandlers.ofString());
                    if (ltRes.statusCode() == 200) {
                        JsonNode ltNode = mapper.readTree(ltRes.body());
                        long leadTimeSec = ltNode.path("data").path("leadtime").asLong(0);
                        if (leadTimeSec > 0) {
                            ZonedDateTime dt = Instant.ofEpochSecond(leadTimeSec).atZone(ZoneId.of("Asia/Ho_Chi_Minh"));
                            String formatted = dt.format(DateTimeFormatter.ofPattern("'Ngày' dd/MM/yyyy"));
                            result.put("leadtime_date", formatted);
                            result.put("leadtime_note", "Dự kiến giao bởi Giao Hàng Nhanh (GHN)");
                        }
                    }
                } catch (Exception ignored) {}

                return result;
            } else {
                System.err.println("[GHN] fee failed HTTP " + fRes.statusCode() + ": " + fRes.body());
                result.put("message", "Không thể tính phí vận chuyển GHN cho tuyến đường này.");
            }
        } catch (Exception e) {
            System.err.println("[GHN] calculateShipping error: " + e.getMessage());
            result.put("message", "Lỗi kết nối tới hệ thống GHN: " + e.getMessage());
        }

        return result;
    }
}
