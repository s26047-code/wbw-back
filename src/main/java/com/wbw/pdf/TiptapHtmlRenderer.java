package com.wbw.pdf;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TipTap JSON → XHTML 변환기 (PDF 생성용).
 * <p>
 * 허용된 노드/마크/속성만 출력하고 모든 텍스트와 속성 값을 이스케이프/검증한다.
 * 알 수 없는 노드는 태그 없이 자식만 출력한다.
 * <p>
 * 지원 서식 (프론트엔드 TipTap 속성 이름과 일치해야 함):
 * <ul>
 *   <li>노드: paragraph, heading(level 1~3), blockquote, bulletList, orderedList, listItem, codeBlock, horizontalRule, hardBreak</li>
 *   <li>문단 속성: textAlign(left|center|right|justify), lineHeight(숫자, 배수), paragraphSpacing(숫자, em, 문단 아래 간격)</li>
 *   <li>마크: bold, italic, underline, strike, textStyle(fontFamily, fontSize, letterSpacing, color)</li>
 * </ul>
 */
public class TiptapHtmlRenderer {

    private static final Pattern NUMBER_UNIT = Pattern.compile("^\\s*(-?\\d+(?:\\.\\d+)?)\\s*(px|pt|em|rem)?\\s*$");
    private static final Pattern HEX_COLOR = Pattern.compile("^#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6})$");

    private final Function<String, String> fontFamilyResolver;

    /**
     * @param fontFamilyResolver 글꼴 이름 → CSS font-family 값(대체 글꼴 포함). 지원하지 않는 글꼴이면 null.
     */
    public TiptapHtmlRenderer(Function<String, String> fontFamilyResolver) {
        this.fontFamilyResolver = fontFamilyResolver;
    }

    public String renderBody(JsonNode doc) {
        StringBuilder sb = new StringBuilder();
        renderChildren(doc, sb);
        return sb.toString();
    }

    private void renderChildren(JsonNode node, StringBuilder sb) {
        JsonNode children = node.get("content");
        if (children != null && children.isArray()) {
            for (JsonNode child : children) {
                render(child, sb);
            }
        }
    }

    private void render(JsonNode node, StringBuilder sb) {
        if (node == null || !node.isObject()) {
            return;
        }
        String type = node.path("type").asText("");
        JsonNode attrs = node.path("attrs");
        switch (type) {
            case "text" -> renderText(node, sb);
            case "hardBreak" -> sb.append("<br/>");
            case "paragraph" -> block("p", paragraphStyle(attrs), node, sb, true);
            case "heading" -> {
                int level = Math.max(1, Math.min(3, attrs.path("level").asInt(1)));
                block("h" + level, paragraphStyle(attrs), node, sb, true);
            }
            case "blockquote" -> block("blockquote", "", node, sb, false);
            case "bulletList" -> block("ul", "", node, sb, false);
            case "orderedList" -> {
                int start = Math.max(1, attrs.path("start").asInt(1));
                sb.append("<ol start=\"").append(start).append("\">");
                renderChildren(node, sb);
                sb.append("</ol>");
            }
            case "listItem" -> block("li", "", node, sb, false);
            case "codeBlock" -> block("pre", "", node, sb, false);
            case "horizontalRule" -> sb.append("<hr/>");
            default -> renderChildren(node, sb);
        }
    }

    private void block(String tag, String style, JsonNode node, StringBuilder sb, boolean keepEmptyLine) {
        sb.append('<').append(tag);
        if (!style.isEmpty()) {
            sb.append(" style=\"").append(escapeAttr(style)).append('"');
        }
        sb.append('>');
        int before = sb.length();
        renderChildren(node, sb);
        if (keepEmptyLine && sb.length() == before) {
            sb.append("&#160;"); // 빈 문단도 한 줄 높이를 유지
        }
        sb.append("</").append(tag).append('>');
    }

    private String paragraphStyle(JsonNode attrs) {
        StringBuilder style = new StringBuilder();
        String align = attrs.path("textAlign").asText("");
        if (align.equals("left") || align.equals("center") || align.equals("right") || align.equals("justify")) {
            style.append("text-align:").append(align).append(';');
        }
        Double lineHeight = number(attrs.get("lineHeight"), 0.8, 4.0);
        if (lineHeight != null) {
            style.append("line-height:").append(fmt(lineHeight)).append(';');
        }
        Double spacing = number(attrs.get("paragraphSpacing"), 0.0, 5.0);
        if (spacing != null) {
            style.append("margin-bottom:").append(fmt(spacing)).append("em;");
        }
        return style.toString();
    }

