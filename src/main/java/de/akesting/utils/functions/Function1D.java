package de.akesting.utils.functions;

import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;

import de.akesting.utils.FileUtils;

// tabulated function y=f(x)
// independent var: x
// dependent var: y
// assuming equidistant spacings and interpolation in between (+extrapolation)

public class Function1D {
    private double xMin;
    // private double xMax;
    double dX;

    double[] yData;

    public Function1D(List<DataPoint1D> dataPoints) {
        if (dataPoints == null || dataPoints.size() < 2) {
            System.err.println("Function1D error: dataPoints<2 not sufficient for any useful function...");
            System.exit(1);
        }
        int length = dataPoints.size();
        xMin = dataPoints.get(0).x();
        // dX = dataPoints.get(1).x() - dataPoints.get(0).x(); //diskretierungsfehler
        dX = (dataPoints.get(length - 1).x() - xMin) / (length - 1.0);
        // xMax = dataPoints.get(length-1).x();
        yData = new double[length];
        for (int i = 0; i < length; i++) {
            yData[i] = dataPoints.get(i).y();
        }
    }

    public int nSize() {
        return yData.length;
    }

    public double dx() {
        return dX;
    }

    // public double xMin(){ return xMin; }
    // public double xMax(){ return xMax; }

    public double x(int i) {
        return xMin + i * dX;
    }

    public double xStart() {
        return xMin;
    }

    public double xEnd() {
        return x(nSize() - 1);
    }

    public double xMax() {
        return xEnd();
    }

    public double yStart() {
        return yData[0];
    }

    public double yEnd() {
        return yData[nSize() - 1];
    }

    public double value(int i) {
        return yData[i];
    }

    public double valueAt(double x) {
        // nur zur Info:
        if (x > xMax() || x < xMin) {
            System.out.printf(String.format(
                    "Function1D: x=%.5f but definition intervall is [%.4f,%.4f]! Use extrapolation ...", x, xMin,
                    xMax()));
        }
        return intp(yData, x, xMin, xMax());
    }

    public int xIndex(double x) {
        return (int) ((x - xMin) / dX);
    }

    public void writeToFile(String filename) {
        PrintWriter fstr = FileUtils.getWriter(filename);
        fstr.printf(Locale.US, "# nX=%d, dX = %e = %8.6f %n", nSize(), dX, dX);
        fstr.printf(Locale.US, "# xMin = %e = %8.6f %n", xMin, xMin);
        fstr.printf(Locale.US, "# xMax = %e = %8.6f %n", xMax(), xMax());
        fstr.printf(Locale.US, "# %10s %10s %10s %10s%n", "x", "y", "z", "isDefined(true=1, false=0)");
        for (int i = 0, nx = nSize(); i < nx; i++) {
            fstr.printf(Locale.US, "%e  %e %n", x(i), value(i));
        }
        fstr.close();
    }

    private double intp(double[] tab, double x, double xmin, double xmax) {
        int n = tab.length;
        // extrapolation:
        if (x <= xmin)
            return tab[0];
        if (x >= xmax)
            return tab[n - 1];

        double intp_value = tab[0];
        double ir = n * (x - xmin) / (xmax - xmin);
        int i = (int) ir;
        double rest = ir - i;
        if ((i >= 0) && (i < n - 1)) {
            intp_value = (1 - rest) * tab[i] + rest * tab[i + 1];
        } else if (i == n - 1) {
            intp_value = tab[n - 1];
        } else {
            // System.err.println("intp: index i = " + i + " (ir=" + ir + ") out of range\n");
            // System.exit(-1);
        }
        return intp_value;
    }
}
