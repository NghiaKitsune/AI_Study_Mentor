package com.studymentor.app.api;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.UUID;

import okhttp3.Request;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * In-memory stand-in for the backend. Replies to any prompt with a
 * canned step-by-step answer after ~900 ms so the UI flow feels real.
 *
 * Implements {@link AiService} so the rest of the app doesn't care.
 */
public class MockAiService implements AiService {

    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    public Call<ChatResponse> chat(ChatRequest request) {
        return new MockCall(request);
    }

    private class MockCall implements Call<ChatResponse> {
        private final ChatRequest req;
        private boolean cancelled;

        MockCall(ChatRequest req) { this.req = req; }

        @Override public Response<ChatResponse> execute() {
            return Response.success(fakeReply(req));
        }

        @Override public void enqueue(@NonNull Callback<ChatResponse> cb) {
            main.postDelayed(() -> {
                if (cancelled) return;
                cb.onResponse(this, Response.success(fakeReply(req)));
            }, 900);
        }

        @Override public boolean isExecuted() { return false; }
        @Override public void cancel() { cancelled = true; }
        @Override public boolean isCanceled() { return cancelled; }
        @Override public Call<ChatResponse> clone() { return new MockCall(req); }
        @Override public Request request() { return null; }
        @Override public Timeout timeout() { return Timeout.NONE; }
    }

    private static ChatResponse fakeReply(ChatRequest req) {
        ChatResponse r = new ChatResponse();
        r.request_id = req.request_id;
        r.conversation_id = req.conversation_id != null ? req.conversation_id : (long)(Math.random() * 100000);
        r.tokens_used = 142;

        String prompt = req.message == null ? "" : req.message.toLowerCase();

        if (prompt.contains("2x") || prompt.contains("x^2") || prompt.contains("quadratic")) {
            r.reply = "Here's how to solve it step by step.";
            r.final_answer = "x = 1/2  or  x = -3";
            r.steps = new ArrayList<>();
            r.steps.add(step(1, "Identify the form", "This is a quadratic: ax² + bx + c = 0 with a=2, b=5, c=-3."));
            r.steps.add(step(2, "Apply the formula", "x = (-b ± √(b² - 4ac)) / 2a"));
            r.steps.add(step(3, "Plug in values", "x = (-5 ± √(25 + 24)) / 4 = (-5 ± 7) / 4"));
            r.steps.add(step(4, "Solve for both roots", "x = 1/2  or  x = -3"));
        } else if (prompt.contains("photosynthesis")) {
            r.reply = "Photosynthesis is how plants make their own food from sunlight.\n\n" +
                    "The basic idea: leaves take in CO₂, roots pull up water, and chlorophyll captures sunlight " +
                    "to power a chemical reaction that produces glucose (food) and oxygen.";
            r.final_answer = "6 CO₂ + 6 H₂O + light → C₆H₁₂O₆ + 6 O₂";
        } else if (prompt.length() < 20) {
            r.reply = "Tell me a bit more — what subject, and what's the exact problem? I can help with math, " +
                    "science, code, history, languages, and more.";
        } else {
            r.reply = "Great question! Here's the short version:\n\n" +
                    "I'm a mock reply right now. Once you wire the real backend " +
                    "(see HANDOFF.md → Section 4) I'll come from the model.\n\n" +
                    "Your prompt was: \"" + req.message + "\"";
            r.final_answer = null;
        }

        r.follow_ups = new ArrayList<>();
        r.follow_ups.add("Explain simpler");
        r.follow_ups.add("Show another method");
        r.follow_ups.add("Practice 3 problems");

        r.commonMistakes = new ArrayList<>();
        if (prompt.contains("2x") || prompt.contains("x^2") || prompt.contains("quadratic")
                || prompt.contains("equation") || prompt.contains("algebra")) {
            r.commonMistakes.add("Watch out for sign errors — write parentheses around negative terms.");
            r.commonMistakes.add("Tip: substitute your answer back into the original equation to verify.");
        } else if (prompt.contains("photosynthesis") || prompt.contains("biology")
                || prompt.contains("chemistry") || prompt.contains("physics")) {
            r.commonMistakes.add("Don't confuse reactants and products — write the equation direction clearly.");
            r.commonMistakes.add("Remember units: energy is Joules (J), force is Newtons (N), velocity is m/s.");
        } else if (prompt.contains("code") || prompt.contains("program") || prompt.contains("function")
                || prompt.contains("bug") || prompt.contains("algorithm")) {
            r.commonMistakes.add("Off-by-one errors are very common in loops — check your index boundaries.");
            r.commonMistakes.add("Always initialise variables before use to avoid unexpected null/zero values.");
        } else {
            r.commonMistakes.add("Read the question carefully — look for keywords like 'not', 'except', or 'always'.");
            r.commonMistakes.add("Tip: eliminate obviously wrong answers first to improve your odds.");
        }
        return r;
    }

    private static ChatResponse.Step step(int i, String title, String body) {
        ChatResponse.Step s = new ChatResponse.Step();
        s.index = i;
        s.title = title;
        s.body = body;
        return s;
    }
}
