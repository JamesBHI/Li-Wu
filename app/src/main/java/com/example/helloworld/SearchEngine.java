package com.example.helloworld;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * 搜索引擎（Bing + 百度 双引擎）
 */
public class SearchEngine {

    public static class SearchResult {
        public String title;
        public String snippet;
        public String url;

        public SearchResult(String title, String snippet, String url) {
            this.title = title;
            this.snippet = snippet;
            this.url = url;
        }
    }

    public static List<SearchResult> search(String query) throws Exception {
        List<SearchResult> allResults = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // 同时从 Bing 和百度搜索
        try {
            List<SearchResult> bingResults = searchBing(query);
            allResults.addAll(bingResults);
        } catch (Exception e) {
            errors.add("Bing: " + e.getMessage());
        }

        try {
            List<SearchResult> baiduResults = searchBaidu(query);
            allResults.addAll(baiduResults);
        } catch (Exception e) {
            errors.add("百度: " + e.getMessage());
        }

        // 去重（按URL去重）
        List<SearchResult> deduped = new ArrayList<>();
        List<String> seenUrls = new ArrayList<>();
        for (SearchResult r : allResults) {
            if (!seenUrls.contains(r.url)) {
                deduped.add(r);
                seenUrls.add(r.url);
            }
        }

        // 只保留前8条
        if (deduped.size() > 8) {
            deduped = deduped.subList(0, 8);
        }

        if (deduped.isEmpty()) {
            throw new Exception("双引擎均搜索失败: " + String.join("; ", errors));
        }

        return deduped;
    }

    // ========== Bing 搜索 ==========

