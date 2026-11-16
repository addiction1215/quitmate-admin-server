package com.quitmate.notice.html;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoticeHtmlSanitizerTest {

    @DisplayName("script, 이벤트 속성, style 을 지운다.")
    @Test
    void removesDangerous() {
        String html = "<p style=\"color:red\" onclick=\"x()\">안녕</p><script>alert(1)</script>"
                + "<img src=\"x\" onerror=\"alert(1)\" data-key=\"notice/a.png\">"
                + "<iframe src=\"http://evil\"></iframe><div class=\"c\">본문</div>";

        String result = NoticeHtmlSanitizer.sanitize(html);

        assertThat(result).doesNotContain("script", "alert", "onclick", "onerror", "style", "iframe", "src=", "<div");
        assertThat(result).contains("<p>안녕</p>", "본문", "data-key=\"notice/a.png\"");
    }

    @DisplayName("javascript: 같은 http/https 가 아닌 링크 주소를 지운다.")
    @Test
    void removesBadLinks() {
        String result = NoticeHtmlSanitizer.sanitize(
                "<a href=\"javascript:alert(1)\">a</a><a href=\"data:text/html,x\">b</a><a href=\"/relative\">c</a>");

        assertThat(result).doesNotContain("javascript", "data:", "href");
    }

    @DisplayName("허용 태그는 남기고, 링크에 rel 을 붙인다.")
    @Test
    void keepsAllowed() {
        String html = "<h2>제목</h2><h3>소제목</h3><p><strong>굵게</strong><b>b</b><em>기울임</em><br></p>"
                + "<ul><li>하나</li></ul><ol><li>둘</li></ol>"
                + "<table><thead><tr><th colspan=\"2\">머리</th></tr></thead><tbody><tr><td rowspan=\"2\">칸</td></tr></tbody></table>"
                + "<a href=\"https://quitmate.app\">링크</a><img data-key=\"notice/a.png\" alt=\"설명\">";

        String result = NoticeHtmlSanitizer.sanitize(html);

        assertThat(result).contains("<h2>제목</h2>", "<h3>소제목</h3>", "<strong>굵게</strong>", "<b>b</b>", "<em>기울임</em>",
                "<ul>", "<ol>", "<li>하나</li>", "<table>", "<thead>", "<tbody>", "colspan=\"2\"", "rowspan=\"2\"",
                "href=\"https://quitmate.app\"", "rel=\"noopener noreferrer\"", "alt=\"설명\"");
    }

    @DisplayName("data-key 가 없거나 notice/ 밖이거나 외부 주소인 이미지는 지운다.")
    @Test
    void removesImagesWithoutValidKey() {
        String result = NoticeHtmlSanitizer.sanitize(
                "<img src=\"https://evil.com/a.png\"><img data-key=\"badge/x.png\"><img data-key=\"notice/../x.png\">"
                        + "<img data-key=\"notice/ok.png\">");

        assertThat(result).doesNotContain("evil", "badge", "..");
        assertThat(result).contains("notice/ok.png");
    }

    @DisplayName("글자 버전: 문단·제목은 줄바꿈, 목록은 '• ', 표 행은 ' | ', 링크는 글자(주소), 이미지는 뺀다.")
    @Test
    void toPlainText() {
        String html = NoticeHtmlSanitizer.sanitize("<h2>업데이트 안내</h2><p>새 기능이 <strong>추가</strong>됐어요.</p>"
                + "<ul><li>다크 모드</li><li>알림 개선</li></ul>"
                + "<table><tr><th>버전</th><th>날짜</th></tr><tr><td>1.2.0</td><td>11/17</td></tr></table>"
                + "<p><a href=\"https://quitmate.app\">자세히</a> <a href=\"https://a.com\">https://a.com</a></p>"
                + "<img data-key=\"notice/a.png\"><p><br></p><p>끝</p>");

        assertThat(NoticeHtmlSanitizer.toPlainText(html)).isEqualTo(String.join("\n",
                "업데이트 안내",
                "새 기능이 추가됐어요.",
                "• 다크 모드",
                "• 알림 개선",
                "버전 | 날짜",
                "1.2.0 | 11/17",
                "자세히(https://quitmate.app) https://a.com",
                "",
                "끝"));
    }

    @DisplayName("data-key 로 서명 URL 을 src 에 채운다.")
    @Test
    void fillImageSrc() {
        String html = NoticeHtmlSanitizer.sanitize("<p>a</p><img data-key=\"notice/a.png\">");

        String result = NoticeHtmlSanitizer.fillImageSrc(html, key -> "https://s3/" + key + "?sig=1");

        assertThat(result).contains("src=\"https://s3/notice/a.png?sig=1\"", "data-key=\"notice/a.png\"");
        assertThat(NoticeHtmlSanitizer.fillImageSrc(null, key -> key)).isNull();
    }
}
