package br.com.quantumindustries.energy;

public final class TransferBudgetCheck {
    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("Transfer budget invariant");
    }

    public static void main(String[] args) {
        for (int limit : new int[] {48, 256}) {
            var budget = new TransferBudget(limit);
            long tick = 100;
            check(budget.receiveRemaining(tick) == limit);
            budget.recordReceive(tick, limit / 2);
            budget.recordReceive(tick, limit / 2);
            check(budget.receiveRemaining(tick) == 0);
            check(budget.extractRemaining(tick) == limit);
            budget.recordExtract(tick, limit - 1);
            check(budget.extractRemaining(tick) == 1);
            budget.recordExtract(tick, 1);
            check(budget.extractRemaining(tick) == 0);
            try {
                budget.recordExtract(tick, 1);
                throw new AssertionError("Overdraw accepted");
            } catch (IllegalArgumentException expected) {}
            check(budget.extractRemaining(tick + 1) == limit);
            check(budget.receiveRemaining(tick + 1) == limit);
            try {
                budget.recordReceive(tick + 1, -1);
                throw new AssertionError("Negative input accepted");
            } catch (IllegalArgumentException expected) {}
            check(budget.receiveRemaining(tick + 1) == limit);
        }
        System.out.println("TransferBudgetCheck: PASS");
    }
}
