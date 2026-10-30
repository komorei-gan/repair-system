package com.repair.controller;

import com.repair.common.result.Result;
import com.repair.service.FileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片上传接口。Controller 只做参数接收与结果返回，业务在 FileService。
 */
@Tag(name = "文件模块")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /** 上传单张图片 → 返回完整可访问 URL（报修人 / 师傅 / 管理员均可用） */
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('REPORTER', 'REPAIRER', 'ADMIN')")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        return Result.success(fileService.upload(file));
    }
}
