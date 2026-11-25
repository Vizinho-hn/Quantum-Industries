package br.com.quantumindustries.thermal;

import net.minecraft.core.Direction;

/** A loaded block entity with its own thermal body and directional contact properties. */
public interface ThermalParticipant {
    ThermalBody thermalBody();
    boolean connectsThermal(Direction face);
    int thermalContactConductanceMilli(Direction face);
}
