package com.popush.henrietta.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class TextUtilsTest {

    @Test
    void splitMessage() {
        var text = "###aaaaa###bbbb###ccccccccc###dddddd";
        var texts = TextUtils.splitMessage(text, 18, "###");
        Assertions.assertEquals(texts, List.of("###aaaaa###bbbb", "###ccccccccc", "###dddddd"));
    }
}