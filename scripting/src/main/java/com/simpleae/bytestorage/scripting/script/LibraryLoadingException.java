package com.simpleae.bytestorage.scripting.script;

/**
 * Нефатальное исключение: запрошенная через {@code include(id)} библиотека ещё загружается в фоне.
 * Логическая шина остаётся включённой и автоматически повторит компиляцию на следующем интервале.
 */
public final class LibraryLoadingException extends RuntimeException {

    private final String libraryId;

    public LibraryLoadingException(String libraryId) {
        super("loading library '" + libraryId + "'");
        this.libraryId = libraryId;
    }

    public String libraryId() {
        return libraryId;
    }
}
