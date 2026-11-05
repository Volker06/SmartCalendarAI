package com.smartcalendar.ai.data.remote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GeminiModels {

    private GeminiModels() {}

    public static class Part {
        public String text;

        public Part(String text) {
            this.text = text;
        }
    }

    public static class Content {
        public String role;
        public List<Part> parts;

        public Content(String role, String text) {
            this.role = role;
            this.parts = Collections.singletonList(new Part(text));
        }
    }

    public static class GenerationConfig {
        public Double temperature = null;
        public String responseMimeType = "application/json";
    }

    public static class Request {
        public Content systemInstruction;
        public List<Content> contents = new ArrayList<>();
        public GenerationConfig generationConfig = new GenerationConfig();

        public static Request of(String systemPrompt, String userMessage) {
            Request r = new Request();
            r.systemInstruction = new Content(null, systemPrompt);
            r.contents.add(new Content("user", userMessage));
            return r;
        }
    }

    public static class Candidate {
        public Content content;
        public String finishReason;
    }

    public static class Response {
        public List<Candidate> candidates;

        public String firstText() {
            if (candidates == null || candidates.isEmpty()) {
                return null;
            }
            Content content = candidates.get(0).content;
            if (content == null || content.parts == null) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (Part part : content.parts) {
                if (part.text != null) {
                    sb.append(part.text);
                }
            }
            return sb.length() == 0 ? null : sb.toString();
        }
    }

    public static class ModelInfo {
        public String name;
        public List<String> supportedGenerationMethods;
    }

    public static class ModelList {
        public List<ModelInfo> models;
    }
}
