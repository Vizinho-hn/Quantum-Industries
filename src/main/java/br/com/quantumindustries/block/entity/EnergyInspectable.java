package br.com.quantumindustries.block.entity;

import net.minecraft.network.chat.Component;

/** Implemented by electrical block entities that expose energy readings to Energy Inspection Goggles and adjacent transfer. */
public interface EnergyInspectable {
    int energy();
    int capacity();
    Component inspectTitle();

    default int receiveEnergy(int maxReceive) {
        return 0;
    }

    default Component inspectDetail() {
        return null;
    }
}
