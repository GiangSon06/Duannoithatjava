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
                        @Value("${app.mail.from-name:SLAND Shop}") String fromName) {
        this.sender = sender; this.from = from; this.fromName = fromName;
    }

    public boolean configured() { return from != null && !from.isBlank(); }

    public boolean sendHtml(String to, String subject, String html) {
        if (!configured() || to == null || to.isBlank()) return false;
        try {
            MimeMessage msg = sender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setFrom(from, fromName); h.setTo(to); h.setSubject(subject); h.setText(html, true);
            sender.send(msg); return true;
        } catch (Exception e) { return false; }
    }

    public boolean sendText(String to, String subject, String body) {
        if (!configured()) return false;
        try {
            SimpleMailMessage m = new SimpleMailMessage(); m.setFrom(from); m.setTo(to); m.setSubject(subject); m.setText(body); sender.send(m); return true;
        } catch (Exception e) { return false; }
    }
}
