package vn.sland.service;

import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service
public class DeliveryService {

    /** Thông tin giao hàng (Java 17 record) */
    public record DeliveryInfo(int days, int km, String emoji, String note) {}

    private static final Map<String, Integer> PROVINCE_KM = new LinkedHashMap<>();

    static {
        PROVINCE_KM.put("Hà Nội",                      0);
        PROVINCE_KM.put("Thành phố Hà Nội",            0);
        PROVINCE_KM.put("Bắc Ninh",                   39);
        PROVINCE_KM.put("Bắc Giang",                  60);
        PROVINCE_KM.put("Hưng Yên",                   64);
        PROVINCE_KM.put("Vĩnh Phúc",                  66);
        PROVINCE_KM.put("Hải Dương",                  66);
        PROVINCE_KM.put("Hà Nam",                     74);
        PROVINCE_KM.put("Hòa Bình",                   79);
        PROVINCE_KM.put("Thái Nguyên",                85);
        PROVINCE_KM.put("Nam Định",                  101);
        PROVINCE_KM.put("Phú Thọ",                   105);
        PROVINCE_KM.put("Thái Bình",                 111);
        PROVINCE_KM.put("Ninh Bình",                 118);
        PROVINCE_KM.put("Hải Phòng",                 120);
        PROVINCE_KM.put("Tuyên Quang",               149);
        PROVINCE_KM.put("Bắc Kạn",                   168);
        PROVINCE_KM.put("Yên Bái",                   171);
        PROVINCE_KM.put("Lạng Sơn",                  177);
        PROVINCE_KM.put("Thanh Hóa",                 184);
        PROVINCE_KM.put("Quảng Ninh",                202);
        PROVINCE_KM.put("Cao Bằng",                  252);
        PROVINCE_KM.put("Sơn La",                    275);
        PROVINCE_KM.put("Hà Giang",                  293);
        PROVINCE_KM.put("Nghệ An",                   300);
        PROVINCE_KM.put("Lào Cai",                   341);
        PROVINCE_KM.put("Lai Châu",                  392);
        PROVINCE_KM.put("Điện Biên",                 400);
        PROVINCE_KM.put("Hà Tĩnh",                   401);
        PROVINCE_KM.put("Quảng Bình",                545);
        PROVINCE_KM.put("Quảng Trị",                 671);
        PROVINCE_KM.put("Thừa Thiên Huế",            728);
        PROVINCE_KM.put("Huế",                       728);
        PROVINCE_KM.put("Đà Nẵng",                   818);
        PROVINCE_KM.put("Quảng Nam",                 880);
        PROVINCE_KM.put("Quảng Ngãi",                981);
        PROVINCE_KM.put("Kon Tum",                  1048);
        PROVINCE_KM.put("Gia Lai",                  1102);
        PROVINCE_KM.put("Bình Định",                1191);
        PROVINCE_KM.put("Phú Yên",                  1279);
        PROVINCE_KM.put("Đắk Lắk",                  1295);
        PROVINCE_KM.put("Đắk Nông",                 1380);
        PROVINCE_KM.put("Bình Phước",               1398);
        PROVINCE_KM.put("Khánh Hòa",                1404);
        PROVINCE_KM.put("Lâm Đồng",                 1413);
        PROVINCE_KM.put("Tây Ninh",                 1459);
        PROVINCE_KM.put("Bình Dương",               1459);
        PROVINCE_KM.put("Ninh Thuận",               1490);
        PROVINCE_KM.put("Bình Thuận",               1526);
        PROVINCE_KM.put("Đồng Nai",                 1527);
        PROVINCE_KM.put("TP. Hồ Chí Minh",          1536);
        PROVINCE_KM.put("Hồ Chí Minh",              1536);
        PROVINCE_KM.put("Thành phố Hồ Chí Minh",   1536);
        PROVINCE_KM.put("TP.HCM",                   1536);
        PROVINCE_KM.put("Long An",                  1552);
        PROVINCE_KM.put("An Giang",                 1581);
        PROVINCE_KM.put("Đồng Tháp",                1582);
        PROVINCE_KM.put("Bà Rịa - Vũng Tàu",       1587);
        PROVINCE_KM.put("Tiền Giang",               1590);
        PROVINCE_KM.put("Vĩnh Long",                1620);
        PROVINCE_KM.put("Bến Tre",                  1621);
        PROVINCE_KM.put("Cần Thơ",                  1649);
        PROVINCE_KM.put("Kiên Giang",               1657);
        PROVINCE_KM.put("Trà Vinh",                 1665);
        PROVINCE_KM.put("Hậu Giang",                1692);
        PROVINCE_KM.put("Sóc Trăng",                1715);
        PROVINCE_KM.put("Bạc Liêu",                 1762);
        PROVINCE_KM.put("Cà Mau",                   1782);
    }

