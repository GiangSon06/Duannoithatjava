package vn.sland.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    private final JavaMailSender sender;
    private final String from;
    private final String fromName;

    public EmailService(JavaMailSender sender,
                        @Value("${app.mail.from:}") String from,
                        @Value("${app.mail.from-name:GS LUXURY}") String fromName) {
        this.sender = sender; this.from = from; this.fromName = fromName;
    }

    public boolean configured() { return from != null && !from.isBlank(); }

    public boolean sendHtml(String to, String subject, String html) {
        if (!configured() || to == null || to.isBlank()) {
            System.err.println("[EmailService] Cannot send: from=" + from + ", to=" + to);
            return false;
        }
        try {
            MimeMessage msg = sender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setFrom(from, fromName); h.setTo(to); h.setSubject(subject); h.setText(html, true);
            sender.send(msg);
            System.out.println("[EmailService] ✅ Email sent successfully to " + to + " (Subject: " + subject + ")");
            return true;
        } catch (Exception e) {
            System.err.println("[EmailService] ❌ Gửi email thất bại tới " + to + ": " + e.getMessage());
            return false;
        }
    }

    public boolean sendText(String to, String subject, String body) {
        if (!configured()) {
            System.err.println("[EmailService] Cannot send text: not configured");
            return false;
        }
        try {
            SimpleMailMessage m = new SimpleMailMessage(); m.setFrom(from); m.setTo(to); m.setSubject(subject); m.setText(body); sender.send(m);
            System.out.println("[EmailService] ✅ Text email sent successfully to " + to);
            return true;
        } catch (Exception e) {
            System.err.println("[EmailService] ❌ Gửi email text thất bại: " + e.getMessage());
            return false;
        }
    }
}
