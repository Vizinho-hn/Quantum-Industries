package br.com.quantumindustries.logistics;

import net.minecraft.network.chat.Component;

public enum ConnectionMode {
    AUTO(true, true), PULL(true, false), PUSH(false, true), NONE(false, false);

    private final boolean input, output;

    ConnectionMode(boolean input, boolean output) {
        this.input = input;
        this.output = output;
    }

    public boolean allowsInput() { return input; }
    public boolean allowsOutput() { return output; }
    public ConnectionMode next() {
        return switch (this) {
            case AUTO -> PULL;
            case NONE -> AUTO;
            case PULL -> PUSH;
            case PUSH -> NONE;
        };
    }
    public Component label() { return Component.translatable("gui.quantumindustries.connection." + name().toLowerCase(java.util.Locale.ROOT)); }
}
