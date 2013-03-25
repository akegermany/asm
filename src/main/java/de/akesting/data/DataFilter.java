package de.akesting.data;

import java.util.Random;

import com.google.common.base.Preconditions;

import de.akesting.autogen.Filter;

class DataFilter {

    private final static long SEED = 42; // <0: not defined seed

    private Random rand = null;

    private final Filter filterConfiguration;

    DataFilter(Filter filter) {
        this.filterConfiguration = Preconditions.checkNotNull(filter);
        rand = (SEED > 0) ? new Random(SEED) : new Random();
    }

    public final boolean dropData(Datapoint dp) {
        if (filterConfiguration.isSetDropDataRel() && sortOutRandomly())
            return true;

        if (filterConfiguration.isSetTimeEndH() && filterConfiguration.isSetTimeStartH()
                && dp.t() > filterConfiguration.getTimeStartH() / 3600.
                && dp.t() <= filterConfiguration.getTimeEndH() / 3600.) {
            System.out.printf("filter out %s", dp.toString());
            return true;
        }

        if (dp.containsSpeed()) {
            if (filterConfiguration.isSetSpeedMinKmh() && dp.v() <= filterConfiguration.getSpeedMinKmh() / 3.6) {
                return true;
            }
            if (filterConfiguration.isSetSpeedMaxKmh() && dp.v() >= filterConfiguration.getSpeedMaxKmh() / 3.6) {
                return true;
            }
        }

        // TODO for flow, density, occupancy

        return false;
    }

    private final boolean sortOutRandomly() {
        if (rand == null) {
            return false;
        }
        return (rand.nextDouble() <= filterConfiguration.getDropDataRel());
    }

}
