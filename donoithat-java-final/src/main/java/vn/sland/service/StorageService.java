package vn.sland.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class StorageService {
    private final Path root;
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg","image/png","image/webp","image/gif");

    public StorageService(@Value("${app.upload-dir:uploads}") String dir) throws IOException {
        root = Paths.get(dir).toAbsolutePath().normalize(); Files.createDirectories(root);
        Files.createDirectories(root.resolve("products")); Files.createDirectories(root.resolve("avatars"));
    }

    public String saveImage(MultipartFile file, String folder, long maxBytes, String prefix) throws IOException {
        if (file == null || file.isEmpty()) return null;
        if (file.getSize() > maxBytes) throw new IllegalArgumentException("Ảnh vượt quá dung lượng cho phép.");
        String type = Optional.ofNullable(file.getContentType()).orElse("").toLowerCase(Locale.ROOT);
        if (!IMAGE_TYPES.contains(type)) throw new IllegalArgumentException("Chỉ chấp nhận JPG, PNG, WEBP hoặc GIF.");
        String original = Optional.ofNullable(file.getOriginalFilename()).orElse("image.jpg");
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')).replaceAll("[^A-Za-z0-9.]","") : ".jpg";
        String name = prefix + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0,8) + ext.toLowerCase(Locale.ROOT);
        Path targetDir = root.resolve(folder).normalize(); Files.createDirectories(targetDir);
        Path target = targetDir.resolve(name).normalize();
        if (!target.startsWith(targetDir)) throw new IOException("Invalid upload path");
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/" + folder + "/" + name;
    }

    public Path root() { return root; }
}
