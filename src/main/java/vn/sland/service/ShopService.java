package vn.sland.service;

import org.springframework.stereotype.Service;
import vn.sland.repository.AppRepository;
import java.util.*;

@Service
public class ShopService {
    private final AppRepository repo;
    public ShopService(AppRepository repo) { this.repo = repo; }

    public List<Map<String,Object>> featured() {
        return repo.list(
            "SELECT p.*, COALESCE(SUM(od.quantity),0) total_sold " +
            "FROM products p LEFT JOIN order_details od ON od.product_id=p.id " +
            "GROUP BY p.id ORDER BY total_sold DESC, p.id DESC LIMIT 5");
    }

    public List<Map<String,Object>> saleProducts() {
        return repo.list("SELECT * FROM products WHERE old_price IS NOT NULL AND old_price>price ORDER BY id DESC");
    }

    public List<Map<String,Object>> categories() {
        return repo.list("SELECT * FROM categories ORDER BY name");
    }

    public Map<String,Object> product(int id) {
        return repo.one(
            "SELECT p.*, c.name category_name FROM products p " +
            "LEFT JOIN categories c ON c.id=p.category_id WHERE p.id=?", id);
    }

    public List<Map<String,Object>> related(int id, int category) {
        return repo.list(
            "SELECT * FROM products WHERE category_id=? AND id<>? ORDER BY id DESC LIMIT 4",
            category, id);
    }

    public List<Map<String,Object>> reviews(Integer productId, int limit, int offset) {
        if (productId == null)
            return repo.list(
                "SELECT r.*, u.username, u.avatar FROM reviews r " +
                "JOIN users u ON u.id=r.user_id ORDER BY r.created_at DESC LIMIT ? OFFSET ?",
                limit, offset);
        return repo.list(
            "SELECT r.*, u.username, u.avatar FROM reviews r " +
            "JOIN users u ON u.id=r.user_id WHERE r.product_id=? " +
            "ORDER BY r.created_at DESC LIMIT ? OFFSET ?",
            productId, limit, offset);
    }

    public int countReviews(Integer productId) {
        if (productId == null) {
            var r = repo.one("SELECT COUNT(*) cnt FROM reviews");
            return r == null ? 0 : ((Number) r.get("cnt")).intValue();
        }
        var r = repo.one("SELECT COUNT(*) cnt FROM reviews WHERE product_id=?", productId);
        return r == null ? 0 : ((Number) r.get("cnt")).intValue();
    }

    public List<Map<String,Object>> featuredReviews(int limit) {
        return repo.list(
            "SELECT r.*, u.username, u.avatar, p.name product_name, p.id product_id_val " +
            "FROM reviews r " +
            "JOIN users u ON u.id=r.user_id " +
            "LEFT JOIN products p ON p.id=r.product_id " +
            "WHERE r.rating = 5 " +
            "ORDER BY r.created_at DESC LIMIT ?", limit);
    }

    /** Lấy danh sách sản phẩm có filter đầy đủ (danh mục, giá, sort, search, phân trang) */
    public List<Map<String,Object>> products(String search, Integer cate, String price, String sort, int limit, int offset) {
        StringBuilder sql = new StringBuilder(
            "SELECT p.*, c.name category_name FROM products p " +
            "LEFT JOIN categories c ON c.id=p.category_id WHERE 1=1");
        List<Object> args = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND (p.name LIKE ? OR p.description LIKE ?)");
            args.add("%" + search.trim() + "%");
            args.add("%" + search.trim() + "%");
        }
        if (cate != null && cate > 0) {
            sql.append(" AND p.category_id=?");
            args.add(cate);
        }
        if (price != null) {
            switch (price) {
                case "duoi-2-trieu"    -> { sql.append(" AND p.price < 2000000");  }
                case "2-den-5-trieu"   -> { sql.append(" AND p.price BETWEEN 2000000 AND 5000000"); }
                case "5-den-10-trieu"  -> { sql.append(" AND p.price BETWEEN 5000000 AND 10000000"); }
                case "tren-10-trieu"   -> { sql.append(" AND p.price > 10000000"); }
            }
        }
        sql.append(switch (sort == null ? "" : sort) {
            case "price_asc"  -> " ORDER BY p.price ASC";
            case "price_desc" -> " ORDER BY p.price DESC";
            case "name_asc"   -> " ORDER BY p.name ASC";
            default           -> " ORDER BY p.id DESC";
        });
        sql.append(" LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return repo.list(sql.toString(), args.toArray());
    }

    /** Đếm tổng số sản phẩm thỏa filter (để tính phân trang) */
    public int countProducts(String search, Integer cate, String price) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) cnt FROM products WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (search != null && !search.isBlank()) {
            sql.append(" AND (name LIKE ? OR description LIKE ?)");
            args.add("%" + search.trim() + "%");
            args.add("%" + search.trim() + "%");
        }
        if (cate != null && cate > 0) {
            sql.append(" AND category_id=?");
            args.add(cate);
        }
        if (price != null) {
            switch (price) {
                case "duoi-2-trieu"   -> sql.append(" AND price < 2000000");
                case "2-den-5-trieu"  -> sql.append(" AND price BETWEEN 2000000 AND 5000000");
                case "5-den-10-trieu" -> sql.append(" AND price BETWEEN 5000000 AND 10000000");
                case "tren-10-trieu"  -> sql.append(" AND price > 10000000");
            }
        }
        var row = repo.one(sql.toString(), args.toArray());
        return row == null ? 0 : ((Number) row.get("cnt")).intValue();
    }
}
