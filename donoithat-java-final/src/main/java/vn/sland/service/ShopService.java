package vn.sland.service;

import org.springframework.stereotype.Service;
import vn.sland.repository.AppRepository;
import java.util.*;

@Service
public class ShopService {
    private final AppRepository repo;
    public ShopService(AppRepository repo){this.repo=repo;}

    public List<Map<String,Object>> featured(){return repo.list("SELECT p.*,COALESCE(SUM(od.quantity),0) total_sold FROM products p LEFT JOIN order_details od ON od.product_id=p.id GROUP BY p.id ORDER BY total_sold DESC,p.id DESC LIMIT 8");}
    public List<Map<String,Object>> saleProducts(){return repo.list("SELECT * FROM products WHERE old_price IS NOT NULL AND old_price>price ORDER BY id DESC");}
    public List<Map<String,Object>> categories(){return repo.list("SELECT * FROM categories ORDER BY name");}
    public Map<String,Object> product(int id){return repo.one("SELECT p.*,c.name category_name FROM products p LEFT JOIN categories c ON c.id=p.category_id WHERE p.id=?",id);}
    public List<Map<String,Object>> related(int id,int category){return repo.list("SELECT * FROM products WHERE category_id=? AND id<>? ORDER BY id DESC LIMIT 4",category,id);}
    public List<Map<String,Object>> reviews(Integer productId,int limit,int offset){
        if(productId==null) return repo.list("SELECT r.*,u.username,u.avatar FROM reviews r JOIN users u ON u.id=r.user_id ORDER BY r.created_at DESC LIMIT ? OFFSET ?",limit,offset);
        return repo.list("SELECT r.*,u.username,u.avatar FROM reviews r JOIN users u ON u.id=r.user_id WHERE r.product_id=? ORDER BY r.created_at DESC LIMIT ? OFFSET ?",productId,limit,offset);
    }
    public List<Map<String,Object>> products(String search,Integer cate,String sort,int limit,int offset){
        StringBuilder sql=new StringBuilder("SELECT p.*,c.name category_name FROM products p LEFT JOIN categories c ON c.id=p.category_id WHERE 1=1");List<Object>a=new ArrayList<>();
        if(search!=null&&!search.isBlank()){sql.append(" AND (p.name LIKE ? OR p.description LIKE ?)");a.add("%"+search.trim()+"%");a.add("%"+search.trim()+"%");}
        if(cate!=null&&cate>0){sql.append(" AND p.category_id=?");a.add(cate);}
        sql.append(switch(sort==null?"":sort){case "price_asc"->" ORDER BY p.price ASC";case "price_desc"->" ORDER BY p.price DESC";case "name"->" ORDER BY p.name ASC";default->" ORDER BY p.id DESC";});
        sql.append(" LIMIT ? OFFSET ?");a.add(limit);a.add(offset);return repo.list(sql.toString(),a.toArray());
    }
}
