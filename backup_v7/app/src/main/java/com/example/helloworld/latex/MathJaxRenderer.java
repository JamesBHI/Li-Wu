package com.example.helloworld.latex;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

/**
 * LaTeX 渲染器 - 使用 CodeCogs SVG 图片服务
 * 公式 → <img src="https://latex.codecogs.com/svg.latex?公式" />
 */
public class MathJaxRenderer {

    private Context context;
    private Handler mainHandler;

    public MathJaxRenderer(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public boolean containsLatex(String text) {
        if (text == null) return false;
        return text.contains("$$") || text.contains("$") ||
               text.contains("\\(") || text.contains("\\[") ||
               text.contains("\\frac") || text.contains("\\int") ||
               text.contains("\\sum") || text.contains("\\alpha") ||
               text.contains("\\pi") || text.contains("\\theta");
    }

    public WebView render(String latexContent, boolean isUser, final Runnable onHeightReady) {
        final WebView webView = new WebView(context);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBuiltInZoomControls(false);
        webView.getSettings().setDisplayZoomControls(false);
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.getSettings().setTextZoom(100);

        webView.setHorizontalScrollBarEnabled(false);
        webView.setVerticalScrollBarEnabled(false);
        webView.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        webView.setScrollContainer(false);

        // ★ 将 LaTeX 转为 HTML（含 SVG 图片标签） ★
        String htmlContent = convertLatexToHtml(latexContent);

        String html = "<!DOCTYPE html><html><head>" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">" +
                "<style>" +
                "  * { margin: 0; padding: 0; box-sizing: border-box; overflow: hidden !important; }" +
                "  html, body { overflow: hidden !important; width: 100%; }" +
                "  body { font-size: 16px; color: #1C1B1F; font-family: serif; line-height: 2.0; padding: 8px; }" +
                "  img.latex-svg { max-width: 100%; height: auto; }" +
                "  .latex-display { display: block; text-align: center; margin: 10px auto; }" +
                "  .latex-inline { vertical-align: middle; display: inline; }" +
                "</style>" +
                "</head><body>" +
                "<div id='content'>" + htmlContent + "</div>" +
                "<script>" +
                "function rh() {" +
                "  var h = Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);" +
                "  if (h && h > 20) Android.setHeight(Math.round(h));" +
                "}" +
                "window.onload = function() { rh(); setTimeout(rh,500); setTimeout(rh,1000); setTimeout(rh,2000); setTimeout(rh,3000); };" +
                "</script>" +
                "</body></html>";

        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void setHeight(final int h) {
                mainHandler.post(() -> {
                    try {
                        ViewGroup.LayoutParams lp = webView.getLayoutParams();
                        int newH = Math.max(h + 30, 200);
                        if (newH > lp.height) {
                            lp.height = newH;
                            webView.setLayoutParams(lp);
                            if (onHeightReady != null) onHeightReady.run();
                        }
                    } catch (Exception ignored) {}
                });
            }
        }, "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                for (int d = 500; d <= 5000; d += 500) {
                    final int delay = d;
                    mainHandler.postDelayed(() -> {
                        try {
                            view.evaluateJavascript(
                                "Math.round(Math.max(document.body.scrollHeight,document.documentElement.scrollHeight))",
                                value -> {
                                    try {
                                        int h = Integer.parseInt(value.replace("\"","").trim());
                                        if (h > 30) {
                                            ViewGroup.LayoutParams lp = webView.getLayoutParams();
                                            lp.height = Math.max(h + 30, lp.height);
                                            webView.setLayoutParams(lp);
                                        }
                                    } catch (Exception ignored) {}
                                }
                            );
                        } catch (Exception ignored) {}
                    }, d);
                }
            }
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 500);
        params.gravity = isUser ? Gravity.END : Gravity.START;
        params.setMargins(0, 2, 0, 2);
        webView.setLayoutParams(params);
        webView.loadDataWithBaseURL("https://latex.codecogs.com", html, "text/html", "UTF-8", null);

        return webView;
    }

    // ==================== 核心：LaTeX → HTML（含 SVG 图片） ====================

    private String convertLatexToHtml(String text) {
        if (text == null || text.isEmpty()) return "";

        StringBuilder result = new StringBuilder();
        int i = 0;

        while (i < text.length()) {
            // 先在当前位置查找公式
            FormulaMatch match = findNextFormula(text, i);

            if (match == null) {
                // 没有公式了，添加剩余文本
                result.append(escapeHtml(text.substring(i)));
                break;
            }

            // 添加公式前的文本
            if (match.start > i) {
                result.append(escapeHtml(text.substring(i, match.start)));
            }

            // 生成 SVG 图片标签
            String imgTag = makeSvgTag(match.formula, match.isDisplay);
            result.append(imgTag);

            i = match.end;
        }

        return result.toString();
    }

    /**
     * 查找下一个公式（支持格式：$$...$$, $...$, \[...\], \(...\)）
     */
    private FormulaMatch findNextFormula(String text, int from) {
        if (from >= text.length()) return null;

        // 1. 查找 $$...$$（最高优先级）
        int dds = text.indexOf("$$", from);
        if (dds >= 0) {
            int dde = text.indexOf("$$", dds + 2);
            if (dde >= 0) {
                String formula = text.substring(dds + 2, dde).trim();
                if (!formula.isEmpty()) {
                    return new FormulaMatch(dds, dde + 2, formula, true);
                }
            }
        }

        // 2. 查找 \[...\]
        int bls = text.indexOf("\\[", from);
        if (bls >= 0) {
            int ble = text.indexOf("\\]", bls + 2);
            if (ble >= 0) {
                String formula = text.substring(bls + 2, ble).trim();
                if (!formula.isEmpty()) {
                    return new FormulaMatch(bls, ble + 2, formula, true);
                }
            }
        }

        // 3. 查找 \(...\)
        int pls = text.indexOf("\\(", from);
        if (pls >= 0) {
            int ple = text.indexOf("\\)", pls + 2);
            if (ple >= 0) {
                String formula = text.substring(pls + 2, ple).trim();
                if (!formula.isEmpty()) {
                    return new FormulaMatch(pls, ple + 2, formula, false);
                }
            }
        }

        // 4. 查找单个 $...$（排除 $$
        int ss = text.indexOf("$", from);
        if (ss >= 0 && (ss + 1 >= text.length() || text.charAt(ss + 1) != '$')) {
            int se = text.indexOf("$", ss + 1);
            if (se >= 0) {
                String formula = text.substring(ss + 1, se).trim();
                if (!formula.isEmpty()) {
                    return new FormulaMatch(ss, se + 1, formula, false);
                }
            }
        }

        return null;
    }

    /**
     * 生成 CodeCogs SVG 图片标签
     */
    private String makeSvgTag(String formula, boolean isDisplay) {
        // URL 编码
        String encoded = java.net.URLEncoder.encode(formula, java.nio.charset.StandardCharsets.UTF_8);
        encoded = encoded.replace("+", "%20");

        String cls = isDisplay ? "latex-display" : "latex-inline";
        String style = isDisplay ? "display:block;margin:10px auto;" : "vertical-align:middle;margin:2px;";

        return "<img class='latex-svg " + cls + "' src='https://latex.codecogs.com/svg.latex?" +
               encoded + "' alt='" + escapeHtml(formula) + "' style='" + style + "max-width:100%;' />";
    }

    /**
     * 转义 HTML 特殊字符
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("\n", "<br/>");
    }

    /**
     * 公式匹配结果
     */
    private static class FormulaMatch {
        int start;
        int end;
        String formula;
        boolean isDisplay;

        FormulaMatch(int start, int end, String formula, boolean isDisplay) {
            this.start = start;
            this.end = end;
            this.formula = formula;
            this.isDisplay = isDisplay;
        }
    }
}
