package com.smartcalendar.ai.data.remote;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.smartcalendar.ai.BuildConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class AiAssistant {

    public enum ErrorType {
        NO_KEY, INVALID_KEY, MODEL_NOT_FOUND, RATE_LIMIT, OVERLOADED, NETWORK, SERVER, BAD_RESPONSE
    }

    public static class Turn {
        public final String userText;
        public final String modelJson;

        public Turn(String userText, String modelJson) {
            this.userText = userText;
            this.modelJson = modelJson;
        }
    }

    public interface Listener {
        void onSuccess(AssistantResponse response);

        void onError(ErrorType type, int httpCode, String detail);
    }

    private static final String TAG = "AiAssistant";
    private static final int MAX_PARSE_ATTEMPTS = 2;
    private static final int PASSES = 2;
    private static final long PASS_DELAY_MS = 2500;
    private static final String[] BACKUP_MODELS = {
            "gemini-3.5-flash-lite", "gemini-3.5-flash", "gemini-3.6-flash",
            "gemini-flash-lite-latest", "gemini-flash-latest", "gemini-3.1-flash-lite",
            "gemini-3.8-flash", "gemini-2.5-flash", "gemini-2.5-flash-lite"
    };
    private static final int MAX_DISCOVERED = 4;
    private static final String[] EXCLUDED_WORDS = {
            "image", "tts", "live", "audio", "embedding", "robotics", "computer", "exp", "thinking", "native"
    };
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Gson GSON = new Gson();

    private AiAssistant() {}

    public static void ask(String userMessage, List<Turn> history, long nowMillis, Listener listener) {
        if (BuildConfig.GEMINI_API_KEY == null || BuildConfig.GEMINI_API_KEY.trim().isEmpty()) {
            listener.onError(ErrorType.NO_KEY, 0, "");
            return;
        }
        String systemPrompt = PromptBuilder.build(nowMillis, TimeZone.getDefault().getID());
        GeminiModels.Request request = buildRequest(systemPrompt, history, userMessage);

        List<String> models = new ArrayList<>();
        addModel(models, BuildConfig.GEMINI_MODEL);
        for (String backup : BACKUP_MODELS) {
            addModel(models, backup);
        }
        new Session(request, models, listener).attempt(0, 1);
    }

    public static String toJson(AssistantResponse response) {
        return GSON.toJson(response);
    }

    private static void addModel(List<String> list, String model) {
        if (model == null) {
            return;
        }
        String m = model.trim();
        if (!m.isEmpty() && !list.contains(m)) {
            list.add(m);
        }
    }

    private static GeminiModels.Request buildRequest(String systemPrompt, List<Turn> history, String userMessage) {
        GeminiModels.Request request = new GeminiModels.Request();
        request.systemInstruction = new GeminiModels.Content(null, systemPrompt);
        if (history != null) {
            for (Turn turn : history) {
                request.contents.add(new GeminiModels.Content("user", turn.userText));
                request.contents.add(new GeminiModels.Content("model", turn.modelJson));
            }
        }
        request.contents.add(new GeminiModels.Content("user", userMessage));
        return request;
    }

    private static class Session {
        final GeminiModels.Request request;
        final List<String> models;
        final Listener listener;

        final List<String> log = new ArrayList<>();
        final List<String> transientModels = new ArrayList<>();
        int transientCount;
        int rateLimitCount;
        int notFoundCount;
        int badResponseCount;
        int otherCount;
        int lastCode;
        boolean discoveryDone;

        Session(GeminiModels.Request request, List<String> models, Listener listener) {
            this.request = request;
            this.models = models;
            this.listener = listener;
        }

        void attempt(int index, int pass) {
            if (index >= models.size()) {
                endOfPass(pass);
                return;
            }
            if (pass > 1 && !transientModels.contains(models.get(index))) {
                attempt(index + 1, pass);
                return;
            }
            send(index, pass, 1);
        }

        void endOfPass(int pass) {
            if (pass == 1 && !discoveryDone) {
                discoverModels(pass);
                return;
            }
            if (pass < PASSES && !transientModels.isEmpty()) {
                MAIN.postDelayed(() -> attempt(0, pass + 1), PASS_DELAY_MS);
                return;
            }
            fail();
        }

        void send(int index, int pass, int parseTry) {
            String model = models.get(index);
            GeminiClient.getApi()
                    .generate(BuildConfig.GEMINI_API_KEY, model, request)
                    .enqueue(new Callback<GeminiModels.Response>() {
                        @Override
                        public void onResponse(@NonNull Call<GeminiModels.Response> call,
                                               @NonNull Response<GeminiModels.Response> response) {
                            if (response.isSuccessful()) {
                                GeminiModels.Response body = response.body();
                                AssistantResponse parsed = body == null ? null : parse(body.firstText());
                                if (parsed != null) {
                                    listener.onSuccess(parsed);
                                } else if (parseTry < MAX_PARSE_ATTEMPTS) {
                                    send(index, pass, parseTry + 1);
                                } else {
                                    badResponseCount++;
                                    note(pass, model + ": phản hồi không đọc được");
                                    attempt(index + 1, pass);
                                }
                                return;
                            }

                            int code = response.code();
                            String detail = errorDetail(response);
                            Log.w(TAG, "Mô hình " + model + " -> HTTP " + code + ": " + detail);

                            if (mapHttpError(code, detail) == ErrorType.INVALID_KEY) {
                                listener.onError(ErrorType.INVALID_KEY, code, detail);
                                return;
                            }
                            lastCode = code;
                            if (code == 404) {
                                notFoundCount++;
                                note(pass, model + ": không tồn tại (404)");
                            } else if (isTransient(code)) {
                                transientCount++;
                                if (code == 429) {
                                    rateLimitCount++;
                                }
                                if (!transientModels.contains(model)) {
                                    transientModels.add(model);
                                }
                                note(pass, model + ": " + code);
                            } else {
                                otherCount++;
                                note(pass, model + ": " + code + (detail.isEmpty() ? "" : " " + detail));
                            }
                            attempt(index + 1, pass);
                        }

                        @Override
                        public void onFailure(@NonNull Call<GeminiModels.Response> call, @NonNull Throwable t) {
                            Log.w(TAG, "Lỗi mạng: " + t);
                            listener.onError(ErrorType.NETWORK, 0, t.getMessage() == null ? "" : t.getMessage());
                        }
                    });
        }

        void discoverModels(int pass) {
            discoveryDone = true;
            GeminiClient.getApi()
                    .listModels(BuildConfig.GEMINI_API_KEY, 100)
                    .enqueue(new Callback<GeminiModels.ModelList>() {
                        @Override
                        public void onResponse(@NonNull Call<GeminiModels.ModelList> call,
                                               @NonNull Response<GeminiModels.ModelList> response) {
                            int firstNew = models.size();
                            if (response.isSuccessful() && response.body() != null && response.body().models != null) {
                                List<String> found = new ArrayList<>();
                                for (GeminiModels.ModelInfo info : response.body().models) {
                                    String name = chatModelName(info);
                                    if (name != null && !models.contains(name) && !name.toLowerCase().contains("preview")) {
                                        found.add(name);
                                    }
                                }
                                Collections.sort(found, Collections.reverseOrder());
                                for (int i = 0; i < found.size() && i < MAX_DISCOVERED; i++) {
                                    models.add(found.get(i));
                                }
                            }
                            if (models.size() > firstNew) {
                                note(pass, "thử thêm mô hình tự tìm: " + models.subList(firstNew, models.size()));
                                attempt(firstNew, pass);
                            } else {
                                endOfPass(pass);
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<GeminiModels.ModelList> call, @NonNull Throwable t) {
                            endOfPass(pass);
                        }
                    });
        }

        void note(int pass, String text) {
            if (pass == 1) {
                log.add(text);
            }
        }

        void fail() {
            String detail = String.join("; ", log);
            if (transientCount > 0) {
                ErrorType type = rateLimitCount == transientCount ? ErrorType.RATE_LIMIT : ErrorType.OVERLOADED;
                listener.onError(type, lastCode, detail);
            } else if (badResponseCount > 0) {
                listener.onError(ErrorType.BAD_RESPONSE, 0, detail);
            } else if (notFoundCount > 0 && otherCount == 0) {
                listener.onError(ErrorType.MODEL_NOT_FOUND, 404, detail);
            } else {
                listener.onError(ErrorType.SERVER, lastCode, detail);
            }
        }
    }

    private static String chatModelName(GeminiModels.ModelInfo info) {
        if (info == null || info.name == null || info.supportedGenerationMethods == null
                || !info.supportedGenerationMethods.contains("generateContent")) {
            return null;
        }
        String name = info.name.startsWith("models/") ? info.name.substring(7) : info.name;
        String lower = name.toLowerCase();
        if (!lower.contains("flash")) {
            return null;
        }
        for (String word : EXCLUDED_WORDS) {
            if (lower.contains(word)) {
                return null;
            }
        }
        return name;
    }

    private static boolean isTransient(int code) {
        return code == 429 || code == 500 || code == 502 || code == 503 || code == 504;
    }

    private static ErrorType mapHttpError(int code, String detail) {
        String d = detail == null ? "" : detail.toLowerCase();
        switch (code) {
            case 400:
                return d.contains("api key") ? ErrorType.INVALID_KEY : ErrorType.SERVER;
            case 401:
            case 403:
                return ErrorType.INVALID_KEY;
            case 404:
                return ErrorType.MODEL_NOT_FOUND;
            case 429:
                return ErrorType.RATE_LIMIT;
            case 500:
            case 502:
            case 503:
            case 504:
                return ErrorType.OVERLOADED;
            default:
                return ErrorType.SERVER;
        }
    }

    private static String errorDetail(Response<?> response) {
        try {
            if (response.errorBody() == null) {
                return "";
            }
            JsonObject root = JsonParser.parseString(response.errorBody().string()).getAsJsonObject();
            String message = root.getAsJsonObject("error").get("message").getAsString();
            return message.length() > 220 ? message.substring(0, 220) + "…" : message;
        } catch (Exception e) {
            return "";
        }
    }

    static AssistantResponse parse(String text) {
        if (text == null) {
            return null;
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        try {
            AssistantResponse result = GSON.fromJson(text.substring(start, end + 1), AssistantResponse.class);
            if (result == null || result.intent == null) {
                return null;
            }
            switch (result.intent) {
                case "create_event":
                    return result.event != null ? result : null;
                case "query_events":
                    return result.range != null ? result : null;
                case "update_event":
                    return result.target != null && result.changes != null ? result : null;
                case "delete_event":
                    return result.target != null ? result : null;
                case "other":
                    return result;
                default:
                    return null;
            }
        } catch (JsonSyntaxException e) {
            return null;
        }
    }
}
