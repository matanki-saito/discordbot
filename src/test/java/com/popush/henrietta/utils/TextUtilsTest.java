package com.popush.henrietta.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class TextUtilsTest {

    @Test
    void splitMessage() {
        var text = "###ZZZZZZZZZZ\nZZZZZZZZZZZZZZZZZZZZ###bbbb###ccccc###dddddd";
        var texts = TextUtils.splitMessage(text, 18, "###");
        Assertions.assertEquals(texts, List.of("###ZZZZZZZZZZ", "ZZZZZZZZZZZZZZZZZZZZ", "###bbbb###ccccc", "###dddddd"));
    }
}