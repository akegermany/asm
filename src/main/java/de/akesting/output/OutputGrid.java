package de.akesting.output;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import Jama.Matrix;

import com.google.common.base.Preconditions;

import com.google.common.base.Stopwatch;
import de.akesting.autogen.SpatioTemporalContour;
import de.akesting.data.DataRepository;
import de.akesting.utils.FileUtils;
import de.akesting.utils.FormatUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.akesting.utils.FormatUtils.DATE_TIME_FORMATTER;

public final class OutputGrid {

    private static final String SEPARATOR = ",";

    private static final double SMALL_VAL = 1e-6;

    private final String filename;

    private final double dxOut;
    private final double dtOut;

    private final boolean withFileOutput;

    private int nDtOut; // nDtOut --> Number of columns.
    private int nDxOut; // nDxOut --> Number of rows.

    private double xStart;
    private double xEnd;
    private double tStart;
    private double tEnd;

    public Matrix vFree;
    public Matrix vCong;
    public Matrix weight;
    public Matrix vOut;

    public Matrix flowFree;
    public Matrix flowCong;
    public Matrix flowOut;

    public Matrix rhoFree;
    public Matrix rhoCong;
    public Matrix rhoOut;

    public Matrix occFree;
    public Matrix occCong;
    public Matrix occOut;

    public Matrix normFree;
    public Matrix normCong;

    private boolean withFlow;
    private boolean withRho;
    private boolean withOcc;

    private boolean isReverseDirection;

    public OutputGrid(String defaultFilename, SpatioTemporalContour spatioTemporalContour, DataRepository dataRep) {
        Preconditions.checkArgument(defaultFilename != null && !defaultFilename.isEmpty());
        checkNotNull(spatioTemporalContour);
        checkNotNull(dataRep);

        Locale.setDefault(Locale.US);

        dxOut = spatioTemporalContour.getDx();
        dtOut = spatioTemporalContour.getDt();

        xStart = dataRep.xMin();
        xEnd = dataRep.xMax();
        tStart = dataRep.tMin();
        tEnd = dataRep.tMax();

        this.withFlow = dataRep.withFlow();
        this.withRho = dataRep.withDensity();
        this.withOcc = dataRep.withOccupancy();

        isReverseDirection = dataRep.isReverseDirection();

        filename = spatioTemporalContour.isSetFilename() ? spatioTemporalContour.getFilename() : defaultFilename;
        withFileOutput = spatioTemporalContour.isWithOutput();

        System.out.println("filename=" + filename);

        if (spatioTemporalContour.isSetXStartKm()) {
            xStart = spatioTemporalContour.getXStartKm() * 1000.;
        }
        if (spatioTemporalContour.isSetXEndKm()) {
            xEnd = spatioTemporalContour.getXEndKm() * 1000.;
        }
        if (spatioTemporalContour.isSetTStartH()) {
            tStart = spatioTemporalContour.getTStartH() * 3600.;
        }
        if (spatioTemporalContour.isSetTEndH()) {
            tEnd = spatioTemporalContour.getTEndH() * 3600.;
        }

        if (xStart > xEnd || tStart > tEnd) {
            System.err.println(" OutputGrid:: check min/max values... Exit(-1)");
            System.exit(-1);
        }

        nDtOut = (int) ((tEnd - tStart) / dtOut) + 1;
        nDxOut = (int) ((xEnd - xStart) / dxOut) + 1;

        // TODO: check mit range aus DataRep !!!

        System.out.printf(" OutputGrid: %n");
        System.out.printf(Locale.US, "    dxOut= %.2f m, dtOut=%.2f s%n", dxOut, dtOut);
        System.out.printf(Locale.US, "    xStart= %.2fm=%.2fkm, xEnd=%.2fm = %.2fkm%n", xStart, xStart / 1000., xEnd,
                xEnd / 1000.);
        System.out.printf(Locale.US, "    tStart= %.2fs=%.2fh, tEnd=%.2fs = %.2fh%n", tStart, tStart / 3600., tEnd,
                tEnd / 3600.);
        System.out.printf(Locale.US, "    grid size: ndx = %d, ndt=%d%n", nDxOut, nDtOut);

        // compare user-defined ranges to data set:
        /*
         * if( xStart < dataRep.xMin() || xEnd > dataRep.xMax() || xStart>=xEnd ||
         * tStart < dataRep.tMin() || tEnd > dataRep.tMax() || tStart>=tEnd ){
         * System.err.println(" !!! check user-defined output range with provided data !!! ");
         * System.exit(-1);
         * }
         */

        initialize();
    }

