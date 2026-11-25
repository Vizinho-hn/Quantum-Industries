package br.com.quantumindustries.energy;

/** Per-device, per-server-tick transfer ceiling. Input and output are separate rates. */
public final class TransferBudget {
    private final int limit;
    private long tick = Long.MIN_VALUE;
    private int received, extracted;

    public TransferBudget(int limit) {
        if (limit <= 0) throw new IllegalArgumentException("Transfer limit must be positive");
        this.limit = limit;
    }

    record State(long tick, int received, int extracted) {}

    State snapshot() { return new State(tick, received, extracted); }

    void restore(State state) {
        tick = state.tick();
        received = state.received();
        extracted = state.extracted();
    }

    private void refresh(long now) {
        if (now != tick) {
            tick = now;
            received = extracted = 0;
        }
    }

    public int receiveRemaining(long now) {
        refresh(now);
        return limit - received;
    }

    public int extractRemaining(long now) {
        refresh(now);
        return limit - extracted;
    }

    public void recordReceive(long now, int amount) {
        if (amount < 0 || amount > receiveRemaining(now)) throw new IllegalArgumentException("Input budget exceeded");
        received += amount;
    }

    public void recordExtract(long now, int amount) {
        if (amount < 0 || amount > extractRemaining(now)) throw new IllegalArgumentException("Output budget exceeded");
        extracted += amount;
    }
}