    private static List<SearchResult> searchBing(String query) throws Exception {
        List<SearchResult> results = new ArrayList<>();
        String encoded = URLEncoder.encode(query, "UTF-8");

        URL url = new URL("https://www.bing.com/search?q=" + encoded + "&count=10");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36");
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setInstanceFollowRedirects(true);

        int responseCode = conn.getResponseCode();
        String contentType = conn.getContentType();
        boolean isGbk = contentType != null && contentType.toLowerCase().contains("gbk");
        String charset = isGbk ? "GBK" : "UTF-8";

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), charset));
        StringBuilder html = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) html.append(line).append("\n");
        reader.close();
        conn.disconnect();

        String content = html.toString();

        String algoMarker = "b_algo";
        int idx = 0;
        int count = 0;

        while ((idx = content.indexOf(algoMarker, idx)) != -1 && count < 5) {
            try {
                int liStart = content.lastIndexOf("<li", idx);
                if (liStart == -1 || liStart < idx - 200) liStart = idx;
                int liEnd = content.indexOf("</li>", idx);
                if (liEnd == -1) break;
                String block = content.substring(liStart, liEnd + 5);

                String hrefMarker = "<a href=\"";
                int hrefIdx = block.indexOf(hrefMarker);
                if (hrefIdx == -1) { hrefMarker = "<a href='"; hrefIdx = block.indexOf(hrefMarker); }
                if (hrefIdx != -1) {
                    int hrefStart = hrefIdx + hrefMarker.length();
                    char closeChar = block.charAt(hrefIdx + 8);
                    int hrefEnd = block.indexOf(closeChar == '\'' ? "'" : "\"", hrefStart);
                    if (hrefEnd == -1) hrefEnd = block.indexOf("\"", hrefStart);
                    String resultUrl = block.substring(hrefStart, hrefEnd);

                    if (resultUrl.contains("bing.com") || resultUrl.contains("go.microsoft.com")) {
                        idx = liEnd; continue;
                    }

                    int aEnd = block.indexOf("</a>", hrefEnd);
                    int titleStart = block.indexOf(">", hrefEnd) + 1;
                    String titleBlock = aEnd > titleStart ? block.substring(titleStart, aEnd) : "";
                    String resultTitle = titleBlock.replaceAll("<[^>]+>", "").trim();

                    String snippet = "";
                    int pIdx1 = block.indexOf("b_lineclamp");
                    if (pIdx1 != -1) {
                        int pStart = block.indexOf(">", pIdx1) + 1;
                        int pEnd = block.indexOf("</p>", pStart);
                        if (pStart > 0 && pEnd > pStart && pEnd - pStart < 500) {
                            snippet = block.substring(pStart, pEnd).trim().replaceAll("<[^>]+>", "").trim();
                        }
                    }

                    resultUrl = decodeHtml(resultUrl);
                    snippet = decodeHtml(snippet);
                    resultTitle = decodeHtml(resultTitle);

                    if (!resultTitle.isEmpty() && !resultUrl.isEmpty()) {
                        results.add(new SearchResult(resultTitle, snippet, resultUrl));
                        count++;
                    }
                }
                idx = liEnd + 5;
            } catch (Exception e) { idx++; }
        }

        if (results.isEmpty()) {
            throw new Exception("Bing 无结果 (code=" + responseCode + ", len=" + content.length() + ")");
        }
        return results;
    }

    // ========== 百度搜索 ==========

    private static List<SearchResult> searchBaidu(String query) throws Exception {
        List<SearchResult> results = new ArrayList<>();
        String encoded = URLEncoder.encode(query, "UTF-8");

        URL url = new URL("https://www.baidu.com/s?wd=" + encoded + "&rn=10");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36");
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setInstanceFollowRedirects(true);

        int responseCode = conn.getResponseCode();

        // 百度是 UTF-8
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder html = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) html.append(line).append("\n");
        reader.close();
        conn.disconnect();

        String content = html.toString();

        // 百度搜索结果格式: <div class="result" ...> 或 <div class="c-abstract">
        // 更通用的方法：查找 <h3> 中的 <a href="...">
        String h3Marker = "<h3";
        int idx = 0;
        int count = 0;

        while ((idx = content.indexOf(h3Marker, idx)) != -1 && count < 5) {
            try {
                int h3End = content.indexOf("</h3>", idx);
                if (h3End == -1) break;
                String h3Block = content.substring(idx, h3End + 5);

                // 提取 <a href="...">
                int aHrefIdx = h3Block.indexOf("href=\"");
                if (aHrefIdx == -1) { idx = h3End; continue; }
                int aHrefStart = aHrefIdx + 6;
                int aHrefEnd = h3Block.indexOf("\"", aHrefStart);
                if (aHrefEnd == -1) { idx = h3End; continue; }
                String resultUrl = h3Block.substring(aHrefStart, aHrefEnd);

                // 过滤百度内部链接
                if (resultUrl.contains("baidu.com") && !resultUrl.contains("www.baidu.com/link")) {
                    idx = h3End; continue;
                }
                // 百度搜索结果链接通常是 /link?url=... 或 http
                if (resultUrl.startsWith("/")) {
                    resultUrl = "https://www.baidu.com" + resultUrl;
                }

                // 提取标题
                int aEnd = h3Block.indexOf("</a>", aHrefEnd);
                int titleS = h3Block.indexOf(">", aHrefEnd) + 1;
                // 可能 a 标签被嵌套，找最后一个 > 在 aEnd 之前
                int lastGt = h3Block.lastIndexOf(">", aEnd - 1);
                if (lastGt > aHrefEnd) titleS = lastGt + 1;
                String titleBlock = aEnd > titleS ? h3Block.substring(titleS, aEnd) : "";
                String resultTitle = titleBlock.replaceAll("<[^>]+>", "").trim();

                // 提取摘要 - 在当前 result 块的后续内容中找
                String snippet = "";
                // 查找当前 h3 之后的 div 或 span 摘要
                int afterH3 = h3End + 5;
                // 找 class="c-abstract" 或 class="content-right_..."
                String[] snippetMarkers = {"c-abstract", "content-right_", "c-span-last", "abs"};
                for (String sm : snippetMarkers) {
                    int smIdx = content.indexOf(sm, afterH3);
                    if (smIdx != -1 && smIdx < afterH3 + 1000) {
                        int pStart = content.indexOf(">", smIdx) + 1;
                        // 找对应的标签结束
                        int pEnd1 = content.indexOf("</div>", pStart);
                        int pEnd2 = content.indexOf("</span>", pStart);
                        int pEnd = -1;
                        if (pEnd1 != -1 && pEnd2 != -1) pEnd = Math.min(pEnd1, pEnd2);
                        else if (pEnd1 != -1) pEnd = pEnd1;
                        else pEnd = pEnd2;

                        if (pStart > 0 && pEnd > pStart && pEnd - pStart < 500) {
                            snippet = content.substring(pStart, pEnd).trim().replaceAll("<[^>]+>", "").trim();
                            break;
                        }
                    }
                }

                // 如果还没找到，尝试找原文摘要：class="c-span18"
                if (snippet.isEmpty()) {
                    int spanIdx = content.indexOf("c-span18", afterH3);
                    if (spanIdx != -1 && spanIdx < afterH3 + 800) {
                        int sStart = content.indexOf(">", spanIdx) + 1;
                        int sEnd = content.indexOf("</div>", sStart);
                        if (sStart > 0 && sEnd > sStart && sEnd - sStart < 500) {
                            snippet = content.substring(sStart, sEnd).trim().replaceAll("<[^>]+>", "").trim();
                        }
                    }
                }

                resultUrl = decodeHtml(resultUrl);
                snippet = decodeHtml(snippet);
                resultTitle = decodeHtml(resultTitle);

                if (!resultTitle.isEmpty()) {
                    results.add(new SearchResult(resultTitle, snippet, resultUrl));
                    count++;
                }

                idx = h3End + 5;
            } catch (Exception e) { idx++; }
        }

        if (results.isEmpty()) {
            throw new Exception("百度无结果 (code=" + responseCode + ", len=" + content.length() + ")");
        }
        return results;
    }

    private static String decodeHtml(String text) {
        return text.replace("&amp;", "&")
                   .replace("&lt;", "<")
                   .replace("&gt;", ">")
                   .replace("&quot;", "\"")
                   .replace("&#39;", "'")
                   .replace("&nbsp;", " ");
    }
}
