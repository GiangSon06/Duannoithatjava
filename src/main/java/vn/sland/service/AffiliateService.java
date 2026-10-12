package vn.sland.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sland.repository.AppRepository;
import vn.sland.util.WebUtil;
import java.util.*;

@Service
public class AffiliateService {
    private final AppRepository repo;
    public AffiliateService(AppRepository repo){this.repo=repo;}

    public Map<String,Object> byUser(long uid){return repo.one("SELECT a.*,u.username,u.email FROM affiliates a JOIN users u ON u.id=a.user_id WHERE a.user_id=?",uid);}
    public String register(long uid,String bank,String account,String owner){
        if(byUser(uid)!=null)return "Bạn đã đăng ký cộng tác viên.";
        String cleanOwner = WebUtil.stripAccents(owner);
        String ref;do{ref="SL"+Long.toString(System.nanoTime(),36).toUpperCase(Locale.ROOT).substring(0,6);}while(repo.one("SELECT id FROM affiliates WHERE ref_code=?",ref)!=null);
        repo.update("INSERT INTO affiliates(user_id,ref_code,bank_name,bank_account,bank_owner,status,created_at) VALUES(?,?,?,?,?,'pending',NOW())",uid,ref,bank.trim(),account.trim(),cleanOwner);return null;
    }
    public List<Map<String,Object>> commissions(long affId){return repo.list("SELECT ac.*,o.fullname buyer_name FROM affiliate_commissions ac LEFT JOIN orders o ON o.id=ac.order_id WHERE ac.affiliate_id=? ORDER BY ac.created_at DESC",affId);}
    public List<Map<String,Object>> withdrawals(long affId){return repo.list("SELECT * FROM affiliate_withdrawals WHERE affiliate_id=? ORDER BY created_at DESC",affId);}
    @Transactional public String requestWithdrawal(long uid,double amount,String bank,String account,String owner,String note){
        Map<String,Object>a=byUser(uid);if(a==null||!"active".equals(a.get("status")))return "Tài khoản CTV chưa hoạt động.";double balance=WebUtil.dbl(a.get("balance"));if(amount<50000)return "Số tiền rút tối thiểu là 50.000đ.";if(amount>balance)return "Số dư không đủ.";
        String cleanOwner = WebUtil.stripAccents(owner);
        repo.update("INSERT INTO affiliate_withdrawals(affiliate_id,amount,bank_name,bank_account,bank_owner,note,status,created_at) VALUES(?,?,?,?,?,?,'pending',NOW())",a.get("id"),amount,bank.trim(),account.trim(),cleanOwner,note);
        repo.update("UPDATE affiliates SET balance=balance-?,bank_name=?,bank_account=?,bank_owner=? WHERE id=?",amount,bank.trim(),account.trim(),cleanOwner,a.get("id"));return null;
    }
    public void recordOrderCommission(long buyerId,long orderId,double total,String sessionRef){
        String ref=WebUtil.cleanRef(sessionRef);if(ref.isBlank()){Map<String,Object>u=repo.one("SELECT referred_by FROM users WHERE id=?",buyerId);if(u!=null)ref=WebUtil.cleanRef(String.valueOf(u.get("referred_by")));}if(ref.isBlank())return;
        Map<String,Object>a=repo.one("SELECT id,commission_rate FROM affiliates WHERE ref_code=? AND status='active'",ref);if(a==null)return;double rate=WebUtil.dbl(a.get("commission_rate")),amt=Math.round(total*rate/100d);
        repo.update("INSERT INTO affiliate_commissions(affiliate_id,order_id,buyer_id,order_amount,commission_rate,commission_amt,type,status,note,created_at) VALUES(?,?,?,?,?,?,'order','pending','Hoa hồng đơn hàng qua link affiliate',NOW())",a.get("id"),orderId,buyerId,total,rate,amt);repo.update("UPDATE orders SET ref_code=? WHERE id=?",ref,orderId);
    }
}