    private void initialize() {
        // nDxOut --> Number of rows. nDtOut --> Number of columns.
        vFree = new Matrix(nDxOut, nDtOut, 0);
        vCong = new Matrix(nDxOut, nDtOut, 0);
        weight = new Matrix(nDxOut, nDtOut, 0);
        vOut = new Matrix(nDxOut, nDtOut, 0);

        if (withFlow) {
            flowFree = new Matrix(nDxOut, nDtOut, 0);
            flowCong = new Matrix(nDxOut, nDtOut, 0);
            flowOut = new Matrix(nDxOut, nDtOut, 0);
        }
        if (withRho) {
            rhoFree = new Matrix(nDxOut, nDtOut, 0);
            rhoCong = new Matrix(nDxOut, nDtOut, 0);
            rhoOut = new Matrix(nDxOut, nDtOut, 0);
        }
        if (withOcc) {
            occFree = new Matrix(nDxOut, nDtOut, 0);
            occCong = new Matrix(nDxOut, nDtOut, 0);
            occOut = new Matrix(nDxOut, nDtOut, 0);
        }
        normFree = new Matrix(nDxOut, nDtOut, 0);
        normCong = new Matrix(nDxOut, nDtOut, 0);
    }

    public void write(DataRepository dataRep) throws IOException {
        Locale.setDefault(Locale.US);

        Stopwatch stopwatch = Stopwatch.createStarted();
        System.out.print("OutputGrid: write to file = \"" + filename + "\" ....  ");
        Writer fstr = checkNotNull(FileUtils.getWriter(filename));

        // header information for data:
        fstr.write(String.format("# number of data elements: %d%n", dataRep.elementListSize()));
        fstr.write(String.format("# is reverse driving direction: %b%n", isReverseDirection));
        fstr.write(String.format("# min and max intervals of datapoints: %n"));
        fstr.write(String.format("# Space x : [%.2f, %.2f]m = [%.2f, %.2f]km %n", dataRep.xMin(), dataRep.xMax(),
                dataRep.xMin() / 1000, dataRep.xMax() / 1000));
        fstr.write(String.format("# Time t  : [%.2f, %.2f]s = [%.3f, %.3f]h %n", dataRep.tMin(), dataRep.tMax(),
                dataRep.tMin() / 3600, dataRep.tMax() / 3600));
        DateTime startDateTime = new DateTime(Math.round(dataRep.tMin() * 1000), DateTimeZone.UTC);
        DateTime endDateTime = new DateTime(Math.round(dataRep.tMax() * 1000), DateTimeZone.UTC);
        fstr.write(String.format("# Time t  : [%s, %s] %n", startDateTime, endDateTime));
        fstr.write(String.format("# Speed v   : [%.3f, %.3f]m/s = [%.2f, %.2f]km/h %n", dataRep.vMin(), dataRep.vMax(),
                dataRep.vMin() * 3.6, dataRep.vMax() * 3.6));
        fstr.write(String.format("# Flow q    : [%.5f, %.5f]/s  = [%.2f, %.2f]/h %n", dataRep.flowMin(),
                dataRep.flowMax(), 3600 * dataRep.flowMin(), 3600 * dataRep.flowMax()));
        fstr.write(String.format("# Density r : [%.5f, %.5f]/m  = [%.2f, %.2f]/km %n", dataRep.rhoMin(),
                dataRep.rhoMax(), 1000 * dataRep.rhoMin(), 1000 * dataRep.rhoMax()));
        fstr.write(String.format("# Occupancy : [%.5f, %.5f] %n", dataRep.occMin(), dataRep.occMax()));
        fstr.write(String.format("# Units changed to SI system !!! Note that col 4 and 5 are for testing the ASM logic only%n"));
        fstr.write(String.format("# x[m]  t[s]  v[m/s]  [v_free[m/s]]  [v_cong[m/s]]  weight[1]  time[hh:mm:ss] norm[1]"));
        if (withFlow) {
            fstr.write(String.format("  flow[1 / s]"));
        }
        if (withDensity()) {
            fstr.write(String.format("   rho[1 / m]"));
        }
        if (withOccupancy()) {
            fstr.write(String.format("  occupancy[1]"));
        }
        fstr.write(String.format("%n"));

        for (int ix = 0; ix < ndx(); ix++) {
            double x0 = position(ix);
            for (int it = 0; it < ndt(); it++) {
                double t0 = time(it);

                long millis = Math.round(t0 * 1000);
                DateTime dateTime = new DateTime(millis, DateTimeZone.UTC);

                // output of time as integer: gnuplot can interpret this as (epoch) seconds
                fstr.write(String.format("%.2f%s%.0f%s%.2f%s%.2f%s%.2f%s%.5f%s%s%s%.5f", x0, SEPARATOR, t0, SEPARATOR, vOut.get(ix, it), SEPARATOR,
                        vFree.get(ix, it), SEPARATOR, vCong.get(ix, it), SEPARATOR, weight.get(ix, it), SEPARATOR, DATE_TIME_FORMATTER.print(dateTime),
                        SEPARATOR, normFree.get(ix, it)));
                if (withFlow()) {
                    fstr.write(String.format("%s%.5f", SEPARATOR, flowOut.get(ix, it)));
                }
                if (withDensity()) {
                    fstr.write(String.format("%s%.6f", SEPARATOR, rhoOut.get(ix, it)));
                }
                if (withOccupancy()) {
                    fstr.write(String.format("%s%.4f", SEPARATOR, occOut.get(ix, it)));
                }
                fstr.write(String.format("%n"));
            }
            fstr.write(String.format("%n")); // block ends
        }
        fstr.close();
        System.out.println(" done in " + stopwatch);
    }


