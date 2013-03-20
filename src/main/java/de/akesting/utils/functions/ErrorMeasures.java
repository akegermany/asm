package de.akesting.utils.functions;

public class ErrorMeasures {

    private ErrorMeasures() {
    }

    public static double calcRelativeError(double x1[], double x2[]) {
        double sum = 0;
        int counter = 0;
        for (int i = 0; i < x1.length; i++) {
            if (x1[i] != 0) { // !!!
                sum += Math.pow((x1[i] - x2[i]) / x1[i], 2);
                counter++;
            }
        }
        return Math.sqrt(sum / counter);
    }

    public static double calcAbsoluteError(double x1[], double x2[]) {
        double sum = 0;
        double denom = 0;
        for (int i = 0; i < x1.length; i++) {
            System.out.printf("x1=%.4f, x2=%.4f %n", x1[i], x2[i]);
            sum += Math.pow(x1[i] - x2[i], 2);
            denom += Math.pow(x1[i], 2);
        }
        return Math.sqrt(sum / denom);
    }

    public static double calcMixError(double x1[], double x2[]) {
        double sum = 0;
        double denom = 0;
        int counter = 0;
        for (int i = 0; i < x1.length; i++) {
            // System.out.printf("x1=%.4f, x2=%.4f %n", x1[i], x2[i]);
            if (x1[i] != 0) {
                sum += Math.pow((x1[i] - x2[i]) / x1[i], 2);
                denom += Math.pow(x1[i], 2);
                counter++;
            }
        }
        denom = Math.sqrt(denom / counter);
        return Math.sqrt(sum / (counter * denom));
    }

    public static double calcMaxError(double x1[], double x2[]) {
        double max = 0;
        for (int i = 0; i < x1.length; i++) {
            if (Math.abs(x1[i] - x2[i]) > max)
                max = Math.abs(x1[i] - x2[i]);
        }
        return max;
    }

}