    public Map<String, Integer> getProvinceKmMap() {
        return Collections.unmodifiableMap(PROVINCE_KM);
    }

    public int kmForProvince(String provinceName) {
        if (provinceName == null || provinceName.isBlank()) return -1;
        String trimmed = provinceName.trim();
        if (PROVINCE_KM.containsKey(trimmed)) return PROVINCE_KM.get(trimmed);
        String lower = trimmed.toLowerCase(Locale.ROOT);
        for (var e : PROVINCE_KM.entrySet()) {
            String key = e.getKey().toLowerCase(Locale.ROOT);
            if (lower.contains(key) || key.contains(lower)) return e.getValue();
        }
        return -1;
    }

    public static int kmToDays(int km) {
        if (km <=    0) return 1;
        if (km <=  120) return 2;
        if (km <=  350) return 3;
        if (km <=  700) return 4;
        if (km <= 1100) return 5;
        if (km <= 1500) return 6;
        return 7;
    }

    public static long calcShippingFee(int km) {
        if (km <=    0) return 0L;
        if (km <=  120) return 20_000L;
        if (km <=  350) return 35_000L;
        if (km <=  700) return 55_000L;
        if (km <= 1100) return 75_000L;
        return 95_000L;
    }

    private static String[] daysMeta(int days) {
        return switch (days) {
            case 1  -> new String[]{"⚡",  "Giao nhanh nội thành, 1 ngày làm việc"};
            case 2  -> new String[]{"🚀",  "Giao trong 2 ngày làm việc"};
            case 3  -> new String[]{"🚛",  "Giao trong 3 ngày làm việc"};
            case 4  -> new String[]{"📦",  "Giao trong 4 ngày làm việc"};
            case 5  -> new String[]{"🗺️", "Giao trong 5 ngày làm việc"};
            case 6  -> new String[]{"✈️",  "Giao trong 6 ngày làm việc"};
            default -> new String[]{"🏔️", "Giao trong 7 ngày làm việc"};
        };
    }

    public DeliveryInfo forAddress(String address) {
        String provinceName = extractProvince(address);
        int km = kmForProvince(provinceName);
        int days = km >= 0 ? kmToDays(km) : 5;
        String[] info = daysMeta(days);
        return new DeliveryInfo(days, Math.max(km, 0), info[0], info[1]);
    }

    public static String extractProvince(String address) {
        if (address == null || address.isBlank()) return "";
        String[] parts = address.split(",");
        return parts[parts.length - 1].trim();
    }

    public LocalDate estimatedDate(String address) {
        int businessDays = forAddress(address).days();
        LocalDate d = LocalDate.now();
        int added = 0;
        while (added < businessDays) {
            d = d.plusDays(1);
            if (d.getDayOfWeek() != DayOfWeek.SATURDAY &&
                d.getDayOfWeek() != DayOfWeek.SUNDAY) added++;
        }
        return d;
    }

    /** Xuất JSON bảng km để nhúng vào JS trang checkout */
    public String provinceKmJson() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var e : PROVINCE_KM.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(e.getKey().replace("\"", "\\\"")).append("\":").append(e.getValue());
            first = false;
        }
        return sb.append("}").toString();
    }
}
