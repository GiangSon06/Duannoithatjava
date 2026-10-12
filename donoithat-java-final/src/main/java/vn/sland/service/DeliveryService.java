package vn.sland.service;

import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service
public class DeliveryService {
    public record DeliveryInfo(int days,int km,String emoji,String note){}
    private static final Map<String,DeliveryInfo> ZONES=new LinkedHashMap<>();
    static {
        add(new DeliveryInfo(2,0,"🚚","Nội thành Hà Nội"),"hà nội","ha noi");
        add(new DeliveryInfo(3,60,"🚚","Khu vực miền Bắc gần Hà Nội"),"bắc ninh","bac ninh","hưng yên","hung yen","hải dương","hai duong","vĩnh phúc","vinh phuc","hà nam","ha nam");
        add(new DeliveryInfo(4,120,"🚛","Khu vực miền Bắc"),"hải phòng","hai phong","nam định","nam dinh","ninh bình","ninh binh","thái bình","thai binh","phú thọ","phu tho","thái nguyên","thai nguyen","hòa bình","hoa binh","quảng ninh","quang ninh");
        add(new DeliveryInfo(5,350,"🚛","Khu vực Bắc Trung Bộ"),"thanh hóa","thanh hoa","nghệ an","nghe an","hà tĩnh","ha tinh");
        add(new DeliveryInfo(6,700,"🚛","Khu vực miền Trung"),"quảng bình","quang binh","quảng trị","quang tri","huế","hue","đà nẵng","da nang","quảng nam","quang nam");
        add(new DeliveryInfo(7,1100,"📦","Khu vực Nam Trung Bộ / Tây Nguyên"),"quảng ngãi","quang ngai","bình định","binh dinh","phú yên","phu yen","khánh hòa","khanh hoa","gia lai","đắk lắk","dak lak","lâm đồng","lam dong");
        add(new DeliveryInfo(8,1700,"📦","Khu vực miền Nam"),"hồ chí minh","ho chi minh","tp hcm","bình dương","binh duong","đồng nai","dong nai","long an","cần thơ","can tho","vũng tàu","vung tau");
    }
    private static void add(DeliveryInfo d,String...names){for(String n:names)ZONES.put(n,d);}
    public DeliveryInfo forAddress(String address){String s=Optional.ofNullable(address).orElse("").toLowerCase(Locale.ROOT);for(var e:ZONES.entrySet())if(s.contains(e.getKey()))return e.getValue();return new DeliveryInfo(8,1500,"📦","Giao hàng toàn quốc");}
    public LocalDate estimatedDate(String address){int business=forAddress(address).days();LocalDate d=LocalDate.now();while(business>0){d=d.plusDays(1);if(d.getDayOfWeek()!=DayOfWeek.SUNDAY)business--;}return d;}
}
