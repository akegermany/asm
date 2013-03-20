package de.akesting.utils.functions;

// general 1D function
// y(x) = f(x)

public class DataPoint1D {
    private double x;
    private double y;

    public DataPoint1D(double x, double y) {
        this.x = x;
        this.y = y;
        // Logger.log("DataPoint1D: x="+x+", y="+y);
    }

    // copy constructor
    public DataPoint1D(DataPoint1D dp) {
        this.x = dp.x();
        this.y = dp.y();
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public void setX(double v) {
        x = v;
    }

    public void setY(double v) {
        y = v;
    }

}
