package vn.sland.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;import java.net.http.*;import java.time.Duration;import java.util.*;

@Service
public class GeminiService {
    private final ObjectMapper mapper;private final String apiKey,model;private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    public GeminiService(ObjectMapper mapper,@Value("${app.gemini.api-key:}")String apiKey,@Value("${app.gemini.model:gemini-3.1-flash-lite}")String model){this.mapper=mapper;this.apiKey=apiKey==null?"":apiKey.trim();this.model=model;}
    public String reply(String prompt){
        if(apiKey.isBlank()) return fallbackReply(prompt);
        String[] candidateModels = {model, "gemini-3.1-flash-lite", "gemini-2.5-flash-lite"};
        for (String m : candidateModels) {
            try{
                Map<String,Object>b=Map.of("contents",List.of(Map.of("parts",List.of(Map.of("text",prompt)))));
                String url="https://generativelanguage.googleapis.com/v1beta/models/"+m+":generateContent?key="+apiKey;
                HttpRequest r=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(b))).build();
                HttpResponse<String> resp = http.send(r,HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    JsonNode root=mapper.readTree(resp.body());
                    JsonNode t=root.at("/candidates/0/content/parts/0/text");
                    if(!t.isMissingNode() && !t.asText().isBlank()) return t.asText().trim();
                } else {
                    System.err.println("[GeminiService] " + m + " returned " + resp.statusCode() + ", trying next candidate...");
                }
            }catch(Exception e){
                System.err.println("[GeminiService] " + m + " error: " + e.getMessage());
            }
        }
        return fallbackReply(prompt);
    }
    private String fallbackReply(String prompt) {
        String query = prompt;
        int idx = prompt.lastIndexOf("Khách hàng hỏi:");
        if (idx >= 0) {
            query = prompt.substring(idx + "Khách hàng hỏi:".length()).trim();
        }
        String p = query.toLowerCase();
        if(p.contains("sofa")) return "Dạ tại GS LUXURY hiện có mẫu Sofa đơn giản với thiết kế hiện đại, nệm êm ái cao cấp giá 6.500.000đ. Bạn có thể bấm vào sản phẩm trên trang chủ để xem chi tiết hoặc thêm vào giỏ hàng nhé!";
        if(p.contains("bàn làm việc") || p.contains("bàn học") || p.contains("bàn")) return "Dạ GS LUXURY có các mẫu Bàn làm việc cách tân (3.000.000đ) và Bàn làm việc đơn giản (3.200.000đ) bằng gỗ MDF chống ẩm rất tiện dụng. Mời bạn tham khảo tại mục Sản phẩm ạ!";
        if(p.contains("giường")) return "Dạ các mẫu Giường ngủ cách tân (3.000.000đ) và Giường ngủ cao cấp (4.500.000đ) của GS LUXURY có chất liệu gỗ cao cấp, bảo hành lên tới 24 tháng. Mời bạn ghé xem chi tiết sản phẩm!";
        if(p.contains("tủ")) return "Dạ GS LUXURY có mẫu Tủ quần áo cách tân (5.000.000đ) với nhiều ngăn chứa tiện ích. Bạn có thể nhấn 'Thêm vào giỏ hàng' để đặt mua trực tiếp trên website.";
        return "Dạ cảm ơn bạn đã nhắn tin cho Nội Thất GS LUXURY! Bạn có thể xem toàn bộ mẫu nội thất tại danh mục Sản phẩm, thêm vào giỏ hàng để đặt hàng, hoặc liên hệ Hotline 0326976832 để được tư vấn tận tình ạ!";
    }
}
