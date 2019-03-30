package de.akesting.data;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;

import com.google.common.base.Stopwatch;
import de.akesting.autogen.ParameterASM;
import de.akesting.output.OutputGrid;
import org.apache.commons.math3.util.FastMath;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class AdaptiveSmoothingMethod {

    private static final double CRIT_NORM_THRESHOLD = 0.1;

    private final ParameterASM parameter;

    private double drivingDir = 1;

    private OutputGrid grid;

    private DataView view;

    public AdaptiveSmoothingMethod(ParameterASM parameterASM) {
        this.parameter = checkNotNull(parameterASM);
        checkArgument(Math.abs(parameterASM.getVgFreeKmh()) > 0.000001,
                "vgFree is near zero (devision not defined)");
        checkArgument(Math.abs(parameterASM.getVgCongKmh()) > 0.000001,
                "vgCong is near zero (devision not defined)");
    }

    private double phi(double x, double t) {
        return phiX(x) * phiT(t);
    }

    private double phiX(double x) {
        if (parameter.isWithTriangular()) {
            return Math.max(0, 1 - Math.abs(x) / parameter.getDxSmooth());
        }
        return Math.exp(-Math.abs(x / parameter.getDxSmooth()));
    }

    private double phiT(double t) {
        if (parameter.isWithTriangular()) {
            return Math.max(0, 1 - Math.abs(t) / parameter.getDtSmooth());
        }
        return Math.exp(-Math.abs(t / parameter.getDtSmooth()));
    }

    public void doSmoothing(DataView view, OutputGrid grid) {
        this.view = checkNotNull(view);
        this.grid = checkNotNull(grid);

        view.generateGriddedData(parameter.getDxSmooth(), parameter.getDtSmooth());

        System.out.println("ASM ... doSmoothing ");

        this.drivingDir = (view.isReverseDirection()) ? -1 : 1;

        Stopwatch stopwatch = Stopwatch.createStarted();

        calculateGrid();
        calculateWeights();
        calculateQuantities();

        System.out.println("**** Profiling: Time for whole loop took " + stopwatch);
    }

    private void calculateGrid() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        double minNormFree = 1e10; // init
        double minNormCong = 1e10; // init

        // main loop
        for (int ix = 0; ix < grid.ndx(); ix++) {
            double x0 = grid.position(ix);
            for (int it = 0; it < grid.ndt(); it++) {
                double t0 = grid.time(it);
                // defaults
                double normFree = 0;
                double normCong = 0;
                double vFree = 0;
                double vCong = 0;
                double flowFree = 0;
                double flowCong = 0;
                double rhoFree = 0;
                double rhoCong = 0;
                double occFree = 0;
                double occCong = 0;

                // loop over data points
                List<Datapoint> griddedData = view.getData(x0, t0);
                // Iterable<Datapoint> griddedData = view.getDataWithoutCopying(x0, t0);
                for (Datapoint dp : griddedData) {

                    double x = dp.x();
                    double t = dp.t();

                    // System.out.printf("x0=%6.1fm, x=%6.1fm,  t0=%6.1fs, t=%6.1fs,  |x-x0|=%6.3fm, |t-t0|=%6.3fs%n", x0, x, t0, t,
                    // Math.abs(x-x0), Math.abs(t-t0));
                    double v = dp.v();

                    double phiFree = dp.weight() * phi(x - x0, t - t0 - drivingDir * (x - x0) / vgFree());
                    normFree += phiFree;
                    vFree += phiFree * v;

                    double phiCong = dp.weight() * phi(x - x0, t - t0 - drivingDir * (x - x0) / vgCong());
                    normCong += phiCong;
                    vCong += phiCong * v;

                    if (view.withFlow()) {
                        double flow = dp.q();
                        flowFree += phiFree * flow;
                        flowCong += phiCong * flow;
                    }

                    if (view.withDensity()) {
                        double rho = dp.rho();
                        rhoFree += phiFree * rho;
                        rhoCong += phiCong * rho;
                    }

                    if (view.withOccupancy()) {
                        double occ = dp.occ();
                        occFree += phiFree * occ;
                        occCong += phiCong * occ;
                    }
                }

                // make normalization:
                grid.vFree.set(ix, it, (normFree == 0) ? 0 : vFree / normFree);
                grid.vCong.set(ix, it, (normCong == 0) ? 0 : vCong / normCong);

                if (view.withFlow()) {
                    grid.flowFree.set(ix, it, (normFree == 0) ? 0 : flowFree / normFree);
                    grid.flowCong.set(ix, it, (normCong == 0) ? 0 : flowCong / normCong);
                }

                if (view.withDensity()) {
                    grid.rhoFree.set(ix, it, (normFree == 0) ? 0 : rhoFree / normFree);
                    grid.rhoCong.set(ix, it, (normCong == 0) ? 0 : rhoCong / normCong);
                }

                if (view.withOccupancy()) {
                    grid.occFree.set(ix, it, (normFree == 0) ? 0 : occFree / normFree);
                    grid.occCong.set(ix, it, (normCong == 0) ? 0 : occCong / normCong);
                }

                // testwise:
                grid.normFree.set(ix, it, normFree);
                grid.normCong.set(ix, it, normCong);

                minNormFree = Math.min(normFree, minNormFree);
                minNormCong = Math.min(normCong, minNormCong);

            }
        }

        if (Math.min(minNormFree, minNormCong) < CRIT_NORM_THRESHOLD) {
            System.out.printf("minNormFree=%6.5f, minNormCong=%6.5f %n", minNormFree, minNormCong);
            System.out
                    .printf("Warning: norm is quite low and smaller than threshold %3.2f. Please try a generous cut-off than the actual settings!%n",
                            CRIT_NORM_THRESHOLD);
        }

        System.out.println("calculate grid data took " + stopwatch);
    }

    private void calculateWeights() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        // (3) calculate weights w --> matrix
        final double vc = parameter.getVcKmh() / 3.6;
        final double dvc = parameter.getDvcKmh() / 3.6;
        for (int ix = 0; ix < grid.ndx(); ix++) {
            for (int it = 0; it < grid.ndt(); it++) {
                double vCong = grid.vCong.get(ix, it);
                double vFree = grid.vFree.get(ix, it);
                double vDecide = Math.min(vCong, vFree);
                if (grid.normCong.get(ix, it) == 0)
                    vDecide = vFree;
                double w = Math.max(0, Math.min(1, 0.5 * (1. + tanh((vc - vDecide) / dvc))));
                grid.weight.set(ix, it, w);
            }
        }
        System.out.println("calculate weights took " + stopwatch);
    }


    private void calculateQuantities() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        // (4) calc results (for speed only at this stage)
        for (int ix = 0; ix < grid.ndx(); ix++) {
            for (int it = 0; it < grid.ndt(); it++) {
                double w = grid.weight.get(ix, it);
                double result = w * grid.vCong.get(ix, it) + (1 - w) * grid.vFree.get(ix, it);
                grid.vOut.set(ix, it, result);
                if (view.withFlow()) {
                    grid.flowOut.set(ix, it, w * grid.flowCong.get(ix, it) + (1 - w) * grid.flowFree.get(ix, it));
                }
                if (view.withDensity()) {
                    grid.rhoOut.set(ix, it, w * grid.rhoCong.get(ix, it) + (1 - w) * grid.rhoFree.get(ix, it));
                }
                if (view.withOccupancy()) {
                    grid.occOut.set(ix, it, w * grid.occCong.get(ix, it) + (1 - w) * grid.occFree.get(ix, it));
                }
                // testwise:
                double normResult = w * grid.normCong.get(ix, it) + (1 - w) * grid.normFree.get(ix, it);
                grid.normFree.set(ix, it, normResult);
            }
        }

        System.out.println("calculate quantities took " + stopwatch);
    }

    private double tanh(double x) {
        return FastMath.tanh(x);
        // return Math.tanh(x);
    }

    private double vgFree() {
        return parameter.getVgFreeKmh() / 3.6;
    }

    private double vgCong() {
        return parameter.getVgCongKmh() / 3.6;
    }

    public void kernelTestOutput(String filename, OutputGrid grid) {
        System.out.println(" kernelTestOutput to file=\"" + filename + "\"");

        int drivingDir = 1;
        double x = 0.5 * (grid.xEnd() - grid.xStart());
        double t = 0.5 * (grid.tEnd() - grid.tStart());

        PrintWriter fstr = null;
        try {
            fstr = new PrintWriter(new BufferedWriter(new FileWriter(filename, false)));
            // header information for data:
            fstr.printf("# x[km]  t[h]  phi_free  phi_cong %n");
            for (int ix = 0; ix < grid.ndx(); ix++) {
                double x0 = grid.position(ix);
                for (int it = 0; it < grid.ndt(); it++) {
                    double t0 = grid.time(it);
                    double phiFree = phi(x - x0, t - t0 - drivingDir * (x - x0) / vgFree());
                    double phiCong = phi(x - x0, t - t0 - drivingDir * (x - x0) / vgCong());
                    fstr.printf(Locale.US, "%e   %e   %e    %e%n", x0 / 1000, t0 / 3600, phiFree, phiCong);
                }
                fstr.printf("%n"); // block ends
            }
        } catch (java.io.IOException e) {
            System.err.println("Error  " + "Cannot open file " + filename);
            e.printStackTrace();
        } finally {
            if (fstr != null) {
                fstr.close();
            }
        }
        System.out.println(" ASM Kerneltest finished...");
    }
}
