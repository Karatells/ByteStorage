package com.simpleae.bytestorage.scripting.script;

/**
 * Элемент панели, которую скрипт рисует в Логическом терминале.
 *
 * @param kind  {@link #TEXT}, {@link #BAR}, {@link #BUTTON} или {@link #ITEM}
 * @param text  подпись / текст
 * @param id    для кнопки — идентификатор, для предмета — id предмета
 * @param color 0xRRGGBB, 0 — цвет по умолчанию
 * @param value для полосы — текущее значение
 * @param max   для полосы — максимум
 */
public record UiElement(int kind, String text, String id, int color, int value, int max) {

    public static final int TEXT = 0;
    public static final int BAR = 1;
    public static final int BUTTON = 2;
    public static final int ITEM = 3;

    public static final int MAX_ELEMENTS = 24;
    public static final int MAX_TEXT = 48;
    public static final int MAX_ID = 40;
}
