package com.example.picturebackend.manager.source;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.example.picturebackend.constant.PictureConstant;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Bing 图片网页适配器，作为 Openverse 候选不足时的兜底来源。 */
@Component
public class BingImageSourceAdapter implements ImageSourceAdapter {
    private static final String SEARCH_URL = "https://cn.bing.com/images/async";

    @Override
    public List<String> search(String keyword, int candidateCount) throws IOException {
        return search(keyword, candidateCount, PictureConstant.IMAGE_REQUEST_TIMEOUT_MILLIS);
    }

    @Override
    public List<String> search(String keyword, int candidateCount, long timeoutMillis) throws IOException {
        if (candidateCount <= 0 || timeoutMillis <= 0) {
            return new ArrayList<>();
        }

        String fetchUrl = SEARCH_URL + "?q=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8)
                + "&mmasync=1&adlt=strict";
        Document document = Jsoup.connect(fetchUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131 Safari/537.36")
                .referrer("https://cn.bing.com/")
                .timeout((int) Math.max(1L,
                        Math.min(PictureConstant.IMAGE_REQUEST_TIMEOUT_MILLIS, timeoutMillis)))
                .get();

        String pageText = document.text().toLowerCase(Locale.ROOT);
        if (pageText.contains("captcha")
                || pageText.contains("verify")
                || pageText.contains("安全验证")
                || pageText.contains("访问异常")) {
            throw new IOException("Bing 返回了验证页面");
        }
        return parse(document.html(), candidateCount);
    }

    /** 解析 Bing 页面中 a.iusc 的 m 属性，取原图地址 murl。 */
    static List<String> parse(String html, int candidateCount) {
        List<String> imageUrlList = new ArrayList<>();
        Document document = Jsoup.parse(html);
        Elements items = document.select("a.iusc");
        for (Element item : items) {
            if (imageUrlList.size() >= candidateCount) {
                break;
            }
            String metadataJson = item.attr("m");
            if (metadataJson.isBlank()) {
                continue;
            }
            try {
                JsonObject metadata = JsonParser.parseString(metadataJson).getAsJsonObject();
                JsonElement imageUrlElement = metadata.get("murl");
                if (imageUrlElement == null || imageUrlElement.isJsonNull()) {
                    continue;
                }
                String imageUrl = imageUrlElement.getAsString();
                if ((imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))
                        && !imageUrlList.contains(imageUrl)) {
                    imageUrlList.add(imageUrl);
                }
            } catch (RuntimeException ignored) {
                // 单个结果格式异常时跳过，继续处理下一张图片。
            }
        }
        return imageUrlList;
    }
}
