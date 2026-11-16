package com.quitmate.notice.html;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.safety.Safelist;

import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * 공지 서식 본문(HTML) 처리(KAN-174).
 * <ul>
 *   <li>{@link #sanitize}: 허용한 태그·속성만 남긴다. 이미지는 src 없이 data-key(S3 파일 키)만 둔다.</li>
 *   <li>{@link #toPlainText}: 정리된 HTML 의 글자 버전. notice.content 에 넣어 푸시·검색·옛 앱이 쓴다.</li>
 *   <li>{@link #fillImageSrc}: 응답할 때 data-key 로 서명 URL 을 만들어 src 에 채운다.</li>
 * </ul>
 */
public final class NoticeHtmlSanitizer {

    public static final String IMAGE_KEY_ATTR = "data-key";
    /** 공지 이미지 파일 키. 같은 버킷의 다른 파일(배지 등)을 서명하지 못하게 notice/ 아래 파일만 받는다. */
    private static final Pattern IMAGE_KEY = Pattern.compile("^notice/[A-Za-z0-9][A-Za-z0-9._-]{0,200}$");

    private static final Safelist SAFELIST = new Safelist()
            .addTags("h2", "h3", "p", "br", "strong", "b", "em", "ul", "ol", "li",
                    "table", "thead", "tbody", "tr", "th", "td", "a", "img")
            .addAttributes("a", "href")
            .addProtocols("a", "href", "http", "https")
            .addAttributes("img", IMAGE_KEY_ATTR, "alt")
            .addAttributes("th", "colspan", "rowspan")
            .addAttributes("td", "colspan", "rowspan")
            .addEnforcedAttribute("a", "rel", "noopener noreferrer");

    private NoticeHtmlSanitizer() {
    }

    public static String sanitize(String html) {
        Document.OutputSettings output = new Document.OutputSettings().prettyPrint(false);
        String cleaned = Jsoup.clean(html, "", SAFELIST, output);

        Document doc = Jsoup.parseBodyFragment(cleaned);
        doc.outputSettings(output);
        for (Element img : doc.select("img")) {
            if (!IMAGE_KEY.matcher(img.attr(IMAGE_KEY_ATTR)).matches()) {
                img.remove(); // 외부 주소 이미지·키 없는 이미지·notice/ 밖의 키
            }
        }
        return doc.body().html();
    }

    public static boolean hasImage(String cleanHtml) {
        return !Jsoup.parseBodyFragment(cleanHtml).select("img").isEmpty();
    }

    public static String fillImageSrc(String cleanHtml, UnaryOperator<String> signedUrlOf) {
        if (cleanHtml == null) {
            return null;
        }
        Document doc = Jsoup.parseBodyFragment(cleanHtml);
        doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
        for (Element img : doc.select("img[" + IMAGE_KEY_ATTR + "]")) {
            img.attr("src", signedUrlOf.apply(img.attr(IMAGE_KEY_ATTR)));
        }
        return doc.body().html();
    }

    /**
     * 글자 버전. 블록(문단·제목) 사이는 줄바꿈, 목록 항목은 "• ", 표의 행은 칸을 " | "로 잇는다.
     * 링크는 글자가 주소와 다르면 "글자(주소)", 이미지는 뺀다. 빈 줄은 겹치지 않게 한 줄만 둔다.
     */
    public static String toPlainText(String cleanHtml) {
        Document doc = Jsoup.parseBodyFragment(cleanHtml);
        StringBuilder sb = new StringBuilder();
        render(doc.body(), sb);

        StringBuilder result = new StringBuilder();
        boolean lastBlank = true; // 맨 앞 빈 줄 제거
        for (String line : sb.toString().split("\n", -1)) {
            String trimmed = line.replaceAll("[ \\t\\u00A0]+", " ").trim();
            if (trimmed.isEmpty()) {
                if (!lastBlank) {
                    result.append('\n');
                }
                lastBlank = true;
            } else {
                result.append(trimmed).append('\n');
                lastBlank = false;
            }
        }
        return result.toString().trim();
    }

    private static final List<String> BLOCKS = List.of("p", "h2", "h3", "ul", "ol", "table", "thead", "tbody");

    private static void render(Node node, StringBuilder sb) {
        if (node instanceof TextNode text) {
            sb.append(text.text());
            return;
        }
        if (!(node instanceof Element el)) {
            return;
        }
        String tag = el.normalName();
        switch (tag) {
            case "img" -> { }
            case "br" -> sb.append('\n');
            case "li" -> {
                breakLine(sb);
                sb.append("• ");
                renderChildren(el, sb);
                breakLine(sb);
            }
            case "tr" -> {
                breakLine(sb);
                boolean first = true;
                for (Element cell : el.children()) {
                    if (!cell.normalName().equals("td") && !cell.normalName().equals("th")) {
                        continue;
                    }
                    StringBuilder cellText = new StringBuilder();
                    renderChildren(cell, cellText);
                    if (!first) {
                        sb.append(" | ");
                    }
                    sb.append(cellText.toString().replaceAll("\\s+", " ").trim());
                    first = false;
                }
                breakLine(sb);
            }
            case "a" -> {
                StringBuilder label = new StringBuilder();
                renderChildren(el, label);
                String text = label.toString().trim();
                String href = el.attr("href");
                if (href.isEmpty() || text.equals(href)) {
                    sb.append(text);
                } else if (text.isEmpty()) {
                    sb.append(href);
                } else {
                    sb.append(text).append('(').append(href).append(')');
                }
            }
            default -> {
                boolean block = BLOCKS.contains(tag) || tag.equals("body");
                if (block) {
                    breakLine(sb);
                }
                renderChildren(el, sb);
                if (block) {
                    breakLine(sb);
                }
            }
        }
    }

    /** 줄을 바꾼다. 이미 줄 처음이면(맨 앞 포함) 더하지 않아 블록 사이에 빈 줄이 생기지 않는다. */
    private static void breakLine(StringBuilder sb) {
        if (!sb.isEmpty() && sb.charAt(sb.length() - 1) != '\n') {
            sb.append('\n');
        }
    }

    private static void renderChildren(Element el, StringBuilder sb) {
        for (Node child : el.childNodes()) {
            render(child, sb);
        }
    }
}
