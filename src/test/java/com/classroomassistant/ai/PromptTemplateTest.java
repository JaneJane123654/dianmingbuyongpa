package com.classroomassistant.ai;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PromptTemplateTest {

    @Test
    void testBuild() {
        PromptTemplate template = new PromptTemplate();
        String prompt = template.build("今天讲了 Java 基础");
        assertTrue(prompt.contains("Java 基础"));
    }
}
