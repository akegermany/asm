package de.akesting.output;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Locale;

import com.google.common.base.Preconditions;

import de.akesting.autogen.CrossSection;
import de.akesting.autogen.TimeSeriesOutput;
import de.akesting.utils.FormatUtils;

public class TimeSeries {

    private String basename;
    private double dt;

    public TimeSeries(String basenameOutput, TimeSeriesOutput timeSeriesOutput, OutputGrid grid) {
        Preconditions.checkNotNull(timeSeriesOutput);
        this.basename = basenameOutput;
        dt = timeSeriesOutput.getDt();
        for (CrossSection crossSection : timeSeriesOutput.getCrossSection()) {
            writeCrosssectionTimeseries(crossSection.getPositionKm() * 1000, grid);
        }

    }

    private String outputFilename(Integer xInt) {
        return basename + ".x_" + xInt.toString();
    }

    private void writeCrosssectionTimeseries(double x, OutputGrid grid) {
        String filename = outputFilename((int) x);
        try {
            PrintWriter fstr = new PrintWriter(new BufferedWriter(new FileWriter(filename, false)));
            // header information for data:
            // fstr.printf("# write cross-section time series %n");
            fstr.printf(Locale.US, "# t[h]  vASM[km/h]  t(hh:mm:ss)  weight[1] ");
            if (grid.withFlow()) {
                fstr.printf(Locale.US, "  %s", "Flow[1/h]");
            }
            if (grid.withFlow()) {
                fstr.printf(Locale.US, "  %s", "Density[1/km]");
            }
            if (grid.withOccupancy()) {
                fstr.printf(Locale.US, "  %s", "Occupancy [1]");
            }
            fstr.printf(Locale.US, "%n");
            double t = grid.tStart();
            while (t <= grid.tEnd()) {
                double vASM = grid.getSpeedResult(x, t);
                double weight = grid.getWeightResult(x, t);
                fstr.printf(Locale.US, "%8.6f  %7.4f  %s  %7.6f", t / 3600., vASM * 3.6,
                        FormatUtils.getFormattedTime(t), weight);

                if (grid.withFlow()) {
                    double flow = grid.getFlowResult(x, t);
                    fstr.printf(Locale.US, "  %f", flow * 3600);
                }
                if (grid.withFlow()) {
                    double rho = grid.getDensityResult(x, t);
                    fstr.printf(Locale.US, "  %f", rho * 1000);
                }
                if (grid.withOccupancy()) {
                    double occ = grid.getOccupancyResult(x, t);
                    fstr.printf(Locale.US, "  %f", occ);
                }
                fstr.printf(Locale.US, "%n");
                fstr.flush();
                t += this.dt;
            }
            fstr.close();
        } catch (java.io.IOException e) {
            System.err.println("Error  " + "Cannot open file " + filename);
            System.err.println(e);
        }

    }

}
