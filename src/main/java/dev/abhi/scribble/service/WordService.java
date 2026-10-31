package dev.abhi.scribble.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class WordService {
    private final List<String> words;

    public WordService(ObjectMapper objectMapper) {
        try {
            ClassPathResource resource = new ClassPathResource("words.json");
            words = objectMapper.readValue(resource.getInputStream(), new TypeReference<List<String>>() {});
            if (words == null || words.isEmpty()) {
                throw new IllegalStateException("words.json must contain at least one word");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not load words.json", e);
        }
    }

    public String getRandomWord() {
        return words.get(ThreadLocalRandom.current().nextInt(words.size()));
    }
}
