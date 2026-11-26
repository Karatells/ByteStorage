package com.simpleae.bytestorage.scripting.script;

import com.simpleae.bytestorage.scripting.ScriptingConfig;
import com.simpleae.bytestorage.scripting.client.ScriptLibraryIndex;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * Серверный резолвер и кэш JS-библиотек для {@code include("author/lib")}.
 * <p>
 * Порядок поиска (никогда не блокирует игровой тик-поток HTTP-запросами):
 * 1. Локальная библиотека в {@code config/simple_bytestorage_scripting/libs/<id>.js} (работает даже офлайн).
 * 2. Кэш в памяти.
 * 3. Проверенный по SHA-256 дисковый кэш {@code config/simple_bytestorage_scripting/libs/.cache/<id>.js}.
 * 4. Фоновая загрузка из {@code ScriptingConfig.ONLINE_LIBRARY_URL} с проверкой SHA-256 против {@code index.json}.
 */
public final class LibraryRegistry implements LibraryResolver {

    public static final LibraryRegistry INSTANCE = new LibraryRegistry();

    private static final Logger LOG = LoggerFactory.getLogger("SBS-LibraryRegistry");
    private static final Pattern VALID_ID = Pattern.compile("^[a-z0-9][a-z0-9_-]{0,31}(/[a-z0-9][a-z0-9_-]{0,31})?$");
    private static final long INDEX_TTL_MS = 10 * 60 * 1000L;
    private static final long ERROR_RETRY_MS = 30 * 1000L;
    private static final int MAX_INDEX_BYTES = 512 * 1024;
    private static final int MAX_LIB_BYTES = 32 * 1024;

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private final Map<String, ScriptLibraryIndex.LibraryEntry> indexById = new ConcurrentHashMap<>();
    private final Map<String, String> sourceCache = new ConcurrentHashMap<>();
    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();
    private final Set<String> pendingAfterIndex = ConcurrentHashMap.newKeySet();
    private final Map<String, FailedDownload> failedDownloads = new ConcurrentHashMap<>();
    private final AtomicBoolean indexLoading = new AtomicBoolean(false);

    private volatile String indexUrl = "";
    private volatile long indexLoadedAt = 0L;
    private volatile String lastIndexError = null;
    private volatile long lastIndexErrorAt = 0L;

    private record FailedDownload(String message, long at) {
    }

    private LibraryRegistry() {
    }

    public static boolean isValidId(String id) {
        return id != null && VALID_ID.matcher(id).matches();
    }

    /** Асинхронная предзагрузка index.json при старте сервера/игры (если разрешено конфигом). */
    public void preloadAsync() {
        try {
            if (ScriptingConfig.ALLOW_ONLINE_LIBRARY.get()) {
                triggerIndexFetch(ScriptingConfig.ONLINE_LIBRARY_URL.get());
            }
        } catch (IllegalStateException ignored) {
            // Конфиг ещё не загружен (например, в юнит-тестах)
        }
    }

    @Override
    public Resolution resolve(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return Resolution.error("include(): library id is required");
        }
        String id = rawId.trim().toLowerCase(Locale.ROOT);
        if (!isValidId(id)) {
            return Resolution.error("include(): invalid library id '" + rawId + "'");
        }

        // 1. Локальный оверрайд / офлайн-библиотека администратора
        String local = tryReadLocalOverride(id);
        if (local != null) {
            return Resolution.ok(local);
        }

        // 2. Память
        String cached = sourceCache.get(id);
        if (cached != null) {
            return Resolution.ok(cached);
        }

        // 3. Онлайн-репозиторий
        if (!ScriptingConfig.ALLOW_ONLINE_LIBRARY.get()) {
            return Resolution.disabled(id);
        }

        String baseUrl = normalizeBase(ScriptingConfig.ONLINE_LIBRARY_URL.get());
        if (baseUrl == null) {
            return Resolution.error("invalid onlineLibraryUrl in config");
        }

        long now = System.currentTimeMillis();
        boolean indexFresh = indexLoadedAt > 0 && baseUrl.equals(indexUrl) && (now - indexLoadedAt) < INDEX_TTL_MS;
        if (!indexFresh) {
            if (indexLoadedAt == 0 && lastIndexError != null && (now - lastIndexErrorAt) < ERROR_RETRY_MS) {
                return Resolution.error("library index unavailable: " + lastIndexError);
            }
            pendingAfterIndex.add(id);
            triggerIndexFetch(baseUrl);
            if (indexLoadedAt == 0 || !baseUrl.equals(indexUrl)) {
                return Resolution.loading(id);
            }
        }

        ScriptLibraryIndex.LibraryEntry entry = indexById.get(id);
        if (entry == null) {
            return Resolution.notFound(id);
        }

        // 4. Дисковый кэш с проверкой SHA-256
        String diskCached = tryReadVerifiedDiskCache(entry);
        if (diskCached != null) {
            sourceCache.put(id, diskCached);
            return Resolution.ok(diskCached);
        }

        // 5. Проверка недавней ошибки загрузки
        FailedDownload fail = failedDownloads.get(id);
        if (fail != null && (now - fail.at()) < ERROR_RETRY_MS) {
            return Resolution.error("failed to download library '" + id + "': " + fail.message());
        }

