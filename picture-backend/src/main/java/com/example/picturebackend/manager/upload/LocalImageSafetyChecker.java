package com.example.picturebackend.manager.upload;

import com.example.picturebackend.Exception.BusinessException;
import com.example.picturebackend.Exception.ErrorCode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 图片本地安全门。
 *
 * 这里只做不依赖模型的基础检查：来源地址关键词和文件头。
 * 它可以拦住明显的敏感地址、伪装文件和空文件，但不能代替真正的视觉审核模型。
 */
public final class LocalImageSafetyChecker {
    private static final List<String> BLOCKED_WORD_LIST = Arrays.asList(
            "porn", "porno", "nsfw", "nude", "naked", "xxx", "sex", "gore", "violence",
            "色情", "淫秽", "裸体", "裸照", "暴力", "血腥", "成人", "三级片"
    );

    private LocalImageSafetyChecker() {
    }

    /** 先检查 URL 或文件名中是否包含明显敏感关键词。 */
    public static boolean isSourceSafe(String source) {
        if (source == null || source.isBlank()) {
            return false;
        }
        String lowerSource = source.toLowerCase(Locale.ROOT);
        for (String blockedWord : BLOCKED_WORD_LIST) {
            if (lowerSource.contains(blockedWord)) {
                return false;
            }
        }
        return true;
    }

    /** 下载完成后检查文件是否真的是支持的图片格式。 */
    public static void check(Object inputSource, File file) {
        String source = inputSource == null ? "" : inputSource.toString();
        if (!isSourceSafe(source)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片来源包含敏感关键词");
        }
        if (file == null || !file.isFile() || file.length() == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片内容为空");
        }
        if (!isSupportedImage(file)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片内容不是支持的图片格式");
        }
    }

    /** 通过文件头判断格式，避免只看扩展名导致脚本或 HTML 被当成图片上传。 */
    private static boolean isSupportedImage(File file) {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            byte[] header = new byte[12];
            int length = inputStream.read(header);
            if (length >= 3
                    && (header[0] & 0xFF) == 0xFF
                    && (header[1] & 0xFF) == 0xD8
                    && (header[2] & 0xFF) == 0xFF) {
                return true;
            }
            if (length >= 8
                    && (header[0] & 0xFF) == 0x89
                    && header[1] == 'P'
                    && header[2] == 'N'
                    && header[3] == 'G'
                    && (header[4] & 0xFF) == 0x0D
                    && (header[5] & 0xFF) == 0x0A
                    && (header[6] & 0xFF) == 0x1A
                    && (header[7] & 0xFF) == 0x0A) {
                return true;
            }
            return length >= 12
                    && header[0] == 'R'
                    && header[1] == 'I'
                    && header[2] == 'F'
                    && header[3] == 'F'
                    && header[8] == 'W'
                    && header[9] == 'E'
                    && header[10] == 'B'
                    && header[11] == 'P';
        } catch (IOException e) {
            return false;
        }
    }
}
