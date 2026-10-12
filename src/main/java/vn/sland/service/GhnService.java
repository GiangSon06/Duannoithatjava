package vn.sland.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.DayOfWeek;
import java.time.LocalDate;
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
    private final DeliveryService deliveryService;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();
    private final ObjectMapper mapper;

    // Cache danh mục để tải cực nhanh và tiết kiệm tài nguyên
    private List<Map<String, Object>> cachedProvinces = null;
    private final Map<Integer, List<Map<String, Object>>> cachedDistricts = new ConcurrentHashMap<>();
    private final Map<Integer, List<Map<String, Object>>> cachedWards = new ConcurrentHashMap<>();

    private final Map<Integer, String> provinceNameById = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> districtToProvinceId = new ConcurrentHashMap<>();

    public GhnService(ObjectMapper mapper,
                      DeliveryService deliveryService,
                      @Value("${app.ghn.api-url:https://dev-online-gateway.ghn.vn/shiip/public-api/}") String apiUrl,
                      @Value("${app.ghn.token:2fc7d720-a764-11f1-a973-aee5264794df}") String token,
                      @Value("${app.ghn.shop-id:216518}") int shopId,
                      @Value("${app.ghn.from-district-id:1482}") int fromDistrictId,
                      @Value("${app.ghn.from-ward-code:11007}") String fromWardCode) {
        this.mapper = mapper;
        this.deliveryService = deliveryService;
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

        // Bước 1: Thử GHN API
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(apiUrl + "master-data/province"))
                    .header("Token", token)
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                JsonNode root = mapper.readTree(res.body());
                JsonNode data = root.path("data");
                if (data.isArray() && data.size() > 0) {
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (JsonNode n : data) {
                        Map<String, Object> m = new LinkedHashMap<>();
                        int id = n.path("ProvinceID").asInt();
                        String name = n.path("ProvinceName").asText();
                        m.put("id", id);
                        m.put("name", name);
                        list.add(m);
                        provinceNameById.put(id, name);
                    }
                    list.sort(Comparator.comparing(a -> String.valueOf(a.get("name"))));
                    cachedProvinces = list;
                    return list;
                }
            }
        } catch (Exception e) {
            System.err.println("[GHN] getProvinces remote unavailable (" + e.getMessage() + "), using fallback.");
        }

        // Bước 2: Fallback qua Open API Việt Nam (open-api.vn)
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://provinces.open-api.vn/api/p/"))
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                JsonNode root = mapper.readTree(res.body());
                if (root.isArray() && root.size() > 0) {
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (JsonNode n : root) {
                        Map<String, Object> m = new LinkedHashMap<>();
                        int id = n.path("code").asInt();
                        String name = n.path("name").asText();
                        m.put("id", id);
                        m.put("name", name);
                        list.add(m);
                        provinceNameById.put(id, name);
                    }
                    list.sort(Comparator.comparing(a -> String.valueOf(a.get("name"))));
                    cachedProvinces = list;
                    return list;
                }
            }
        } catch (Exception e) {
            System.err.println("[GHN-Fallback] open-api.vn error: " + e.getMessage());
        }

        // Bước 3: Fallback tĩnh từ từ điển 63 tỉnh của DeliveryService (luôn đảm bảo có dữ liệu)
        List<Map<String, Object>> staticList = new ArrayList<>();
        int idCounter = 1;
        for (String pName : deliveryService.getProvinceKmMap().keySet()) {
            if (pName.contains("TP.HCM") || pName.equals("Huế") || pName.equals("Thành phố Hà Nội")) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", idCounter);
            m.put("name", pName);
            staticList.add(m);
            provinceNameById.put(idCounter, pName);
            idCounter++;
        }
        staticList.sort(Comparator.comparing(a -> String.valueOf(a.get("name"))));
        cachedProvinces = staticList;
        return staticList;
    }

    public List<Map<String, Object>> getDistricts(int provinceId) {
        if (cachedDistricts.containsKey(provinceId)) {
            return cachedDistricts.get(provinceId);
        }

        // Bước 1: Thử GHN
        try {
            String body = "{\"province_id\":" + provinceId + "}";
            HttpRequest req = HttpRequest.newBuilder(URI.create(apiUrl + "master-data/district"))
                    .header("Token", token)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(3))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                JsonNode root = mapper.readTree(res.body());
                JsonNode data = root.path("data");
                if (data.isArray() && data.size() > 0) {
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (JsonNode n : data) {
                        Map<String, Object> m = new LinkedHashMap<>();
                        int dId = n.path("DistrictID").asInt();
                        String dName = n.path("DistrictName").asText();
                        m.put("id", dId);
                        m.put("name", dName);
                        list.add(m);
                        districtToProvinceId.put(dId, provinceId);
                    }
                    list.sort(Comparator.comparing(a -> String.valueOf(a.get("name"))));
                    cachedDistricts.put(provinceId, list);
                    return list;
                }
            }
        } catch (Exception ignored) {}

        // Bước 2: Fallback qua open-api.vn
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://provinces.open-api.vn/api/p/" + provinceId + "?depth=2"))
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                JsonNode root = mapper.readTree(res.body());
                JsonNode districts = root.path("districts");
                if (districts.isArray() && districts.size() > 0) {
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (JsonNode n : districts) {
                        Map<String, Object> m = new LinkedHashMap<>();
                        int dId = n.path("code").asInt();
                        String dName = n.path("name").asText();
                        m.put("id", dId);
                        m.put("name", dName);
                        list.add(m);
                        districtToProvinceId.put(dId, provinceId);
                    }
                    list.sort(Comparator.comparing(a -> String.valueOf(a.get("name"))));
                    cachedDistricts.put(provinceId, list);
                    return list;
                }
            }
        } catch (Exception ignored) {}

        // Bước 3: Fallback tĩnh
        List<Map<String, Object>> fallback = new ArrayList<>();
        String provName = provinceNameById.getOrDefault(provinceId, "Tỉnh/Thành");
        fallback.add(Map.of("id", provinceId * 100 + 1, "name", "Quận / Huyện trung tâm (" + provName + ")"));
        fallback.add(Map.of("id", provinceId * 100 + 2, "name", "Khu vực nội thành"));
        fallback.add(Map.of("id", provinceId * 100 + 3, "name", "Khu vực ngoại thành"));
        for (var item : fallback) {
            districtToProvinceId.put((Integer) item.get("id"), provinceId);
        }
        cachedDistricts.put(provinceId, fallback);
        return fallback;
    }

    public List<Map<String, Object>> getWards(int districtId) {
        if (cachedWards.containsKey(districtId)) {
            return cachedWards.get(districtId);
        }

        // Bước 1: Thử GHN
        try {
            String body = "{\"district_id\":" + districtId + "}";
            HttpRequest req = HttpRequest.newBuilder(URI.create(apiUrl + "master-data/ward?district_id=" + districtId))
                    .header("Token", token)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(3))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                JsonNode root = mapper.readTree(res.body());
                JsonNode data = root.path("data");
                if (data.isArray() && data.size() > 0) {
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (JsonNode n : data) {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("code", n.path("WardCode").asText());
                        m.put("name", n.path("WardName").asText());
                        list.add(m);
                    }
                    list.sort(Comparator.comparing(a -> String.valueOf(a.get("name"))));
                    cachedWards.put(districtId, list);
                    return list;
                }
            }
        } catch (Exception ignored) {}

        // Bước 2: Fallback qua open-api.vn
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://provinces.open-api.vn/api/d/" + districtId + "?depth=2"))
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                JsonNode root = mapper.readTree(res.body());
                JsonNode wards = root.path("wards");
                if (wards.isArray() && wards.size() > 0) {
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (JsonNode n : wards) {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("code", String.valueOf(n.path("code").asInt()));
                        m.put("name", n.path("name").asText());
                        list.add(m);
                    }
                    list.sort(Comparator.comparing(a -> String.valueOf(a.get("name"))));
                    cachedWards.put(districtId, list);
                    return list;
                }
            }
        } catch (Exception ignored) {}

        // Bước 3: Fallback tĩnh
        List<Map<String, Object>> fallback = new ArrayList<>();
        fallback.add(Map.of("code", districtId + "_01", "name", "Phường / Xã trung tâm"));
        fallback.add(Map.of("code", districtId + "_02", "name", "Khu vực 1"));
        fallback.add(Map.of("code", districtId + "_03", "name", "Khu vực 2"));
        cachedWards.put(districtId, fallback);
        return fallback;
    }

    public Map<String, Object> calculateShipping(int toDistrictId, String toWardCode, int weightGram, long orderValue) {
        return calculateShipping(toDistrictId, toWardCode, weightGram, orderValue, null);
    }

    public Map<String, Object> calculateShipping(int toDistrictId, String toWardCode, int weightGram, long orderValue, String provinceName) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. Thử qua GHN API nếu có thể
        try {
            int serviceId = 53321;
            int serviceTypeId = 2;
            String serviceName = "Giao hàng chuẩn (GHN)";

            try {
                String sBody = "{\"shop_id\":" + shopId + ",\"from_district\":" + fromDistrictId + ",\"to_district\":" + toDistrictId + "}";
                HttpRequest sReq = HttpRequest.newBuilder(URI.create(apiUrl + "v2/shipping-order/available-services"))
                        .header("Token", token)
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(3))
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
                    .timeout(Duration.ofSeconds(3))
                    .POST(HttpRequest.BodyPublishers.ofString(feeBody))
                    .build();
            HttpResponse<String> fRes = http.send(fReq, HttpResponse.BodyHandlers.ofString());

            if (fRes.statusCode() == 200) {
                JsonNode fNode = mapper.readTree(fRes.body());
                long fee = fNode.path("data").path("total").asLong(0);
                if (fee > 0) {
                    result.put("status", "success");
                    result.put("fee", fee);
                    result.put("service_name", serviceName);

                    try {
                        String ltBody = "{\"from_district_id\":" + fromDistrictId + ",\"from_ward_code\":\"" + fromWardCode + "\",\"to_district_id\":" + toDistrictId + ",\"to_ward_code\":\"" + toWardCode + "\",\"service_id\":" + serviceId + "}";
                        HttpRequest ltReq = HttpRequest.newBuilder(URI.create(apiUrl + "v2/shipping-order/leadtime"))
                                .header("Token", token)
                                .header("ShopId", String.valueOf(shopId))
                                .header("Content-Type", "application/json")
                                .timeout(Duration.ofSeconds(3))
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
                                result.put("leadtime_note", "Dự kiến giao bởi GHN");
                            }
                        }
                    } catch (Exception ignored) {}

                    return result;
                }
            }
        } catch (Exception ignored) {}

        // 2. Fallback tính phí chính xác theo DeliveryService
        String prov = provinceName;
        if (prov == null || prov.isBlank()) {
            Integer provId = districtToProvinceId.get(toDistrictId);
            if (provId != null) {
                prov = provinceNameById.get(provId);
            }
        }

        int km = deliveryService.kmForProvince(prov);
        long fee = DeliveryService.calcShippingFee(km);
        int days = km >= 0 ? DeliveryService.kmToDays(km) : 3;

        LocalDate est = LocalDate.now();
        int added = 0;
        while (added < days) {
            est = est.plusDays(1);
            if (est.getDayOfWeek() != DayOfWeek.SATURDAY && est.getDayOfWeek() != DayOfWeek.SUNDAY) added++;
        }
        String formattedDate = est.format(DateTimeFormatter.ofPattern("'Ngày' dd/MM/yyyy"));

        result.put("status", "success");
        result.put("fee", fee);
        result.put("service_name", "Giao hàng chuẩn (GS LUXURY Express)");
        result.put("leadtime_date", formattedDate);
        result.put("leadtime_note", "Dự kiến giao hàng trong " + days + " ngày làm việc");

        return result;
    }
}
