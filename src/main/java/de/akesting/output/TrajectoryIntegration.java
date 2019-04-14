package de.akesting.output;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Locale;

import com.google.common.base.Preconditions;

import de.akesting.autogen.FloatingCar;
import de.akesting.autogen.Trajectories;
import de.akesting.utils.FileUtils;
import de.akesting.utils.FormatUtils;

public class TrajectoryIntegration {

    private static final String FILE_ENDING = ".gfcd_";

    private static final String FILE_ENDING_FC = ".gfcd_indiv_";

    private final String basename;

    private final OutputGrid grid;

    private final Trajectories config;

    public TrajectoryIntegration(String defaultBasename, Trajectories config, OutputGrid grid) throws IOException {
        this.config = Preconditions.checkNotNull(config);
        this.grid = Preconditions.checkNotNull(grid);
        this.basename = config.isSetBaseFilename() ? config.getBaseFilename() : defaultBasename;

        FileUtils.deleteFileList(".", basename + FILE_ENDING + "\\d*");

        if (config.isSetDn()) {
            calcTrajectories(config.getDn());
        }

        // further Floating cars with individual start pos/times:
        if (config.isSetFloatingCar()) {
            for (int i = 0; i < config.getFloatingCar().size(); i++) {
                FloatingCar fc = config.getFloatingCar().get(i);
                String filename = basename + FILE_ENDING_FC + String.valueOf(i + 1);
                double xStart = grid.isReverseDirection() ? grid.xEndGrid() : grid.xStartGrid();
                if (fc.isSetStartXKm()) {
                    xStart = 1000 * fc.getStartXKm();
                }
                double tStart = fc.getStartTH() * 3600;
                writeTrajectory(filename, tStart, xStart);
            }
        }
    }

    private void calcTrajectories(int nTraj) throws IOException {
        double smallVal = 1;
        double t1 = grid.tStart();
        double t2 = grid.tEndGrid() - integrateTrajBackwards(grid.tEndGrid()) - smallVal;
        double deltaT = (int) ((t2 - t1) / nTraj);
        System.out.printf(" Interval for trajectories: [%.2f, %.2f]h  --> deltaT=%.2fmin%n", t1 / 3600, t2 / 3600,
                deltaT / 60);
        for (int i = 0; i < nTraj; i++) {
            String filename = basename + FILE_ENDING + String.valueOf(i + 1);
            double xStart = grid.isReverseDirection() ? grid.xEndGrid() : grid.xStartGrid();
            writeTrajectory(filename, t1 + i * deltaT, xStart);
        }
    }

    private double integrateTrajBackwards(double t0) {
        double x = !grid.isReverseDirection() ? grid.xEndGrid() : grid.xStartGrid();
        double t = t0;
        double v;
        double dx = 0;
        while (!passedSection(x, !grid.isReverseDirection())) {
            if (!grid.isDataAvailable(x, t)) {
                System.err.println(" !!! Trajectories error ! Check for bug?");
                System.err.println(" xEndGrid = " + grid.xEndGrid() / 1000. + " xStartGrid=" + grid.xStartGrid()
                        / 1000.);
            }
            v = grid.get(OutputDataType.V_OUT, x, t);
            dx = v * config.getDt();
            t -= config.getDt();
            x += !grid.isReverseDirection() ? -dx : dx;
        }
        return (t0 - t);
    }

    private boolean passedSection(double x, boolean isReverse) {
        return ((isReverse) ? (x <= grid.xStartGrid()) : (x >= grid.xEndGrid()));
    }

    private void writeTrajectory(String filename, double tStartTraj, double xStart) throws IOException {
        System.out.println("writeTrajectory: filename=\"" + filename + "\", starttime =" + tStartTraj);
        Writer fstr = FileUtils.getWriter(filename);
        fstr.write(String.format(Locale.US, "# integrated trajectory from GASM velocity field ! %n"));
        fstr.write(String.format(Locale.US, "# t[s]  x[m]  v[m/s]  t(hh:mm:ss) "));
        fstr.write(String.format("%n"));
        // main loop:
        double t = tStartTraj;
        double x = xStart; // (isReverseDirection)? grid.xEndGrid() : grid.xStartGrid();
        double v;
        double dx = 0;
        while (!passedSection(x, grid.isReverseDirection())) {
            if (!grid.isDataAvailable(x, t))
                break;
            v = grid.get(OutputDataType.V_OUT, x, t);
            // / v = effectiveSpeed(v); // !!!! TODO !!! Noch keine Parameter, nicht quantiativ getestet !!!
            fstr.write(String.format(Locale.US, " %f  %f  %f  %s ", t, x, v, FormatUtils.getFormattedTime(t)));
            // integration
            dx = v * config.getDt();
            x += grid.isReverseDirection() ? -dx : dx;
            t += config.getDtOut();
            fstr.write(String.format("%n"));
        }
        fstr.close();
    }
}
