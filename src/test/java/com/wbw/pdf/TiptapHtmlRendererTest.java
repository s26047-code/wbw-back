package com.wbw.pdf;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TiptapHtmlRendererTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final TiptapHtmlRenderer renderer =
            new TiptapHtmlRenderer(f -> f.equals("Nanum Myeongjo") ? "'Nanum Myeongjo',serif" : null);

    private String render(String json) throws Exception {
        return renderer.renderBody(mapper.readTree(json));
    }

    @Test
    void rendersMarksAndParagraphAttributes() throws Exception {
        String html = render("""
                {"type":"doc","content":[{"type":"paragraph","attrs":{"textAlign":"center","lineHeight":2,"paragraphSpacing":1.5},
                 "content":[
                   {"type":"text","text":"굵게","marks":[{"type":"bold"},{"type":"italic"},{"type":"underline"}]},
                   {"type":"text","text":"글꼴","marks":[{"type":"textStyle","attrs":{"fontFamily":"Nanum Myeongjo","fontSize":"20px","letterSpacing":"0.1em","color":"#333333"}}]}
                 ]}]}
                """);
        assertThat(html).contains("<p style=\"text-align:center;line-height:2;margin-bottom:1.5em;\">");
        assertThat(html).contains("<strong><em><u>굵게</u></em></strong>");
        assertThat(html).contains("font-family:'Nanum Myeongjo',serif;");
        assertThat(html).contains("font-size:20px;letter-spacing:0.1em;color:#333333;");
    }

    @Test
    void escapesTextAndDropsUnsafeStyleValues() throws Exception {
        String html = render("""
                {"type":"doc","content":[{"type":"paragraph","attrs":{"textAlign":"x;background:url(evil)","lineHeight":"1;color:red"},
                 "content":[{"type":"text","text":"<script>alert(1)</script> & \\u0001",
                   "marks":[{"type":"textStyle","attrs":{"fontFamily":"Evil\\"Font","fontSize":"20px;color:red","color":"red;x"}}]}]}]}
                """);
        assertThat(html).contains("&lt;script&gt;alert(1)&lt;/script&gt; &amp; ");
        assertThat(html).doesNotContain("<script").doesNotContain("evil").doesNotContain("color:red").doesNotContain("\u0001");
        assertThat(html).startsWith("<p>");
    }

    @Test
    void emptyParagraphKeepsLineHeight() throws Exception {
        assertThat(render("{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\"}]}")).isEqualTo("<p>&#160;</p>");
    }

    @Test
    void rendersStructuralNodes() throws Exception {
        String html = render("""
                {"type":"doc","content":[
                  {"type":"heading","attrs":{"level":9},"content":[{"type":"text","text":"H"}]},
                  {"type":"orderedList","attrs":{"start":3},"content":[{"type":"listItem","content":[{"type":"paragraph","content":[{"type":"text","text":"a"}]}]}]},
                  {"type":"horizontalRule"},
                  {"type":"paragraph","content":[{"type":"text","text":"x"},{"type":"hardBreak"},{"type":"text","text":"y"}]},
                  {"type":"unknownWidget","content":[{"type":"text","text":"kept"}]}
                ]}
                """);
        assertThat(html).isEqualTo("<h3>H</h3><ol start=\"3\"><li><p>a</p></li></ol><hr/><p>x<br/>y</p>kept");
    }

    @Test
    void pageBreakParagraphStartsNewPage() throws Exception {
        String html = render("""
                {"type":"doc","content":[
                  {"type":"paragraph","attrs":{"pageBreak":true},"content":[{"type":"text","text":"1장 끝"}]},
                  {"type":"paragraph","attrs":{"pageBreak":true}},
                  {"type":"paragraph","attrs":{"pageBreak":"true"},"content":[{"type":"text","text":"2장 끝"}]},
                  {"type":"paragraph","attrs":{"pageBreak":false},"content":[{"type":"text","text":"보통"}]}
                ]}
                """);
        assertThat(html).isEqualTo(
                "<p style=\"page-break-after:always;\">1장 끝</p>"
                        + "<div class=\"page-break\"></div>"
                        + "<p style=\"page-break-after:always;\">2장 끝</p>"
                        + "<p>보통</p>");
    }

    @Test
    void paragraphLineHeightAndSpacingAcceptFrontendFormats() throws Exception {
        // 프론트 기본 형식: lineHeight 숫자(배수), paragraphSpacing 숫자(em)
        assertThat(render("""
                {"type":"doc","content":[{"type":"paragraph","attrs":{"lineHeight":1.6,"paragraphSpacing":0.8},
                 "content":[{"type":"text","text":"a"}]}]}
                """)).isEqualTo("<p style=\"line-height:1.6;margin-bottom:0.8em;\">a</p>");

        assertThat(TiptapHtmlRenderer.lineHeightCss(mapper.readTree("\"1.6\""))).isEqualTo("1.6");
        assertThat(TiptapHtmlRenderer.lineHeightCss(mapper.readTree("\"160%\""))).isEqualTo("1.6");
        assertThat(TiptapHtmlRenderer.lineHeightCss(mapper.readTree("\"28px\""))).isEqualTo("28px");
        assertThat(TiptapHtmlRenderer.lineHeightCss(mapper.readTree("10"))).isNull();
        assertThat(TiptapHtmlRenderer.lineHeightCss(mapper.readTree("\"1;color:red\""))).isNull();

        assertThat(TiptapHtmlRenderer.paragraphSpacingCss(mapper.readTree("0"))).isEqualTo("0em");
        assertThat(TiptapHtmlRenderer.paragraphSpacingCss(mapper.readTree("1.25"))).isEqualTo("1.25em");
        assertThat(TiptapHtmlRenderer.paragraphSpacingCss(mapper.readTree("12"))).isEqualTo("12px");
        assertThat(TiptapHtmlRenderer.paragraphSpacingCss(mapper.readTree("\"9pt\""))).isEqualTo("9pt");
        assertThat(TiptapHtmlRenderer.paragraphSpacingCss(mapper.readTree("-1"))).isNull();
    }

    @Test
    void cssLengthValidation() throws Exception {
        assertThat(TiptapHtmlRenderer.cssLength(mapper.readTree("18"), "px", 6, 96, 6, 72, 0.3, 6)).isEqualTo("18px");
        assertThat(TiptapHtmlRenderer.cssLength(mapper.readTree("\"12pt\""), "px", 6, 96, 6, 72, 0.3, 6)).isEqualTo("12pt");
        assertThat(TiptapHtmlRenderer.cssLength(mapper.readTree("\"500px\""), "px", 6, 96, 6, 72, 0.3, 6)).isNull();
        assertThat(TiptapHtmlRenderer.cssLength(mapper.readTree("\"-0.05em\""), "em", -10, 30, -8, 24, -0.5, 1.5)).isEqualTo("-0.05em");
    }
}
