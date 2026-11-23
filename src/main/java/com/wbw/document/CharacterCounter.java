package com.wbw.document;

import java.text.BreakIterator;
import java.util.Locale;

/**
 * 글자 수 계산기.
 * <p>
 * 규칙 (프론트엔드도 동일한 규칙을 사용해야 화면과 저장값이 일치한다):
 * <ul>
 *   <li>화면에 보이는 글자(확장 자소 클러스터) 1개를 1자로 센다. 예) "한", "é"(e+결합부호), "👨‍👩‍👧" 는 모두 1자.</li>
 *   <li>줄바꿈(문단 구분, 강제 줄바꿈)은 어느 쪽에도 세지 않는다.</li>
 *   <li>공백 포함: 띄어쓰기·탭·전각 공백·NBSP 등 공백 문자를 포함한다.</li>
 *   <li>공백 제외: 모든 공백 문자를 제외한다.</li>
 * </ul>
 * 프론트엔드 대응 구현: {@code new Intl.Segmenter(undefined, {granularity: 'grapheme'})}
 */
public final class CharacterCounter {

    private CharacterCounter() {
    }

    public record Counts(int withSpaces, int withoutSpaces) {
    }

    public static Counts count(String text) {
        if (text == null || text.isEmpty()) {
            return new Counts(0, 0);
        }
        BreakIterator it = BreakIterator.getCharacterInstance(Locale.ROOT);
        it.setText(text);
        int with = 0;
        int without = 0;
        int start = it.first();
        for (int end = it.next(); end != BreakIterator.DONE; start = end, end = it.next()) {
            String cluster = text.substring(start, end);
            if (isLineBreak(cluster)) {
                continue;
            }
            with++;
            if (!isWhitespace(cluster)) {
                without++;
            }
        }
        return new Counts(with, without);
    }

    private static boolean isLineBreak(String cluster) {
        return cluster.equals("\n") || cluster.equals("\r\n") || cluster.equals("\r")
                || cluster.equals(" ") || cluster.equals(" ");
    }

    private static boolean isWhitespace(String cluster) {
        return cluster.codePoints().allMatch(cp -> Character.isWhitespace(cp) || Character.isSpaceChar(cp));
    }
}
