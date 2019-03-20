package de.akesting.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Tables {

    private static final Logger LOG = LoggerFactory.getLogger(Tables.class);

    private static final double TINY_VALUE = 1.e-10;

    private Tables() {
    }

    /**
     * intp interpolates the array tab with n+1 equidistant points in [xmin,xmax]
     * at the location x; an error message is produced on attempting extrapolation
     */
    public static double intp(double[] tab, double x, double xmin, double xmax) {
        return intp(tab, tab.length, x, xmin, xmax);
    }

    public static double intp(double[] tab, int n, double x, double xmin, double xmax) {
        double intp_value = tab[0];
        double ir = n * (x - xmin) / (xmax - xmin);
        int i = (int) ir;
        double rest = ir - i;
        if ((i >= 0) && (i < n - 1)) {
            intp_value = (1 - rest) * tab[i] + rest * tab[i + 1];
        } else if (i == n - 1) {
            intp_value = tab[n - 1];
        } else {
            LOG.error("intp: index i={} (ir={}) out of range", i, ir);
            System.exit(-1);
        }
        return intp_value;
    }

    /**
     * Inter- and extrapolation of tabulated functions given by an array x_vals
     * with INCREASING (but nonequidistant) values, and the associated y values
     * y_vals. x is the value for which inter(extra-)polation is sought. On
     * extrapolation, the left value y_vals[0] (right value y_vals[n]) is
     * outputted if x<x_vals[0] (x>x_vals[n]). NOT TIME OPTIMIZED
     */

    public static double intpextp(double[] x_vals, double[] y_vals, double x) {
        int nx = x_vals.length;
        int ny = y_vals.length;
        int n = Math.min(nx, ny);
        if (nx != ny) {
            LOG.warn("Tables.intpextp: length={} of x_vals not equal length={} of yvals[]", nx, ny);
        }
        int i = 0;
        double intp_value;

        while ((x_vals[i] <= x) && (i < n - 1))
            i++;

        if (i == 0) { // extrapolation to "left" side
            intp_value = y_vals[0];
        } else if ((i == n - 1) && (x > x_vals[i])) { // extrapol. to "right" side
            intp_value = y_vals[n - 1];
        } else if (Math.abs(x_vals[i] - x_vals[i - 1]) < TINY_VALUE) { // treatment
            // of jumps
            intp_value = y_vals[i];
        } else { // linear interpolation
            intp_value = y_vals[i - 1] + (y_vals[i] - y_vals[i - 1]) * (x - x_vals[i - 1])
                    / (x_vals[i] - x_vals[i - 1]);
        }

        if (false) {
            LOG.warn("Tables.intpextp: nx={}" + nx + " ny=" + ny + " x=" + x + " x_vals[0]=" + x_vals[0]
                    + " x_vals[nx-1]=" + x_vals[nx - 1] + " result=" + intp_value);
        }
        return (intp_value);
    }

    // interpolation taken from mic-code
    // Version using only the index range imin ... imax and extrapolating
    // constant values otherwise; reverse=true means that the array x
    // has x values in decreasing order. NOT TIME OPTIMIZED
    public static double intpextp(double[] x, double[] y, double pos, boolean reverse) {
        return intpextp(x, y, pos, 0, x.length - 1, reverse);
    }

    public static double intpextp(double[] x, double[] y, double pos, int imin, int imax, boolean reverse) {

        double tinyValue = 0.000001;
        double intp_value;
        int i = imin;
        if (reverse)
            while ((x[i] >= pos) && (i < imax))
                i++;
        else
            while ((x[i] <= pos) && (i < imax))
                i++;
        if (i == imin)
            intp_value = y[imin]; // left extrapolation
        else if (i == imax)
            intp_value = y[imax]; // right extrapolation
        else if (Math.abs(x[i] - x[i - 1]) < tinyValue)
            intp_value = y[i]; // same x values
        else
            intp_value = y[i - 1] + (y[i] - y[i - 1]) * (pos - x[i - 1]) / (x[i] - x[i - 1]); // interpolation
        return (intp_value);
    }

}
