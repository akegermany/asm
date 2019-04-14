package de.akesting.output;

public class OutputDataPoint {

    private final float x;
    private final float t;
    private final float[] data = new float[OutputDataType.VALUES.size()];


    OutputDataPoint(double x, double t) {
        this.x = (float) x;
        this.t = (float) t;
    }

    public double x() {
        return x;
    }

    public double t() {
        return t;
    }

    public double getValue(OutputDataType outputDataType) {
        return data[outputDataType.ordinal()];
    }


    public void setValue(OutputDataType outputDataType, double value) {
        data[outputDataType.ordinal()] = (float) value;
    }

}
