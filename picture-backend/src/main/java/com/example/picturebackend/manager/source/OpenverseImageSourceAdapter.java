package com.example.picturebackend.manager.source;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
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
        String fetchUrl = API_URL + "?q=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8)
                + "&page_size=" + candidateCount
                + "&mature=false&license=cc0,pdm&size=large&excluded_source=wikimedia";

        HttpResponse response = null;
        try {
            response = HttpRequest.get(fetchUrl)
                    .header("Accept", "application/json")
                    .timeout(20000)
                    .execute();
            if (!response.isOk()) {
                throw new IOException("Openverse 请求失败，HTTP " + response.getStatus());
            }
            return parse(response.body());
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
            if (source.equalsIgnoreCase("wikimedia")) {
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
