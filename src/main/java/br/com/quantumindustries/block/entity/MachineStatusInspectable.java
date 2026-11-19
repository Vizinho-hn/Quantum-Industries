package br.com.quantumindustries.block.entity;

import net.minecraft.network.chat.Component;

/** Shared client-visible status contract for machine blocks so players can see at a glance whether a machine is running, blocked, or ready. */
public interface MachineStatusInspectable {
    enum State { IDLE, WORKING, BLOCKED, READY }

    Component statusTitle();

    Component statusDetail();

    State statusState();

    /** Progress in [0.0, 1.0] while working, or 1.0 when output is ready. */
    float statusProgress();
}
