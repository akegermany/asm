package de.akesting.data;

import java.util.Random;

import com.google.common.base.Preconditions;

import de.akesting.autogen.RandomErrors;

class DataRandomizer {

    private static final long SEED = 42;

    private final Random rand;

    private final RandomErrors randomErrors;

    DataRandomizer(RandomErrors randomErrors) {
        this.randomErrors = Preconditions.checkNotNull(randomErrors);
        rand = (SEED > 0) ? new Random(SEED) : new Random();
    }

    final Datapoint randomize(Datapoint dp) {
        if (rand == null) {
            return dp;
        }
        // Datapoint dp = new Datapoint(dpOrg);
        if (randomErrors.isSetRelPosition()) {
            double xOrg = dp.x();
            double xErr = (2 * rand.nextDouble() - 1) * randomErrors.getRelPosition();
            dp.set_x(Math.max(0, xOrg + xErr));
        }
        if (randomErrors.isSetRelSpeed()) {
            double vOrg = dp.v();
            double vErrRel = 1 + (2 * rand.nextDouble() - 1) * randomErrors.getRelSpeed();
            dp.set_v(Math.max(0, vOrg * vErrRel));
        }
        if (randomErrors.isSetRelTime()) {
            double tOrg = dp.t();
            double tErrRel = 1 + (2 * rand.nextDouble() - 1) * randomErrors.getRelTime();
            dp.set_v(Math.max(0, tOrg * tErrRel));
        }

        return dp;
    }
}
