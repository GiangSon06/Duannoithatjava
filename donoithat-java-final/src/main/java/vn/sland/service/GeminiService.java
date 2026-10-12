package vn.sland.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;import java.net.http.*;import java.time.Duration;import java.util.*;

@Service
public class GeminiService {
    private final ObjectMapper mapper;private final String apiKey,model;private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    public GeminiService(ObjectMapper mapper,@Value("${app.gemini.api-key:}")String apiKey,@Value("${app.gemini.model:gemini-2.0-flash}")String model){this.mapper=mapper;this.apiKey=apiKey==null?"":apiKey.trim();this.model=model;}
    public String reply(String prompt){if(apiKey.isBlank())return "Tính năng tư vấn AI chưa được cấu hình GEMINI_API_KEY.";try{Map<String,Object>b=Map.of("contents",List.of(Map.of("parts",List.of(Map.of("text",prompt)))));String url="https://generativelanguage.googleapis.com/v1beta/models/"+model+":generateContent?key="+apiKey;HttpRequest r=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(25)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(b))).build();JsonNode root=mapper.readTree(http.send(r,HttpResponse.BodyHandlers.ofString()).body());JsonNode t=root.at("/candidates/0/content/parts/0/text");return t.isMissingNode()?"Xin lỗi, tôi chưa thể trả lời lúc này.":t.asText();}catch(Exception e){return "Xin lỗi, tôi đang gặp sự cố. Vui lòng thử lại sau!";}}
}
