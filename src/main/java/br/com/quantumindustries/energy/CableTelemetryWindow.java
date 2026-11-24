package br.com.quantumindustries.energy;

/** Two bounded one-second buckets. Only successfully committed cable transfers
 * are recorded. Read the last completed bucket independent of block tick order. */
public final class CableTelemetryWindow {
    public static final int TICKS = 20;
    public record Sample(int injected, int delivered, int loss, int peakFlow, int throughput) {
        public static final Sample ZERO = new Sample(0,0,0,0,0);
    }
    private long second = Long.MIN_VALUE;
    private Sample current = Sample.ZERO, previous = Sample.ZERO;

    public void record(long tick, int flowThisTick, int amount, int injected, int delivered, int loss) {
        long bucket = Math.floorDiv(tick, TICKS);
        if (bucket < second) throw new IllegalArgumentException("Telemetry tick moved backwards");
        if (bucket != second) {
            previous = second != Long.MIN_VALUE && bucket - second == 1 ? current : Sample.ZERO;
            current = Sample.ZERO;
            second = bucket;
        }
        current = new Sample(current.injected() + injected, current.delivered() + delivered,
                current.loss() + loss, Math.max(current.peakFlow(), flowThisTick),
                current.throughput() + amount);
    }

    public Sample completed(long tick) {
        long wanted = Math.floorDiv(tick, TICKS) - 1;
        if (wanted == second) return current;
        if (second != Long.MIN_VALUE && wanted == second - 1) return previous;
        return Sample.ZERO;
    }
}
