package com.simpleae.bytestorage.util;

/** Правило обмена энергией между Контроллером и Конденсатором (чистая логика, покрыта тестами). */
public final class CapacitorLogic {

    /** Конденсатор подпитывает Контроллер, пока тот заполнен меньше, чем на эту долю... */
    public static final double REFILL_BELOW = 0.50;
    /** ...до этого уровня. */
    public static final double REFILL_TO = 0.60;
    /** Конденсатор забирает излишек, когда Контроллер заполнен больше, чем на эту долю... */
    public static final double ABSORB_ABOVE = 0.90;
    /** ...сбрасывая Контроллер до этого уровня. */
    public static final double ABSORB_TO = 0.85;

    private CapacitorLogic() {
    }

    /**
     * @return положительное число — сколько FE конденсатор отдаёт Контроллеру, отрицательное — сколько забирает у него,
     *         0 — ничего не делать
     * @param limit максимум FE за один обмен
     */
    public static int transfer(int controllerStored, int controllerMax, int capStored, int capMax, int limit) {
        if (controllerMax <= 0 || limit <= 0) {
            return 0;
        }
        double fill = controllerStored / (double) controllerMax;
        if (fill < REFILL_BELOW && capStored > 0) {
            int target = (int) (controllerMax * REFILL_TO);
            return Math.max(0, Math.min(Math.min(capStored, target - controllerStored), limit));
        }
        if (fill > ABSORB_ABOVE && capStored < capMax) {
            int target = (int) (controllerMax * ABSORB_TO);
            return -Math.max(0, Math.min(Math.min(capMax - capStored, controllerStored - target), limit));
        }
        return 0;
    }
}
