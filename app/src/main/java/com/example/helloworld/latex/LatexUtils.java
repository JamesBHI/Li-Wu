package com.example.helloworld.latex;

import android.content.Context;
import android.graphics.Color;
import android.view.ViewGroup;
import android.webkit.WebView;

public class LatexUtils {

    public static WebView createChatWebView(Context context) {
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
        webView.setVerticalScrollBarEnabled(true);

        String html = "<!DOCTYPE html><html><head>" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">" +
                "<script>" +
                "MathJax = {" +
                "  tex: { inlineMath: [['$','$'],['\\\\(','\\\\)']], displayMath: [['$$','$$'],['\\\\[','\\\\]']] }," +
                "  svg: {fontCache: 'global'}," +
                "  options: {enableMenu: false}" +
                "};" +
                "</script>" +
                "<script src='https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-svg.js'></script>" +
                "<style>" +
                "  * { margin:0; padding:0; box-sizing:border-box; }" +
                "  html,body { width:100%; min-height:100%; }" +
                "  body { font-family:-apple-system,'Helvetica Neue',sans-serif; padding:8px 12px; background:#F5FBFF; color:#1C1B1F; }" +
                "  @keyframes msgIn { from{opacity:0;transform:translateY(12px)} to{opacity:1;transform:translateY(0)} }" +
                "  @keyframes fadeIn { from{opacity:0} to{opacity:1} }" +
                "  @keyframes pulseDot { 0%,80%,100%{transform:scale(0.6);opacity:0.4} 40%{transform:scale(1);opacity:1} }" +
                "  @keyframes twinkle { 0%,100%{opacity:1;transform:scale(1)} 50%{opacity:0.4;transform:scale(1.3)} }" +
                "  @keyframes float { 0%,100%{transform:translateY(0)} 50%{transform:translateY(-6px)} }" +
                "  :root {" +
                "    --accent-rgb: 100,181,246;" +
                "    --user-bubble: linear-gradient(135deg,#B3E5FC,#81D4FA);" +
                "    --ai-bubble: #FFFFFF;" +
                "    --ai-shadow: rgba(0,0,0,0.08);" +
                "    --cu-text: #1C1B1F;" +
                "    --ca-text: #1C1B1F;" +
                "    --welcome-title: #444;" +
                "    --welcome-sub: #999;" +
                "    --welcome-tag: #64B5F6;" +
                "    --s-color: #888;" +
                "    --t-color: #BBB;" +
                "    --tk-color: #BBB;" +
                "    --th-color: #999;" +
                "  }" +
                "  .s { font-size:12px; color:var(--s-color); margin-top:14px; animation:fadeIn 0.3s ease; }" +
                "  .su { text-align:right; } .sa { text-align:left; }" +
                "  .c { line-height:1.8; font-size:16px; margin:4px 0; animation:msgIn 0.35s ease-out; color:var(--ca-text); }" +
                "  .cu { text-align:right; padding:8px 14px; background:var(--user-bubble); border-radius:16px 16px 4px 16px; display:inline-block; max-width:92%; margin-left:auto; color:var(--cu-text); }" +
                "  .ca { text-align:left; padding:8px 14px; background:var(--ai-bubble); border-radius:16px 16px 16px 4px; display:inline-block; max-width:92%; margin-right:auto; box-shadow:0 1px 3px var(--ai-shadow); color:var(--ca-text); }" +
                "  .t { font-size:10px; color:var(--t-color); animation:fadeIn 0.5s ease; }" +
                "  .tu { text-align:right; } .ta { text-align:left; }" +
                "  .tk { font-size:9px; color:var(--tk-color); text-align:center; animation:fadeIn 0.5s ease; margin:4px 0; }" +
                "  .th { color:var(--th-color); font-style:italic; }" +
                "  .cb { background:#1E1E1E; border-radius:10px; margin:10px 0; overflow:hidden; border:1px solid #333; animation:msgIn 0.4s ease-out; }" +
                "  .ch { background:#2D2D2D; padding:6px 12px; font-size:10px; color:#AAA; display:flex; justify-content:space-between; }" +
                "  .ch button { background:none; border:none; color:var(--welcome-tag); font-size:10px; cursor:pointer; }" +
                "  pre { margin:0; padding:12px; overflow-x:auto; }" +
                "  code { font-size:11px; color:#D4D4D4; font-family:'Courier New',monospace; line-height:1.6; white-space:pre-wrap; }" +
                "  mjx-container { max-width:100%; display:inline-block; }" +
                "  .cpbtn { background:none; border:none; color:var(--welcome-tag); font-size:11px; cursor:pointer; padding:2px 6px; vertical-align:middle; border-radius:4px; }" +
                "  #welcome { display:flex; align-items:center; justify-content:center; min-height:65vh; text-align:center; padding:20px; }" +
                "  #welcome .w-icon { position:relative; font-size:60px; margin-bottom:8px; display:inline-block; animation:float 3s ease-in-out infinite; }" +
                "  #welcome .w-icon .atom { font-size:60px; }" +
                "  #welcome .w-icon .sparkle { position:absolute; top:-10px; right:-14px; font-size:24px; animation:twinkle 2s ease-in-out infinite; }" +
                "  #welcome .w-title { font-size:20px; color:var(--welcome-title); margin-bottom:8px; font-weight:700; }" +
                "  #welcome .w-sub { font-size:14px; color:var(--welcome-sub); }" +
                "  #welcome .w-tag { font-size:11px; color:var(--welcome-tag); margin-top:14px; letter-spacing:2px; }" +
                "  .thinking-dots { display:inline-flex; align-items:center; gap:4px; margin-left:6px; }" +
                "  .thinking-dots .dot { width:6px; height:6px; background:var(--welcome-tag); border-radius:50%; animation:pulseDot 1.4s ease-in-out infinite; }" +
                "  .thinking-dots .dot:nth-child(2) { animation-delay:0.2s; }" +
                "  .thinking-dots .dot:nth-child(3) { animation-delay:0.4s; }" +
                "  body.dark { background:#1A1A2E; color:#E0E0E0; }" +
                "  body.dark .cb { background:#1A1A1A; border-color:#444; }" +
                "  body.dark .ch { background:#252525; color:#CCC; }" +
                "  body.dark code { color:#D0D0D0; }" +
                "  body.dark mjx-container { color:#E0E0E0; fill:#E0E0E0; }" +
                "</style>" +
                "</head><body>" +
                "<div id='welcome'>" +
                "  <div>" +
                "    <div class='w-icon'><span class='atom'>⚛️</span><span class='sparkle'>✨</span></div>" +
                "    <div class='w-title'>你好，请问有什么可以帮到你？</div>" +
                "    <div class='w-sub'>我是理物，你的物理竞赛 AI 助手</div>" +
                "    <div class='w-tag'>✦ 智慧 · 精准 · 灵动 ✦</div>" +
                "  </div>" +
                "</div>" +
                "<div id='chat'></div>" +
                "<script>" +
                "var aiMsgs=[];" +
                "function setWebTheme(ar,ag,ab,ub,abub,ash,wt,wsub,wtg,cut,cat,sc,tc,tkc,thc){" +
                "  var r=document.documentElement;" +
                "  r.style.setProperty('--accent-rgb',ar+','+ag+','+ab);" +
                "  r.style.setProperty('--user-bubble',ub);" +
                "  r.style.setProperty('--ai-bubble',abub);" +
                "  r.style.setProperty('--ai-shadow',ash);" +
                "  r.style.setProperty('--cu-text',cut);" +
                "  r.style.setProperty('--ca-text',cat);" +
                "  r.style.setProperty('--s-color',sc);" +
                "  r.style.setProperty('--t-color',tc);" +
                "  r.style.setProperty('--tk-color',tkc);" +
                "  r.style.setProperty('--th-color',thc);" +
                "  r.style.setProperty('--welcome-title',wt);" +
                "  r.style.setProperty('--welcome-sub',wsub);" +
                "  r.style.setProperty('--welcome-tag',wtg);" +
                "}" +
                "function copyCode(b){var c=b.parentElement.nextElementSibling.textContent;navigator.clipboard.writeText(c);b.textContent='✅';setTimeout(function(){b.textContent='📋复制';},2000);}" +
                "function copyAi(i){navigator.clipboard.writeText(aiMsgs[i]);}" +
                "function scrollToBottom(){window.scrollTo({top:document.body.scrollHeight,behavior:'smooth'});}" +
                "function showThinking(){var d=document.getElementById('chat');var n=new Date();var t=('0'+n.getHours()).slice(-2)+':'+('0'+n.getMinutes()).slice(-2);var v=document.createElement('div');v.id='thinking';v.innerHTML='<div class=\"s sa\">⚛️ 理物</div><div class=\"c ca th\">⏳ 思考中<span class=\"thinking-dots\"><span class=\"dot\"></span><span class=\"dot\"></span><span class=\"dot\"></span></span></div><div class=\"t ta\">'+t+'</div>';d.appendChild(v);scrollToBottom();}" +
                "function hideThinking(){var e=document.getElementById('thinking');if(e){e.style.opacity='0';e.style.transition='opacity 0.3s';setTimeout(function(){if(e&&e.parentNode)e.remove();},300);}}" +
                "function showWelcome(){var w=document.getElementById('welcome');w.style.display='flex';w.style.opacity='1';}" +
                "function hideWelcome(){var w=document.getElementById('welcome');w.style.display='none';w.style.opacity='0';}" +
                "function setThemeMode(d){if(d)document.body.classList.add('dark');else document.body.classList.remove('dark');MathJax.typesetPromise();}" +
                "function addUserMsg(t){var d=document.getElementById('chat');var n=new Date();var t2=('0'+n.getHours()).slice(-2)+':'+('0'+n.getMinutes()).slice(-2);d.innerHTML+='<div class=\"s su\">🧑 我</div><div class=\"c cu\">'+t+'</div><div class=\"t tu\">'+t2+'</div>';scrollToBottom();MathJax.typesetPromise().then(function(){setTimeout(function(){MathJax.typesetPromise();},300);});}" +
                "function addAiMsg(h,r,ti){hideThinking();var d=document.getElementById('chat');var n=new Date();var t2=('0'+n.getHours()).slice(-2)+':'+('0'+n.getMinutes()).slice(-2);var i=aiMsgs.length;aiMsgs.push(r);d.innerHTML+='<div class=\"s sa\">⚛️ 理物 <button class=\"cpbtn\" onclick=\"copyAi('+i+')\">📋</button></div><div class=\"c ca\">'+h+'</div>';if(ti)d.innerHTML+='<div class=\"tk\">'+ti+'</div>';d.innerHTML+='<div class=\"t ta\">'+t2+'</div>';scrollToBottom();MathJax.typesetPromise().then(function(){setTimeout(function(){MathJax.typesetPromise();},500);});}" +
                "function clearChat(){document.getElementById('chat').innerHTML='';aiMsgs=[];}" +
                "</script>" +
                "</body></html>";

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        webView.setLayoutParams(params);
        webView.loadDataWithBaseURL("https://cdn.jsdelivr.net", html, "text/html", "UTF-8", null);
        return webView;
    }
}
