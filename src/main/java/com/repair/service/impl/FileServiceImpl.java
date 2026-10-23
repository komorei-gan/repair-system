package com.repair.service.impl;

import com.repair.common.exception.BusinessException;
import com.repair.common.result.ErrorCode;
import com.repair.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 图片上传实现：校验 → 落盘到 uploads/ → 返回可访问 URL。
 * 文件名使用 UUID + 原扩展名，避免同名覆盖与路径穿越。
 */
@Slf4j
@Service
public class FileServiceImpl implements FileService {

    /** 允许的图片扩展名（小写） */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    /** 单张图片大小上限：5MB */
    private static final long MAX_SIZE = 5L * 1024 * 1024;

    /** 上传根目录（application.yml 中 app.upload.dir，运行时归一化为绝对路径） */
    private final Path uploadDir;

    public FileServiceImpl(@Value("${app.upload.dir:./uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择要上传的图片");
        }

        String originalName = file.getOriginalFilename();
        String ext = extension(originalName);
        if (ext == null || !ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持 jpg、jpeg、png、gif、webp 格式的图片");
        }

        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "图片大小不能超过 5MB");
        }

        // 确保上传目录存在（幂等）
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            log.error("创建上传目录失败: {}", uploadDir, e);
            throw new BusinessException(ErrorCode.SERVER_ERROR, "上传目录初始化失败");
        }

        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path target = uploadDir.resolve(fileName);
        try {
            file.transferTo(target);
        } catch (IOException e) {
            log.error("图片保存失败: {}", target, e);
            throw new BusinessException(ErrorCode.SERVER_ERROR, "图片保存失败");
        }

        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/")
                .path(fileName)
                .toUriString();
    }

    /** 取文件名小写扩展名，不含点号；无扩展名返回 null */
    private String extension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return null;
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return null;
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
