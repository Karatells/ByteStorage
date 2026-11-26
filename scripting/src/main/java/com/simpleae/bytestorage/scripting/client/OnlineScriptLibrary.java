package com.simpleae.bytestorage.scripting.client;

import com.simpleae.bytestorage.scripting.SimpleAeScripting;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

/**
 * Асинхронный клиентский загрузчик онлайн-библиотеки скриптов и JS-библиотек с GitHub.
 * - Работает только по HTTPS, без редиректов, с таймаутом 6 сек и жёсткими лимитами размера.
 * - Проверяет SHA-256 каждого загруженного script.js против index.json.
 * - Никогда не запускает скрипт автоматически — только передаёт текст в редактор.
 */
public final class OnlineScriptLibrary {

    private static final Logger LOG = LoggerFactory.getLogger("SBS-ScriptLibrary");
    private static final long CACHE_TTL_MS = 10 * 60 * 1000L;
    private static final int MAX_INDEX_BYTES = 512 * 1024;
    private static final int MAX_SCRIPT_BYTES = 32 * 1024;

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private static volatile String cachedUrl = "";
    private static volatile long cachedAt = 0L;
    private static volatile ScriptLibraryIndex.IndexData cachedData = null;

    private OnlineScriptLibrary() {
    }

    public static String currentModVersion() {
        return ModList.get().getModContainerById(SimpleAeScripting.MOD_ID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("1.4.0");
    }

    /** Возвращает кэшированный список JS-библиотек для автодополнения include("..."). */
    public static List<ScriptLibraryIndex.LibraryEntry> cachedLibraries() {
        ScriptLibraryIndex.IndexData data = cachedData;
        return data == null ? List.of() : data.libraries();
    }

    public static void fetchIndex(String baseUrl, boolean forceRefresh,
                                  Consumer<ScriptLibraryIndex.IndexData> onSuccess,
                                  Consumer<String> onError) {
        String normalized = normalizeBase(baseUrl);
        if (normalized == null) {
            onError.accept("Invalid library URL (must start with https://)");
            return;
        }
        long now = System.currentTimeMillis();
        if (!forceRefresh && cachedData != null && normalized.equals(cachedUrl) && (now - cachedAt) < CACHE_TTL_MS) {
            onSuccess.accept(cachedData);
            return;
        }
        Thread t = new Thread(() -> {
            try {
                byte[] bytes = download(normalized + "/index.json", MAX_INDEX_BYTES);
                ScriptLibraryIndex.IndexData data = ScriptLibraryIndex.parseAll(new String(bytes, StandardCharsets.UTF_8));
                cachedUrl = normalized;
                cachedAt = System.currentTimeMillis();
                cachedData = data;
                Minecraft.getInstance().execute(() -> onSuccess.accept(data));
            } catch (Exception ex) {
                LOG.warn("Failed to load online script library from {}: {}", normalized, ex.getMessage());
                Minecraft.getInstance().execute(() -> onError.accept("Offline or failed to load library"));
            }
        }, "sbs-script-library-index");
        t.setDaemon(true);
        t.start();
    }

    public static void fetchScript(String baseUrl, ScriptLibraryIndex.Entry entry,
                                   Consumer<String> onSuccess,
                                   Consumer<String> onError) {
        String normalized = normalizeBase(baseUrl);
        if (normalized == null) {
            onError.accept("Invalid library URL");
            return;
        }
        Thread t = new Thread(() -> {
            try {
                byte[] bytes = download(normalized + "/" + entry.path(), MAX_SCRIPT_BYTES);
                if (!entry.sha256().isEmpty()) {
                    String actual = ScriptLibraryIndex.sha256Hex(bytes);
                    if (!actual.equalsIgnoreCase(entry.sha256())) {
                        throw new IllegalStateException("SHA-256 checksum mismatch");
                    }
                }
                String code = new String(bytes, StandardCharsets.UTF_8);
                Minecraft.getInstance().execute(() -> onSuccess.accept(code));
            } catch (Exception ex) {
                LOG.warn("Failed to download script {}: {}", entry.id(), ex.getMessage());
                Minecraft.getInstance().execute(() -> onError.accept("Download failed: " + ex.getMessage()));
            }
        }, "sbs-script-library-fetch");
        t.setDaemon(true);
        t.start();
    }

    private static String normalizeBase(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        if (!s.startsWith("https://") || s.length() < 12) {
            return null;
        }
        return s;
    }

    private static byte[] download(String url, int maxBytes) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(6))
                .header("User-Agent", "SimpleByteStorage-Scripting")
                .GET()
                .build();
        HttpResponse<byte[]> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + resp.statusCode());
        }
        byte[] body = resp.body();
        if (body == null || body.length > maxBytes) {
            throw new IllegalStateException("Response exceeds size limit");
        }
        return body;
    }
}
