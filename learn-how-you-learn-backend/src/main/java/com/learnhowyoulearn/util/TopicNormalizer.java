package com.learnhowyoulearn.util;

import org.springframework.stereotype.Component;

@Component
public class TopicNormalizer {

    public String normalize(String name) {
        if (name == null) return "";
        String result = name.toLowerCase().trim();
        // Remove special characters except alphanumeric and spaces
        result = result.replaceAll("[^a-z0-9 ]", " ");
        // Remove trailing 's' for basic plural handling (arrays → array, trees → tree)
        result = result.replaceAll("\\b(\\w+)s\\b", "$1");
        // Collapse multiple spaces
        result = result.replaceAll("\\s+", " ").trim();
        return result;
    }
}
