package com.wbw.document;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CharacterCounterTest {

    @ParameterizedTest(name = "[{index}] \"{0}\" → 공백 포함 {1}, 공백 제외 {2}")
    @CsvSource(delimiter = '|', quoteCharacter = '"', value = {
            "가나다|3|3",
            "안녕 하세요|6|5",
            "Hello, World!|13|12",
            "\"  \"|2|0",
            "a  b|4|2",
    })
    void countsBasicText(String text, int with, int without) {
        CharacterCounter.Counts c = CharacterCounter.count(text);
        assertThat(c.withSpaces()).isEqualTo(with);
        assertThat(c.withoutSpaces()).isEqualTo(without);
    }

    @Test
    void emptyAndNull() {
        assertThat(CharacterCounter.count("")).isEqualTo(new CharacterCounter.Counts(0, 0));
        assertThat(CharacterCounter.count(null)).isEqualTo(new CharacterCounter.Counts(0, 0));
    }

    @Test
    void lineBreaksAreNotCounted() {
        assertThat(CharacterCounter.count("첫 줄\n둘째 줄\r\n셋째")).isEqualTo(new CharacterCounter.Counts(9, 7));
    }

    @Test
    void graphemeClustersCountAsOne() {
        // 가족 이모지(ZWJ 결합), 국기, 결합 문자, 조합형 한글(첫가끝)
        assertThat(CharacterCounter.count("👨‍👩‍👧").withSpaces()).isEqualTo(1);
        assertThat(CharacterCounter.count("🇰🇷").withSpaces()).isEqualTo(1);
        assertThat(CharacterCounter.count("é").withSpaces()).isEqualTo(1);
        assertThat(CharacterCounter.count("각").withSpaces()).isEqualTo(1);
        assertThat(CharacterCounter.count("👍🏽").withSpaces()).isEqualTo(1);
    }

    @Test
    void allKindsOfSpacesAreExcluded() {
        // 탭, 전각 공백, NBSP
        CharacterCounter.Counts c = CharacterCounter.count("a\tb　c d");
        assertThat(c.withSpaces()).isEqualTo(7);
        assertThat(c.withoutSpaces()).isEqualTo(4);
    }
}
