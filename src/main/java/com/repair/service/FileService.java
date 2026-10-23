package com.repair.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 图片文件服务：负责图片上传落盘与可访问 URL 生成。
 */
public interface FileService {

    /**
     * 上传单张图片，保存到本地上传目录，返回完整可访问 URL。
     * 校验空文件、扩展名白名单与大小上限。
     */
    String upload(MultipartFile file);
}
