package br.com.quantumindustries.energy;

import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.energy.DelegatingEnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Exposes a real machine battery with the same rate budget used by internal transfers. */
public final class MachineEnergyPort extends DelegatingEnergyHandler {
    private final BlockEntity machine;
    private final BooleanSupplier receives;
    private final BooleanSupplier supplies;
    private final TransferBudget budget;
    private final SnapshotJournal<TransferBudget.State> journal;

    public MachineEnergyPort(BlockEntity machine, EnergyHandler battery, BooleanSupplier receives,
            BooleanSupplier supplies, TransferBudget budget, Runnable changed) {
        super(battery);
        this.machine = machine;
        this.receives = receives;
        this.supplies = supplies;
        this.budget = budget;
        journal = new SnapshotJournal<>() {
            @Override protected TransferBudget.State createSnapshot() {
                return budget == null ? null : budget.snapshot();
            }

            @Override protected void revertToSnapshot(TransferBudget.State state) {
                if (budget != null) budget.restore(state);
            }

            @Override protected void onRootCommit(TransferBudget.State state) {
                if (!machine.isRemoved()) changed.run();
            }
        };
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        if (!(machine.getLevel() instanceof ServerLevel level) || machine.isRemoved() || !receives.getAsBoolean()) return 0;
        long tick = level.getGameTime();
        int limit = budget == null ? amount : Math.min(amount, budget.receiveRemaining(tick));
        int inserted = super.insert(limit, transaction);
        if (inserted > 0) {
            journal.updateSnapshots(transaction);
            if (budget != null) budget.recordReceive(tick, inserted);
        }
        return inserted;
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        if (!(machine.getLevel() instanceof ServerLevel level) || machine.isRemoved() || !supplies.getAsBoolean()) return 0;
        long tick = level.getGameTime();
        int limit = budget == null ? amount : Math.min(amount, budget.extractRemaining(tick));
        int extracted = super.extract(limit, transaction);
        if (extracted > 0) {
            journal.updateSnapshots(transaction);
            if (budget != null) budget.recordExtract(tick, extracted);
        }
        return extracted;
    }
}
