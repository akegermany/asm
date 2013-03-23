package de.akesting.output;

import java.io.PrintWriter;
import java.util.Locale;

import com.google.common.base.Preconditions;

import de.akesting.utils.FileUtils;

public class Traveltime {
    private final String filename;

    private final double v0Ref;

    private final double referenceTraveltime;

    private final double dt;

    private final double dtOut;

    private PrintWriter fstr;

    private OutputGrid grid;

    public Traveltime(String defaultFilename, de.akesting.autogen.Traveltime traveltime, OutputGrid grid) {
        this.grid = Preconditions.checkNotNull(grid);
        v0Ref = traveltime.getSpeedReferenceKmh() / 3.6;
        dt = traveltime.getDt();
        dtOut = traveltime.getDtOut();

        referenceTraveltime = Math.abs(grid.xEnd() - grid.xStart()) / v0Ref;
        filename = traveltime.isSetFilename() ? traveltime.getFilename() : defaultFilename;
        fstr = FileUtils.getWriter(filename);
        calcTraveltime();
    }

    private void calcTraveltime() {
        Preconditions.checkNotNull(fstr);
        System.out.println("Traveltime: write to file = \"" + filename + "\"");
        writeHeader();

        double t = grid.tStart();
        while (t <= grid.tEnd()) {
            double ttInstant = integrateSpace(t);
            double ttEnter = integrateTraj(t);
            double ttExit = integrateTrajBackwards(t);
            double ttMean = 0.5 * (ttEnter + ttExit);
            if (ttEnter < 0)
                ttMean = ttExit;
            if (ttExit < 0)
                ttMean = ttEnter;
            double vMean = Math.abs(grid.xEnd() - grid.xStart()) / Math.abs(ttMean);
            // double vMean0=(ttEnter<0) ? 0 : Math.abs(xEnd-xStart)/Math.abs(ttEnter);
            // double vMean1=(ttExit<0) ? 0 : Math.abs(xEnd-xStart)/Math.abs(ttExit);
            fstr.printf(Locale.US, "%8.5f  %8.5f  %8.5f  %8.5f  ", t / 3600., ttMean / 60., (ttMean / referenceTraveltime),
                    vMean * 3.6);
            fstr.printf(Locale.US, " %s%.5f  ", (ttEnter < 0) ? "?" : "", ttEnter / 60.);
            fstr.printf(Locale.US, " %s%.5f ", (ttExit < 0) ? "?" : "", ttExit / 60.);
            fstr.printf(Locale.US, " %8.5f ", ttInstant / 60.);
            fstr.printf(Locale.US, " %s %n", FormatUtils.getFormatedTime(t));
            t += dtOut;
        }
        fstr.close();
    }

    private double integrateSpace(double t0) {
        double ttInst = 0;
        double x = grid.xStart();
        double v = 0;
        double dx;
        while (x < grid.xEnd()) {
            if (!grid.isDataAvailable(x, t0))
                return -1;
            v = grid.getSpeedResult(x, t0);
            dx = v * dt;
            ttInst += (v <= 1) ? 0 : dx / v; // !!!
            x += (v <= 1) ? 1 * dt : dx;
        }
        return (ttInst);
    }

    private double integrateTraj(double t0) {
        double x = grid.isReverseDirection() ? grid.xEnd() : grid.xStart();
        double t = t0;
        double v;
        double dx = 0;
        while (!passedSection(x, grid.isReverseDirection())) {
            if (!grid.isDataAvailable(x, t))
                return -1;
            v = grid.getSpeedResult(x, t);
            dx = v * dt;
            t += dt;
            x += grid.isReverseDirection() ? -dx : dx;
        }
        return (t - t0);
    }

    // ans Ende setzen und rueckwarts in zeit
    private double integrateTrajBackwards(double t0) {
        double x = grid.isReverseDirection() ? grid.xStart() : grid.xEnd();
        double t = t0;
        double v;
        double dx = 0;
        while (!passedSection(x, !grid.isReverseDirection())) {
            if (!grid.isDataAvailable(x, t))
                return -1;
            v = grid.getSpeedResult(x, t);
            dx = v * dt;
            t -= dt;
            x += !grid.isReverseDirection() ? -dx : dx;
        }
        return (t0 - t);
    }

    private boolean passedSection(double x, boolean isReverse) {
        return ((isReverse) ? (x <= grid.xStart()) : (x >= grid.xEnd()));
    }

    private void writeHeader() {
        Preconditions.checkNotNull(fstr);
        fstr.printf("# Spatiotemporal intervals for calculating the traveltime: %n");
        fstr.printf(Locale.US, "# Space x : [%.2f, %.2f]m = [%.2f, %.2f]km %n", grid.xStart(), grid.xEnd(), grid.xStartKm(),
                grid.xEndKm());
        fstr.printf(Locale.US, "# Time  t : [%.2f, %.2f]s = [%.3f, %.3f]h %n", grid.tStart(), grid.tEnd(), grid.tStartH(),
                grid.tEndH());
        fstr.printf(Locale.US, "# integration dt = %fs %n", dt);
        fstr.printf(Locale.US, "# output dt      = %fs %n", dtOut);
        fstr.printf(Locale.US, "# v0Ref: %.2fkm/h --> reference traveltime: %.1fs=%.2fmin=%fh %n", 3.6 * v0Ref,
                referenceTraveltime, referenceTraveltime / 60., referenceTraveltime / 3600.);
        fstr.printf(Locale.US, "# Definition of travel times: %n");
        fstr.printf(Locale.US,
                "# ttEnter(t): travel time for passing section at time when ENTERING the section (not defined at end of time interval) %n");
        fstr.printf(
                Locale.US,
                "# ttExit(t) : travel time for passing section at time when LEAVING the section  (not defined at beginning of time time interval)%n");
        fstr.printf(Locale.US,
                "# tt(t)     : 0.5*(ttEnter+ttExit). If one of the travel times is NOT defined, the mean tt is simply the defined quantity%n");
        fstr.printf(Locale.US, "# t[h]  tt[min] tt_rel[1]  vMean[km/h]  ttEnter[min]  ttExit[min]  ttInst[min]  t(hh:mm:ss) %n");
    }

}
