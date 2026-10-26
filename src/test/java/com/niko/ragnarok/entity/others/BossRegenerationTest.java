package com.niko.ragnarok.entity.others;

public final class BossRegenerationTest {
    public static void main(String[] args) {
        BossRegeneration regeneration = new BossRegeneration();
        for (int cycle = 0; cycle < 3; cycle++) {
            for (int tick = 1; tick <= 20; tick++) {
                check(regeneration.advance(true, 20) == (tick == 20), "Wrong heal tick");
            }
        }
        for (int tick = 0; tick < 19; tick++) regeneration.advance(true, 20);
        check(!regeneration.advance(false, 20), "Ineligible tick healed");
        for (int tick = 1; tick <= 20; tick++) {
            check(regeneration.advance(true, 20) == (tick == 20), "Timer was not reset");
        }
        check(regeneration.advance(true, 1), "One-tick interval failed");
        check(regeneration.advance(true, 1), "One-tick interval did not repeat");
        BossRegeneration otherMob = new BossRegeneration();
        regeneration.advance(true, 3);
        check(!otherMob.advance(true, 3), "Timers shared between mobs");
        check(!otherMob.advance(true, 3), "Timer advanced too soon");
        check(otherMob.advance(true, 3), "Custom interval failed");
        System.out.println("Boss regeneration interval and reset checks passed.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
