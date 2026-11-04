package com.simpleae.bytestorage.client.guide;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Реєстр розділів Книги-довідника. Основний мод реєструє свої розділи, а аддони (наприклад, Scripting)
 * додають власні через {@link #register} у клієнтському setup — тоді в тій самій книзі з'являється нова інформація.
 * Текст береться з мовних файлів: заголовок і тіло — ключі перекладу (тіло: абзаци через \n, коди § підтримуються).
 */
public final class GuideRegistry {

    /** Розділ книги. Чим менше {@code order}, тим вище в списку. */
    public record Chapter(String id, int order, String titleKey, String bodyKey) {
    }

    private static final List<Chapter> CHAPTERS = new ArrayList<>();

    private GuideRegistry() {
    }

    /** Додає розділ (повторна реєстрація з тим самим id замінює попередній). */
    public static synchronized void register(Chapter chapter) {
        CHAPTERS.removeIf(c -> c.id().equals(chapter.id()));
        CHAPTERS.add(chapter);
    }

    public static synchronized List<Chapter> chapters() {
        List<Chapter> copy = new ArrayList<>(CHAPTERS);
        copy.sort(Comparator.comparingInt(Chapter::order));
        return copy;
    }

    /** Розділи основного мода. */
    public static void registerBuiltin() {
        String[] ids = {"intro", "network", "controller", "storage", "terminals", "wireless", "autocraft", "assembler", "provider", "iobus", "jei"};
        int order = 0;
        for (String id : ids) {
            register(new Chapter(id, order += 10,
                    "guide.simple_bytestorage." + id + ".title",
                    "guide.simple_bytestorage." + id + ".body"));
        }
    }
}
