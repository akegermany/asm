package de.akesting.data;

import de.akesting.autogen.SingleData;

public class Datapoint {
    private double x = Double.NaN;
    private double t = Double.NaN;
    private double v = Double.NaN;
    private double q = Double.NaN;
    private double rho = Double.NaN;
    private double occ = Double.NaN;
    private double weight = Double.NaN;

    public Datapoint() {
    }

    // copy constructor
    public Datapoint(Datapoint dp) {
        this.x = dp.x;
        this.t = dp.t;
        this.v = dp.v;
        this.q = dp.q;
        this.rho = dp.rho;
        this.occ = dp.occ;
        this.weight = dp.weight;
    }

    public Datapoint(SingleData singleData) {
        this.x = singleData.getPosKm() / 1000.;
        this.t = singleData.getTimeH() / 3600.;
        this.v = singleData.getSpeedKmh() / 3.6;
        if (singleData.isSetDensityInvkm()) {
            this.rho = singleData.getDensityInvkm() / 1000.;
        }
        if (singleData.isSetFlowInvh()) {
            this.q = singleData.getFlowInvh() / 3600;
        }
        if (singleData.isSetOccupancy()) {
            this.occ = singleData.getOccupancy();
        }
    }

    public double x() {
        return x;
    }

    public double t() {
        return t;
    }

    public double v() {
        return v;
    }

    public double q() {
        return q;
    }

    public double rho() {
        return rho;
    }

    public double occ() {
        return occ;
    }

    public double weight() {
        return weight;
    }

    public void set_x(double val) {
        x = val;
    }

    public void set_t(double val) {
        t = val;
    }

    public void set_v(double val) {
        v = val;
    }

    public void set_q(double val) {
        q = val;
    }

    public void set_rho(double val) {
        rho = val;
    }

    public void set_occ(double val) {
        occ = val;
    }

    public void set_weight(double val) {
        weight = val;
    }

    public boolean containsSpeed() {
        return !Double.isNaN(v);
    }

    public boolean containsFlow() {
        return !Double.isNaN(q);
    }

    public boolean containsDensity() {
        return !Double.isNaN(rho);
    }

    public boolean containsOccupancy() {
        return !Double.isNaN(occ);
    }

    public boolean isValid() {
        if (t < 0)
            return false;
        if (x < 0)
            return false;
        if (containsSpeed() && v < 0)
            return false;
        if (!containsSpeed())
            return false;
        return true;
    }

    public void print() {
        System.out.printf("x=%.2fkm, t=%.2fh, dataWeight=%.1f", x / 1000., t / 3600., weight);
        if (containsSpeed())
            System.out.printf(", v=%.2fkm/h", 3.6 * v);
        if (containsFlow())
            System.out.printf(", q=%.2fkm/h", 3600 * q);
        if (containsDensity())
            System.out.printf(", rho=%.2fkm/h", 1000 * rho);
        if (containsOccupancy())
            System.out.printf(", occ=%.5f", occ);
        System.out.printf("%n");
    }

    @Override
    public String toString() {
        return "Datapoint [x=" + x + ", t=" + t + ", v=" + v + ", q=" + q + ", rho=" + rho + ", occ=" + occ
                + ", weight=" + weight + "]";
    }
}
