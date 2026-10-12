package vn.sland.util;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;

public final class WebUtil {
    private WebUtil() {}

    public static boolean logged(HttpSession s) { return s != null && s.getAttribute("user_id") != null; }
    public static boolean admin(HttpSession s) { return logged(s) && "admin".equals(String.valueOf(s.getAttribute("role"))); }

    @SuppressWarnings("unchecked")
    public static List<Map<String,Object>> cart(HttpSession s) {
        Object v = s.getAttribute("cart");
        if (v instanceof List<?>) return (List<Map<String,Object>>) v;
        List<Map<String,Object>> cart = new ArrayList<>();
        s.setAttribute("cart", cart);
        return cart;
    }

    public static int intv(Object x) {
        if (x == null) return 0;
        if (x instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(x)); } catch (Exception e) { return 0; }
    }

    public static long longv(Object x) {
        if (x == null) return 0L;
        if (x instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(x)); } catch (Exception e) { return 0L; }
    }

    public static double dbl(Object x) {
        if (x == null) return 0d;
        if (x instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(x)); } catch (Exception e) { return 0d; }
    }

    public static BigDecimal moneyValue(Object x) {
        if (x instanceof BigDecimal b) return b;
        return BigDecimal.valueOf(dbl(x));
    }

    public static String money(Object x) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        nf.setMaximumFractionDigits(0);
        return nf.format(dbl(x)) + "đ";
    }

    public static String cleanRef(String ref) {
        return ref == null ? "" : ref.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    public static String safe(String s) { return s == null ? "" : s.trim(); }
}