    double getSpeedResult(double x, double t) {
        return interpolate(vOut, x, t);
    }

    double getFlowResult(double x, double t) {
        if (!withFlow)
            return 0;
        return interpolate(this.flowOut, x, t);
    }

    double getDensityResult(double x, double t) {
        if (!withRho)
            return 0;
        return interpolate(this.rhoOut, x, t);
    }

    double getOccupancyResult(double x, double t) {
        if (!withOcc)
            return 0;
        return interpolate(this.occOut, x, t);
    }

    double getWeightResult(double x, double t) {
        return interpolate(weight, x, t);
    }

    boolean isDataAvailable(double x, double t) {
        // System.out.printf(" x=%f, t=%f, %b %n", x,t,(x>=xStart && x<=xEnd && t>=tStart && t<=tEnd) );
        // return( x>=xStart && x<=xEnd && t>=tStart && t<=tEnd );
        return (x >= xStart && x <= xEndGrid() && t >= tStart && t <= tEndGrid());
    }

    private double interpolate(Matrix matrix, double x, double t) {
        int xindex = (int) (SMALL_VAL + (x - xStart) / dxOut);
        int tindex = (int) (SMALL_VAL + (t - tStart) / dtOut);
        // development:
        if (!isDataAvailable(x, t)) {
            System.out.printf("x-xstart=%.8f --> xindex=%d=%.8f, nDxOut=%d%n", x - xStart, xindex,
                    (x - xStart) / dxOut, nDxOut);
            System.out.printf("t-tstart=%.8f --> tindex=%d=%.8f, nDtOut=%d%n", t - tStart, tindex,
                    (t - tStart) / dtOut, nDtOut);
            System.err.printf(" interpolate:: (x,t)=(%.5f,%.5f) excesses boundary limits! exit(-1)%n", x, t);
            System.exit(-1);
        }

        // System.out.printf("interpolate::(x=%.3f,t=%.3f) --> matrix(data, %d, %d)=%.2f%n", x, t, xindex, tindex,
        // matrix.get(xindex, tindex));

        // interpolation in 2D (bilinear), cf. Numerical Recipies
        double w1 = 0;
        double w2 = 0;
        if (xindex + 1 < matrix.getRowDimension())
            w1 = ((x - xStart) - xindex * dxOut) / dxOut;
        if (tindex + 1 < matrix.getColumnDimension())
            w2 = ((t - tStart) - tindex * dtOut) / dtOut;
        // data grid:
        double v1 = matrix.get(xindex, tindex);
        double v2 = 0;
        double v3 = 0;
        double v4 = 0;
        if (w1 > 0)
            v2 = matrix.get(xindex + 1, tindex);
        if (w2 > 0)
            v4 = matrix.get(xindex, tindex + 1);
        if (w1 > 0 && w2 > 0)
            v3 = matrix.get(xindex + 1, tindex + 1);
        // System.out.printf("w1=%f, w2=%f, v1=%.3f, v2=%.3f, v3=%.3f, v4=%.3%n",w1,w2,v1,v2,v3,v4);
        return ((1 - w1) * (1 - w2) * v1 + w1 * (1 - w2) * v2 + w1 * w2 * v3 + (1 - w1) * w2 * v4);
    }

    public boolean withFlow() {
        return withFlow;
    }

    public boolean withDensity() {
        return withRho;
    }

    public boolean withOccupancy() {
        return withOcc;
    }

    public double dtOut() {
        return dtOut;
    }

    public double dxOut() {
        return dxOut;
    }

    public int ndt() {
        return vFree.getColumnDimension();
    }

    public int ndx() {
        return vFree.getRowDimension();
    }

    public double position(int ix) {
        return xStart + ix * dxOut;
    }

    public double time(int it) {
        return tStart + it * dtOut;
    }

    public double tStart() {
        return tStart;
    }

    public double tEnd() {
        return tEnd;
    }

    public double xStart() {
        return xStart;
    }

    public double xEnd() {
        return xEnd;
    }

    public double tStartGrid() {
        return tStart();
    }

    public double xStartGrid() {
        return xStart();
    }

    public double tEndGrid() {
        return tStart + (nDtOut - 1) * dtOut;
    }

    public double xEndGrid() {
        return xStart + (nDxOut - 1) * dxOut;
    }

    public boolean isReverseDirection() {
        return isReverseDirection;
    }

    public boolean withFileOutput() {
        return withFileOutput;
    }

    public double xStartKm() {
        return xStart / 1000.;
    }

    public double xEndKm() {
        return xEnd / 1000.;
    }

    public double tStartH() {
        return tStart / 3600.;
    }

    public double tEndH() {
        return tEnd / 3600.;
    }
}
