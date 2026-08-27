package com.example.picturebackend.manager.source;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.example.picturebackend.constant.PictureConstant;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Openverse 图片搜索适配器，固定只取可公开复用且非敏感的常见位图。 */
@Component
public class OpenverseImageSourceAdapter implements ImageSourceAdapter {
    private static final String API_URL = "https://api.openverse.org/v1/images/";
    private static final List<String> ALLOW_FILE_TYPES = Arrays.asList("jpg", "jpeg", "png", "webp");

    @Override
    public List<String> search(String keyword, int candidateCount) throws IOException {
        return search(keyword, candidateCount, PictureConstant.IMAGE_REQUEST_TIMEOUT_MILLIS);
    }

    @Override
    public List<String> search(String keyword, int candidateCount, long timeoutMillis) throws IOException {
        List<String> imageUrlList = new ArrayList<>();
        if (candidateCount <= 0 || timeoutMillis <= 0) {
            return imageUrlList;
        }

        // Openverse 匿名请求每页最多20条，40条候选需要分两页请求。
        int pageSize = Math.min(candidateCount, 20);
        int pageCount = (candidateCount + pageSize - 1) / pageSize;
        int pageTimeoutMillis = (int) Math.max(1L,
                Math.min(PictureConstant.IMAGE_REQUEST_TIMEOUT_MILLIS, timeoutMillis / pageCount));
        for (int page = 1; page <= pageCount; page++) {
            String fetchUrl = buildPageUrl(keyword, candidateCount, page);
            HttpResponse response = null;
            try {
                response = HttpRequest.get(fetchUrl)
                        .header("Accept", "application/json")
                        .timeout(pageTimeoutMillis)
                        .execute();
                if (!response.isOk()) {
                    throw new IOException("Openverse 请求失败，HTTP " + response.getStatus());
                }
                for (String imageUrl : parse(response.body())) {
                    if (!imageUrlList.contains(imageUrl)) {
                        imageUrlList.add(imageUrl);
                    }
                    if (imageUrlList.size() >= candidateCount) {
                        return imageUrlList;
                    }
                }
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException("Openverse 请求失败", e);
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        }
        return imageUrlList;
    }

    static String buildPageUrl(String keyword, int candidateCount, int page) {
        int pageSize = Math.min(candidateCount, 20);
        return API_URL + "?q=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8)
                + "&page=" + page
                + "&page_size=" + pageSize
                + "&mature=false&license=cc0,pdm&size=large&excluded_source=wikimedia";
    }

    static List<String> parse(String json) {
        List<String> imageUrlList = new ArrayList<>();
        JsonArray items = JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("results");
        if (items == null) {
            return imageUrlList;
        }

        for (JsonElement element : items) {
            JsonObject item = element.getAsJsonObject();
            String source = text(item, "source");
            String license = text(item, "license").toLowerCase(Locale.ROOT);
            String fileType = text(item, "filetype").toLowerCase(Locale.ROOT);
            String imageUrl = text(item, "url");
            boolean mature = item.has("mature") && !item.get("mature").isJsonNull()
                    && item.get("mature").getAsBoolean();
            int width = number(item, "width");
            int height = number(item, "height");

            // 再次过滤敏感图片，防止上游接口没有正确执行 mature=false。
            if (mature) {
                continue;
            }
            // 只接收可以公开使用的 CC0 和 PDM 图片。
            if (!license.equals("cc0") && !license.equals("pdm")) {
                continue;
            }
            // 中国大陆上线时不使用 Wikimedia 图片。
            if (source.toLowerCase(Locale.ROOT).contains("wikimedia")) {
                continue;
            }
            if (!ALLOW_FILE_TYPES.contains(fileType)) {
                continue;
            }
            if (width <= 0 || height <= 0 || imageUrl.isBlank()) {
                continue;
            }
            imageUrlList.add(imageUrl);
        }
        return imageUrlList;
    }

    private static String text(JsonObject object, String name) {
        JsonElement value = object.get(name);
        if (value == null || value.isJsonNull()) {
            return "";
        }
        return value.getAsString();
    }

    private static int number(JsonObject object, String name) {
        try {
            return object.get(name).getAsInt();
        } catch (Exception ignored) {
            return 0;
        }
    }
}