        // 6. Асинхронная загрузка библиотеки
        triggerLibraryFetch(baseUrl, entry);
        return Resolution.loading(id);
    }

    private void triggerIndexFetch(String rawBaseUrl) {
        String baseUrl = normalizeBase(rawBaseUrl);
        if (baseUrl == null || !indexLoading.compareAndSet(false, true)) {
            return;
        }
        Thread t = new Thread(() -> {
            try {
                byte[] bytes = download(baseUrl + "/index.json", MAX_INDEX_BYTES);
                List<ScriptLibraryIndex.LibraryEntry> libs =
                        ScriptLibraryIndex.parseLibraries(new String(bytes, StandardCharsets.UTF_8));
                indexById.clear();
                for (ScriptLibraryIndex.LibraryEntry lib : libs) {
                    indexById.put(lib.id().toLowerCase(Locale.ROOT), lib);
                }
                indexUrl = baseUrl;
                indexLoadedAt = System.currentTimeMillis();
                lastIndexError = null;
                LOG.info("Loaded {} JS libraries from {}", libs.size(), baseUrl);

                // Сразу докачиваем ожидающие библиотеки
                for (String pending : Set.copyOf(pendingAfterIndex)) {
                    pendingAfterIndex.remove(pending);
                    ScriptLibraryIndex.LibraryEntry entry = indexById.get(pending);
                    if (entry != null && !sourceCache.containsKey(pending)) {
                        String disk = tryReadVerifiedDiskCache(entry);
                        if (disk != null) {
                            sourceCache.put(pending, disk);
                        } else {
                            triggerLibraryFetch(baseUrl, entry);
                        }
                    }
                }
            } catch (Exception ex) {
                lastIndexError = ex.getMessage();
                lastIndexErrorAt = System.currentTimeMillis();
                LOG.warn("Failed to fetch JS library index from {}: {}", baseUrl, ex.getMessage());
            } finally {
                indexLoading.set(false);
            }
        }, "sbs-lib-index");
        t.setDaemon(true);
        t.start();
    }

    private void triggerLibraryFetch(String baseUrl, ScriptLibraryIndex.LibraryEntry entry) {
        String id = entry.id().toLowerCase(Locale.ROOT);
        if (!inFlight.add(id)) {
            return;
        }
        Thread t = new Thread(() -> {
            try {
                byte[] bytes = download(baseUrl + "/" + entry.path(), MAX_LIB_BYTES);
                if (!entry.sha256().isEmpty()) {
                    String actual = ScriptLibraryIndex.sha256Hex(bytes);
                    if (!actual.equalsIgnoreCase(entry.sha256())) {
                        throw new IllegalStateException("SHA-256 checksum mismatch");
                    }
                }
                String code = new String(bytes, StandardCharsets.UTF_8);
                sourceCache.put(id, code);
                failedDownloads.remove(id);
                writeDiskCache(id, bytes);
                LOG.info("Cached JS library '{}' ({} bytes, SHA-256 verified)", id, bytes.length);
            } catch (Exception ex) {
                failedDownloads.put(id, new FailedDownload(ex.getMessage(), System.currentTimeMillis()));
                LOG.warn("Failed to download JS library '{}': {}", id, ex.getMessage());
            } finally {
                inFlight.remove(id);
            }
        }, "sbs-lib-fetch-" + id.replace('/', '_'));
        t.setDaemon(true);
        t.start();
    }

    private static Path libsRoot() {
        try {
            return FMLPaths.CONFIGDIR.get().resolve("simple_bytestorage_scripting").resolve("libs");
        } catch (Throwable ignored) {
            return Path.of("config", "simple_bytestorage_scripting", "libs");
        }
    }

    private static String tryReadLocalOverride(String id) {
        try {
            Path root = libsRoot().toAbsolutePath().normalize();
            Path file = root.resolve(id + ".js").normalize();
            if (!file.startsWith(root) || !Files.isRegularFile(file)) {
                return null;
            }
            if (Files.size(file) > MAX_LIB_BYTES) {
                return null;
            }
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String tryReadVerifiedDiskCache(ScriptLibraryIndex.LibraryEntry entry) {
        try {
            String id = entry.id().toLowerCase(Locale.ROOT);
            Path cacheRoot = libsRoot().resolve(".cache").toAbsolutePath().normalize();
            Path file = cacheRoot.resolve(id + ".js").normalize();
            if (!file.startsWith(cacheRoot) || !Files.isRegularFile(file)) {
                return null;
            }
            if (Files.size(file) > MAX_LIB_BYTES) {
                return null;
            }
            byte[] bytes = Files.readAllBytes(file);
            if (!entry.sha256().isEmpty()) {
                String actual = ScriptLibraryIndex.sha256Hex(bytes);
                if (!actual.equalsIgnoreCase(entry.sha256())) {
                    return null;
                }
            }
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void writeDiskCache(String id, byte[] bytes) {
        try {
            Path cacheRoot = libsRoot().resolve(".cache").toAbsolutePath().normalize();
            Path file = cacheRoot.resolve(id + ".js").normalize();
            if (!file.startsWith(cacheRoot)) {
                return;
            }
            Files.createDirectories(file.getParent());
            Files.write(file, bytes);
        } catch (Exception ex) {
            LOG.debug("Could not write library disk cache for {}: {}", id, ex.getMessage());
        }
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
