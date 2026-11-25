package br.com.quantumindustries.energy.qes;

import br.com.quantumindustries.energy.qes.ElectricalNetworkState.NetworkStatus;

/** For an edge, current is signed from its first endpoint to its second. */
public record ElectricalNodeState(double potentialMv, double currentMa, double resistanceMOhm,
                                  double powerFePerTick, double lossFePerTick, NetworkStatus status) {}
