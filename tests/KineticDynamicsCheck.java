import br.com.quantumindustries.mechanical.KineticDynamics;

/** Standalone physics regression: javac + java, without a Minecraft installation. */
public final class KineticDynamicsCheck {
    private static void expect(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static boolean near(double a, double b) { return Math.abs(a - b) <= 1e-8; }
    public static void main(String[] args) {
        var rest = KineticDynamics.State.atRest();
        expect(near(KineticDynamics.advance(rest, 0, 0, 5, 0, 6, .05).angularVelocityRadiansPerSecond(), 0), "idle");
        expect(near(KineticDynamics.advance(rest, 3, 4, 5, 0, 6, .05).angularVelocityRadiansPerSecond(), 0), "stalled load");
        var started = KineticDynamics.advance(rest, 6, 2, 2, 0, 6, .05);
        expect(near(started.angularVelocityRadiansPerSecond(), .1), "angular acceleration");
        var heavy = KineticDynamics.advance(rest, 6, 2, 8, 0, 6, .05);
        expect(heavy.angularVelocityRadiansPerSecond() < started.angularVelocityRadiansPerSecond(), "larger inertia accelerates slower");
        var reverse = KineticDynamics.advance(rest, -6, 2, 2, 0, 6, .05);
        expect(reverse.angularVelocityRadiansPerSecond() < 0, "reverse drive");
        var coasting = KineticDynamics.advance(new KineticDynamics.State(0, .1), 0, 4, 2, 0, 6, .05);
        expect(coasting.angularVelocityRadiansPerSecond() == 0, "load stops without reversing");
        var speedCapped = KineticDynamics.advance(rest, 900, 0, 1, 0, 2, .05);
        expect(near(speedCapped.angularVelocityRadiansPerSecond(), 2), "velocity bound");
        expect(near(KineticDynamics.toRpm(Math.PI), 30), "rpm conversion");
        expect(KineticDynamics.wrap(-Math.PI) >= 0, "angle normalized");
        boolean invalid = false;
        try { KineticDynamics.advance(rest, 1, 0, 0, 0, 2, .05); } catch (IllegalArgumentException e) { invalid = true; }
        expect(invalid, "invalid inertia rejected");
        System.out.println("KineticDynamicsCheck: PASS (10 checks)");
    }
}
