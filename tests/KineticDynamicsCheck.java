import br.com.quantumindustries.mechanical.KineticDynamics;

/** Standalone physics regression: javac + java, without a Minecraft installation. */
public final class KineticDynamicsCheck {
    private static void expect(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static boolean near(double a, double b) { return Math.abs(a - b) <= 1e-8; }
    public static void main(String[] args) {
        var detail = KineticDynamics.advanceDetailed(KineticDynamics.State.atRest(), 6, 2, 2, 0, 12, .05);
        expect(near(detail.angularAcceleration(), 2), "detailed acceleration");
        expect(near(KineticDynamics.reflectInertia(3, 2), 12), "reflected inertia");
        expect(near(KineticDynamics.mergeAngularVelocity(12, 6), 2), "momentum merge");
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
        var stuck = KineticDynamics.advanceDetailed(rest, 2.05, 2, 2, 0, 12, .05, .08);
        expect(stuck.stalled() && stuck.angularAcceleration() == 0 && stuck.netTorque() == 0, "static friction holds a weak drive");
        expect(!KineticDynamics.advanceDetailed(rest, 2.1, 2, 2, 0, 12, .05, .08).stalled(), "drive overcomes static friction");
        expect(near(detail.netTorque(), 4) && near(detail.kineticEnergy(), .01), "detailed torque and stored energy");
        expect(near(detail.mechanicalPower(), .4), "detailed net mechanical power");
        for (double ratio : new double[] {2.0, -.5}) {
            expect(near(KineticDynamics.reflectInertia(4, ratio), 4 * ratio * ratio), "gear reflects inertia quadratically");
            expect(near(KineticDynamics.reflectDrag(.2, ratio), .2 * ratio * ratio), "gear reflects drag quadratically");
            double reflectedDrive = KineticDynamics.reflectDriveTorque(6, ratio, .8);
            double localOmega = ratio * 3;
            expect(near(reflectedDrive * 3, 6 * localOmega * .8), "drive reflection loses rather than creates power");
            double localLoad = 5;
            double reflectedLoad = KineticDynamics.reflectLoadTorque(localLoad, ratio, .8);
            expect(localLoad * Math.abs(localOmega) <= reflectedLoad * 3, "output power cannot exceed input power");
        }
        expect(near(KineticDynamics.mergeAngularVelocity(4 * 3 + 2 * -1, 6), 10.0 / 6), "joining rotors conserves signed momentum");
        expect(near(KineticDynamics.mergeAngularVelocity(4 * 2, 4), 2), "splitting retains local angular velocity");
        invalid = false;
        try { KineticDynamics.advanceDetailed(rest, Double.NaN, 0, 1, 0, 12, .05, 1); }
        catch (IllegalArgumentException e) { invalid = true; }
        expect(invalid, "static friction cannot mask invalid torque");
        invalid = false;
        try { KineticDynamics.reflectDriveTorque(2, 0, .8); } catch (IllegalArgumentException e) { invalid = true; }
        expect(invalid, "zero gear ratio is rejected");
        invalid = false;
        try { KineticDynamics.reflectLoadTorque(2, 1, 1.1); } catch (IllegalArgumentException e) { invalid = true; }
        expect(invalid, "efficiency above unity is rejected");
        System.out.println("KineticDynamicsCheck: PASS (integration, friction, gears, power, momentum, invalid inputs)");
    }
}
