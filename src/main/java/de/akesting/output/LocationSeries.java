package de.akesting.output;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Locale;

import com.google.common.base.Preconditions;

import de.akesting.autogen.LocationSeriesOutput;
import de.akesting.autogen.TimeSnapshot;

public class LocationSeries {

    private String basename;
    private double dx;

    public LocationSeries(String basenameOutput, LocationSeriesOutput locationSeriesOutput, OutputGrid grid) {
        Preconditions.checkNotNull(locationSeriesOutput);
        this.basename = basenameOutput;

        dx = locationSeriesOutput.getDx();

        for (TimeSnapshot element : locationSeriesOutput.getTimeSnapshot()) {
            writeSnapshotLoationSeries(element.getTimeH() * 3600, grid);
        }
    }

    private String outputFilename(Integer tInt) {
        return basename + ".t_" + tInt.toString();
    }

    private void writeSnapshotLoationSeries(double time, OutputGrid grid) {
        String filename = outputFilename((int) (time / 60.)); // t in minutes
        try {
            PrintWriter fstr = new PrintWriter(new BufferedWriter(new FileWriter(filename, false)));
            // header information for data:
            fstr.printf("# Spatiotemporal intervals for calculating the traveltime: %n");
            fstr.printf(Locale.US, "# x[km]  vASM[km/h]  ");
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
            double x = grid.xStart();
            while (x < grid.xEnd()) {
                double vASM = grid.getSpeedResult(x, time);
                fstr.printf(Locale.US, "%f  %f  ", x / 1000., vASM * 3.6);
                if (grid.withFlow()) {
                    double flow = grid.getFlowResult(x, time);
                    fstr.printf(Locale.US, "  %f", flow * 3600);
                }
                if (grid.withFlow()) {
                    double rho = grid.getDensityResult(x, time);
                    fstr.printf(Locale.US, "  %f", rho * 1000);
                }
                if (grid.withOccupancy()) {
                    double occ = grid.getOccupancyResult(x, time);
                    fstr.printf(Locale.US, "  %f", occ);
                }
                fstr.printf(Locale.US, "%n");
                fstr.flush();
                x += dx;
            }
            fstr.close();
        } catch (java.io.IOException e) {
            System.err.println("Error  " + "Cannot open file " + filename);
            e.printStackTrace();
        }
    }

}