    private void renderText(JsonNode node, StringBuilder sb) {
        String text = node.path("text").asText("");
        if (text.isEmpty()) {
            return;
        }
        StringBuilder open = new StringBuilder();
        StringBuilder close = new StringBuilder();
        JsonNode marks = node.get("marks");
        if (marks != null && marks.isArray()) {
            for (JsonNode mark : marks) {
                String markType = mark.path("type").asText("");
                String tag = switch (markType) {
                    case "bold" -> "strong";
                    case "italic" -> "em";
                    case "underline" -> "u";
                    case "strike" -> "s";
                    default -> null;
                };
                if (tag != null) {
                    open.append('<').append(tag).append('>');
                    close.insert(0, "</" + tag + ">");
                } else if (markType.equals("textStyle")) {
                    String style = textStyle(mark.path("attrs"));
                    if (!style.isEmpty()) {
                        open.append("<span style=\"").append(escapeAttr(style)).append("\">");
                        close.insert(0, "</span>");
                    }
                }
            }
        }
        sb.append(open).append(escapeText(text)).append(close);
    }

    private String textStyle(JsonNode attrs) {
        StringBuilder style = new StringBuilder();
        String family = attrs.path("fontFamily").asText(null);
        if (family != null) {
            String css = fontFamilyResolver.apply(family);
            if (css != null) {
                style.append("font-family:").append(css).append(';');
            }
        }
        String fontSize = cssLength(attrs.get("fontSize"), "px", 6, 96, 6, 72, 0.3, 6);
        if (fontSize != null) {
            style.append("font-size:").append(fontSize).append(';');
        }
        String spacing = cssLength(attrs.get("letterSpacing"), "em", -10, 30, -8, 24, -0.5, 1.5);
        if (spacing != null) {
            style.append("letter-spacing:").append(spacing).append(';');
        }
        String color = attrs.path("color").asText("");
        if (HEX_COLOR.matcher(color).matches()) {
            style.append("color:").append(color).append(';');
        }
        return style.toString();
    }

    /**
     * "17px", "12pt", "0.05em", 17 등의 값을 검증하여 CSS 길이로 만든다. 단위가 없으면 defaultUnit.
     * 단위별 허용 범위를 벗어나면 null.
     */
    static String cssLength(JsonNode value, String defaultUnit,
                            double pxMin, double pxMax, double ptMin, double ptMax, double emMin, double emMax) {
        if (value == null || value.isNull()) {
            return null;
        }
        String raw = value.isNumber() ? value.asText() : value.asText("");
        Matcher m = NUMBER_UNIT.matcher(raw.toLowerCase(Locale.ROOT));
        if (!m.matches()) {
            return null;
        }
        double n = Double.parseDouble(m.group(1));
        String unit = m.group(2) == null ? defaultUnit : m.group(2);
        if (unit.equals("rem")) {
            unit = "em";
        }
        boolean ok = switch (unit) {
            case "px" -> n >= pxMin && n <= pxMax;
            case "pt" -> n >= ptMin && n <= ptMax;
            case "em" -> n >= emMin && n <= emMax;
            default -> false;
        };
        return ok ? fmt(n) + unit : null;
    }

    static Double number(JsonNode value, double min, double max) {
        if (value == null || value.isNull()) {
            return null;
        }
        double n;
        if (value.isNumber()) {
            n = value.asDouble();
        } else {
            Matcher m = NUMBER_UNIT.matcher(value.asText(""));
            if (!m.matches() || (m.group(2) != null && !m.group(2).equals("em"))) {
                return null;
            }
            n = Double.parseDouble(m.group(1));
        }
        return n >= min && n <= max ? n : null;
    }

    static String fmt(double n) {
        if (n == Math.rint(n)) {
            return String.valueOf((long) n);
        }
        return String.format(Locale.ROOT, "%.3f", n).replaceAll("0+$", "");
    }

    public static String escapeText(String s) {
        StringBuilder out = new StringBuilder(s.length());
        s.codePoints().forEach(cp -> {
            switch (cp) {
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '&' -> out.append("&amp;");
                case '\t' -> out.append("&#160;&#160;&#160;&#160;");
                case '\n' -> out.append("<br/>");
                default -> {
                    // XML 1.0에서 허용되지 않는 제어 문자 제거
                    boolean validXml = cp >= 0x20 && !(cp >= 0xD800 && cp <= 0xDFFF) && cp != 0xFFFE && cp != 0xFFFF;
                    if (validXml) {
                        out.appendCodePoint(cp);
                    }
                }
            }
        });
        return out.toString();
    }

    public static String escapeAttr(String s) {
        return s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
