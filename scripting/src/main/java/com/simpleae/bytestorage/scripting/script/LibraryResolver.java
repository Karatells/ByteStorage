package com.simpleae.bytestorage.scripting.script;

/**
 * Поставщик исходного кода JS-библиотек для функции {@code include(id)}.
 */
public interface LibraryResolver {

    enum Status {
        OK,
        LOADING,
        NOT_FOUND,
        DISABLED,
        ERROR
    }

    record Resolution(Status status, String source, String message) {
        public static Resolution ok(String source) {
            return new Resolution(Status.OK, source, null);
        }

        public static Resolution loading(String id) {
            return new Resolution(Status.LOADING, null, id);
        }

        public static Resolution notFound(String id) {
            return new Resolution(Status.NOT_FOUND, null, "unknown library '" + id + "'");
        }

        public static Resolution disabled(String id) {
            return new Resolution(Status.DISABLED, null,
                    "online libraries disabled by server admin (cannot load '" + id + "')");
        }

        public static Resolution error(String msg) {
            return new Resolution(Status.ERROR, null, msg);
        }
    }

    Resolution resolve(String id);
}
