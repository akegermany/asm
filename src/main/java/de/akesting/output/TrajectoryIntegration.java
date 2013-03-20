package de.akesting.output;

import java.io.PrintWriter;
import java.util.Locale;

import com.google.common.base.Preconditions;

import de.akesting.autogen.FloatingCar;
import de.akesting.autogen.Trajectories;
import de.akesting.utils.FileUtils;

public class TrajectoryIntegration {

    private String basename;
    private final String fileEnding = ".gfcd_";
    private final String fileEndingFC = ".gfcd_indiv_";
    private boolean isReverseDirection;
    /*
     * private double xStart;
     * private double xEnd;
     * private double tStart;
     * private double tEnd;
     */
    private double dt;
    private double dtOut;

    private OutputGrid grid;

    private final Trajectories config;

    public TrajectoryIntegration(String defaultBasename, Trajectories config, OutputGrid grid) {
        this.config = Preconditions.checkNotNull(config);
        this.grid = grid;

        /*
         * xStart = grid.xStart();
         * xEnd = grid.xEnd();
         * tStart = grid.tStart();
         * tEnd = grid.tEnd();
         */

        isReverseDirection = grid.isReverseDirection();
        /*
         * if(isReverseDirection){
         * double d = xStart;
         * xStart = xEnd;
         * xEnd = d;
         * }
         */

        this.basename = config.getBaseFilename();
        dt = config.getDt();
        dtOut = config.getDtOut();
        int nTraj = config.getDn();

        FileUtils.deleteFileList(".", basename + fileEnding + "\\d*");

        if (nTraj > 0) {
            calcTrajectories(nTraj);
        }

        // further Floating cars with individual start pos/times:
        if (config.isSetFloatingCar()) {
            for (int i = 0; i < config.getFloatingCar().size(); i++) {
                FloatingCar fc = config.getFloatingCar().get(i);
                String filename = basename + fileEndingFC + String.valueOf(i + 1);
                double xStart = (isReverseDirection) ? grid.xEndGrid() : grid.xStartGrid();
                if (fc.isSetStartXKm()) {
                    xStart = 1000 * fc.getStartXKm();
                }
                double tStart = fc.getStartT();
                writeTrajectory(filename, tStart, xStart);
            }
        }
    }

    private void calcTrajectories(int nTraj) {
        double SMALL_VAL = 1; //
        double t1 = grid.tStart();
        double t2 = grid.tEndGrid() - integrateTrajBackwards(grid.tEndGrid()) - SMALL_VAL;
        double deltaT = (int) ((t2 - t1) / nTraj);
        System.out.printf(" Interval for trajectories: [%.2f, %.2f]h  --> deltaT=%.2fmin%n", t1 / 3600, t2 / 3600,
                deltaT / 60);
        for (int i = 0; i < nTraj; i++) {
            String filename = basename + fileEnding + String.valueOf(i + 1);
            double xStart = (isReverseDirection) ? grid.xEndGrid() : grid.xStartGrid();
            writeTrajectory(filename, t1 + i * deltaT, xStart);
        }
    }

    private double integrateTrajBackwards(double t0) {
        double x = (!isReverseDirection) ? grid.xEndGrid() : grid.xStartGrid();
        double t = t0;
        double v;
        double dx = 0;
        while (!passedSection(x, !isReverseDirection)) {
            if (!grid.isDataAvailable(x, t)) {
                System.err.println(" !!! Trajectories error ! Check for bug?");
                System.err.println(" xEndGrid = " + grid.xEndGrid() / 1000. + " xStartGrid=" + grid.xStartGrid()
                        / 1000.);
            }
            v = grid.getSpeedResult(x, t);
            dx = v * dt;
            t -= dt;
            x += (!isReverseDirection) ? -dx : dx;
        }
        return (t0 - t);
    }

    private boolean passedSection(double x, boolean isReverse) {
        return ((isReverse) ? (x <= grid.xStartGrid()) : (x >= grid.xEndGrid()));
    }

    private void writeTrajectory(String filename, double tStartTraj, double xStart) {
        System.out.println("writeTrajectory: filename=\"" + filename + "\", starttime =" + tStartTraj);
        PrintWriter fstr = FileUtils.getWriter(filename);
        fstr.printf(Locale.US, "# integrated trajectory from GASM velocity field ! %n");
        fstr.printf(Locale.US, "# t[s]  x[m]  v[m/s]  t(hh:mm:ss) ");
        fstr.printf("%n");
        // main loop:
        double t = tStartTraj;
        double x = xStart; // (isReverseDirection)? grid.xEndGrid() : grid.xStartGrid();
        double v;
        double dx = 0;
        while (!passedSection(x, isReverseDirection)) {
            if (!grid.isDataAvailable(x, t))
                break;
            v = grid.getSpeedResult(x, t);
            // / v = effectiveSpeed(v); // !!!! TODO !!! Noch keine Parameter, nicht quantiativ getestet !!!
            fstr.printf(Locale.US, " %f  %f  %f  %s ", t, x, v, FormatUtils.getFormatedTime(t));
            // integration
            dx = v * dt;
            x += (isReverseDirection) ? -dx : dx;
            t += dtOut;
            fstr.printf("%n");
        }
        fstr.close();
    }
}
