package de.akesting.output;

import java.io.PrintWriter;
import java.util.Locale;

import de.akesting.utils.FileUtils;

public class Traveltime {
    private String filename;
    private final double v0Ref;
    private final double referenceTraveltime;
    private final double dt;
    private final double dtOut;

    private final boolean isReverseDirection;
    private double xStart;
    private double xEnd;
    private double tStart;
    private double tEnd;

    private PrintWriter fstr;

    private OutputGrid grid;

    public Traveltime(String defaultFilename, de.akesting.autogen.Traveltime traveltime, OutputGrid grid) {
        System.out.println(" constructor of Traveltime ...");
        this.grid = grid;

        xStart = grid.xStartGrid();
        xEnd = grid.xEndGrid();
        tStart = grid.tStartGrid();
        tEnd = grid.tEndGrid();

        isReverseDirection = grid.isReverseDirection();

        v0Ref = traveltime.getSpeedReferenceKmh() / 3.6;
        dt = traveltime.getDt();
        dtOut = traveltime.getDtOut();

        this.referenceTraveltime = Math.abs(xEnd - xStart) / v0Ref;
        calcTraveltime();
    }

    private void calcTraveltime() {
        System.out.println("Traveltime: write to file = \"" + filename + "\"");
        writeHeader();

        // main loop:
        double t = tStart;
        while (t <= tEnd) {
            // System.out.printf("calcTraveltime: t=%.2f, tEnd=%.2f %n", t, tEnd);
            double ttInstant = integrateSpace(t);
            double ttEnter = integrateTraj(t);
            double ttExit = integrateTrajBackwards(t);
            double ttMean = 0.5 * (ttEnter + ttExit);
            if (ttEnter < 0)
                ttMean = ttExit;
            if (ttExit < 0)
                ttMean = ttEnter;
            double vMean = Math.abs(xEnd - xStart) / Math.abs(ttMean);
            // double vMean0=(ttEnter<0) ? 0 : Math.abs(xEnd-xStart)/Math.abs(ttEnter);
            // double vMean1=(ttExit<0) ? 0 : Math.abs(xEnd-xStart)/Math.abs(ttExit);
            fstr.printf(Locale.US, "%8.5f  %8.5f  %8.5f  %8.5f  ", t / 3600., ttMean / 60.,
                    (ttMean / referenceTraveltime), vMean * 3.6);
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
        double x = xStart;
        double v = 0;
        double dx;
        while (x < xEnd) {
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
        double x = (isReverseDirection) ? xEnd : xStart;
        double t = t0;
        double v;
        double dx = 0;
        while (!passedSection(x, isReverseDirection)) {
            if (!grid.isDataAvailable(x, t))
                return -1;
            v = grid.getSpeedResult(x, t);
            dx = v * dt;
            t += dt;
            x += (isReverseDirection) ? -dx : dx;
        }
        return (t - t0);
    }

    // ans Ende setzen und rueckwarts in zeit
    private double integrateTrajBackwards(double t0) {
        double x = (isReverseDirection) ? xStart : xEnd;
        double t = t0;
        double v;
        double dx = 0;
        while (!passedSection(x, !isReverseDirection)) {
            if (!grid.isDataAvailable(x, t))
                return -1;
            v = grid.getSpeedResult(x, t);
            dx = v * dt;
            t -= dt;
            x += (!isReverseDirection) ? -dx : dx;
        }
        return (t0 - t);
    }

    private boolean passedSection(double x, boolean isReverse) {
        return ((isReverse) ? (x <= xStart) : (x >= xEnd));
    }

    private void writeHeader() {
        fstr = FileUtils.getWriter(filename);
        // header information for data:
        fstr.printf("# Spatiotemporal intervals for calculating the traveltime: %n");
        fstr.printf(Locale.US, "# Space x : [%.2f, %.2f]m = [%.2f, %.2f]km %n", xStart, xEnd, xStart / 1000,
                xEnd / 1000);
        fstr.printf(Locale.US, "# Time  t : [%.2f, %.2f]s = [%.3f, %.3f]h %n", tStart, tEnd, tStart / 3600, tEnd / 3600);
        fstr.printf(Locale.US, "# integration dt = %fs %n", dt);
        fstr.printf(Locale.US, "# output dt      = %fs %n", dtOut);
        fstr.printf(Locale.US, "# v0Ref: %.2fkm/h --> reference traveltime: %.1fs=%.2fmin=%fh %n", 3.6 * v0Ref,
                referenceTraveltime, referenceTraveltime / 60., referenceTraveltime / 3600.);
        fstr.printf(Locale.US, "# Definition of travel times: %n");
        fstr.printf(
                Locale.US,
                "# ttEnter(t): travel time for passing section at time when ENTERING the section (not defined at end of time interval) %n");
        fstr.printf(
                Locale.US,
                "# ttExit(t) : travel time for passing section at time when LEAVING the section  (not defined at beginning of time time interval)%n");
        fstr.printf(
                Locale.US,
                "# tt(t)     : 0.5*(ttEnter+ttExit). If one of the travel times is NOT defined, the mean tt is simply the defined quantity%n");
        fstr.printf(Locale.US,
                "# t[h]  tt[min] tt_rel[1]  vMean[km/h]  ttEnter[min]  ttExit[min]  ttInst[min]  t(hh:mm:ss) %n");
    }

}
