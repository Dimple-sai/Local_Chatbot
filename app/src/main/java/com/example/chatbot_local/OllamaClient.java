package com.example.chatbot_local;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OllamaClient {
    public interface Listener {
        void onToken(String snapshot);

        void onComplete();

        void onError(String message);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private int generation;
    private HttpURLConnection connection;

    public void cancel() {
        synchronized (this) {
            generation++;
            disconnect();
        }
    }

    public void stream(String server, String model, List<Message> history, Listener listener) {
        final int run;
        synchronized (this) {
            generation++;
            run = generation;
            disconnect();
        }
        executor.execute(new Runnable() {
            @Override
            public void run() {
                read(run, server, model, history, listener);
            }
        });
    }

    private void read(int run, String server, String model, List<Message> history, Listener listener) {
        HttpURLConnection opened = null;
        try {
            String base = server.endsWith("/") ? server.substring(0, server.length() - 1) : server;
            opened = (HttpURLConnection) new URL(base + "/api/chat").openConnection();
            opened.setConnectTimeout(8000);
            opened.setReadTimeout(0);
            opened.setRequestMethod("POST");
            opened.setDoOutput(true);
            opened.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            opened.setRequestProperty("Accept", "application/x-ndjson");
            synchronized (this) {
                if (run != generation) return;
                connection = opened;
            }

            JSONObject body = new JSONObject();
            body.put("model", model);
            body.put("stream", true);
            body.put("think", false);
            body.put("keep_alive", "30m");
            JSONObject options = new JSONObject();
            options.put("temperature", 0.7);
            options.put("num_ctx", 4096);
            body.put("options", options);
            JSONArray messages = new JSONArray();
            for (int i = 0; i < history.size(); i++) {
                Message message = history.get(i);
                if (message.content == null || message.content.trim().length() == 0) continue;
                JSONObject item = new JSONObject();
                item.put("role", message.role);
                item.put("content", message.content);
                messages.put(item);
            }
            body.put("messages", messages);

            byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
            OutputStream output = opened.getOutputStream();
            output.write(payload);
            output.close();

            int code = opened.getResponseCode();
            InputStream stream = code >= 200 && code < 300 ? opened.getInputStream() : opened.getErrorStream();
            if (stream == null) {
                if (isCurrent(run)) listener.onError("The local model returned an empty response.");
                return;
            }
            if (code < 200 || code >= 300) {
                String error = readAll(stream);
                if (isCurrent(run)) listener.onError(error.length() == 0 ? "The local model rejected the request." : error);
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            StringBuilder full = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!isCurrent(run)) return;
                if (line.trim().length() == 0) continue;
                JSONObject event = new JSONObject(line);
                if (event.has("error")) {
                    listener.onError(event.optString("error"));
                    return;
                }
                JSONObject message = event.optJSONObject("message");
                if (message != null) {
                    String token = message.optString("content", "");
                    if (token.length() > 0) {
                        full.append(token);
                        listener.onToken(full.toString());
                    }
                }
                if (event.optBoolean("done")) break;
            }
            reader.close();
            if (isCurrent(run)) listener.onComplete();
        } catch (Exception error) {
            if (!isCurrent(run)) return;
            String detail = error.getMessage() == null ? "The local model could not be reached." : error.getMessage();
            listener.onError(detail);
        } finally {
            synchronized (this) {
                if (connection == opened) connection = null;
            }
            if (opened != null) opened.disconnect();
        }
    }

    private synchronized boolean isCurrent(int run) {
        return run == generation;
    }

    private void disconnect() {
        if (connection != null) {
            connection.disconnect();
            connection = null;
        }
    }

    private static String readAll(InputStream stream) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder text = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            text.append(line);
        }
        reader.close();
        return text.toString();
    }
}
