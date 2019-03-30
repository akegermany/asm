package de.akesting.data;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import de.akesting.autogen.VirtualGrid;

import static com.google.common.base.Preconditions.checkNotNull;

public final class DataView {

    private int nx = 1;
    private int nt = 1;

    private double dx;
    private double dt;

    private final DataRepository dataRep;

    private final VirtualGrid virtualGridConfig;

    private List<Datapoint>[][] griddedData; // matrix of list [x][t]

    boolean withFlow() {
        return dataRep.withFlow();
    }

    boolean withDensity() {
        return dataRep.withDensity();
    }

    boolean withOccupancy() {
        return dataRep.withOccupancy();
    }

    public DataView(VirtualGrid virtualGrid, DataRepository dataRep) {
        this.virtualGridConfig = checkNotNull(virtualGrid);
        this.dataRep = checkNotNull(dataRep);
    }

    boolean withVirtualGrid() {
        return virtualGridConfig.isSetNDtCutoff() && virtualGridConfig.isSetNDxCutoff();
    }

    List<Datapoint> getData(double x0, double t0) {
        if (!withVirtualGrid() || (nx == 1 && nt == 1)) {
            return dataRep.data();
        }
        int indexX0 = indexX(x0);
        int indexT0 = indexT(t0);

        // System.out.printf("x0=%.3fkm, t0=%.3fh --> ix=%d, it=%d (nx=%d, nt=%d) %n", x0, t0, indexX0, indexT0, nx, nt);

        List<Datapoint> list = new ArrayList<>();
        int nNeigbors = 1;
        do {
            if (nNeigbors > 1) {
                // System.out.printf(" only %d datapoints in virtual grid...add %d-th neigbours  (x0=%.3fkm, t0=%.3fh --> ix=%d, it=%d (nx=%d, nt=%d))%n",
                // list.size(), nNeigbors, x0, t0, indexX0, indexT0, nx, nt);
                list.clear();
            }
            for (int ix = -nNeigbors; ix <= nNeigbors; ix++) {
                for (int it = -nNeigbors; it <= nNeigbors; it++) {
                    if (isAvailable(indexX0 + ix, indexT0 + it)) {
                        list.addAll(griddedData[indexX0 + ix][indexT0 + it]);
                    }
                }
            }
            nNeigbors++;
            if (list.size() == dataRep.data().size())
                break;
        } while (list.size() < virtualGridConfig.getNDataMin());
        // System.out.printf("(x0,t0)=(%.2f, %.2f) --> (ix,it)=(%d,%d) --> list.size=%d, maxSize=%d%n", x0, t0, ix, it, list.size(),
        // dataRep.data().size());
        return list;
    }


//    Iterable<Datapoint> getData(double x0, double t0) {
//        if (!withVirtualGrid() || (nx == 1 && nt == 1)) {
//            return dataRep.data();
//        }
//        int indexX0 = indexX(x0);
//        int indexT0 = indexT(t0);
//
//        // System.out.printf("x0=%.3fkm, t0=%.3fh --> ix=%d, it=%d (nx=%d, nt=%d) %n", x0, t0, indexX0, indexT0, nx, nt);
//
//        List<List<Datapoint>> lists = new ArrayList<>();
//        int nNeigbors = 1;
//        do {
//            if (nNeigbors > 1) {
//                // System.out.printf(" only %d datapoints in virtual grid...add %d-th neigbours  (x0=%.3fkm, t0=%.3fh --> ix=%d, it=%d (nx=%d, nt=%d))%n",
//                // list.size(), nNeigbors, x0, t0, indexX0, indexT0, nx, nt);
//                lists.clear();
//            }
//            for (int ix = -nNeigbors; ix <= nNeigbors; ix++) {
//                for (int it = -nNeigbors; it <= nNeigbors; it++) {
//                    if (isAvailable(indexX0 + ix, indexT0 + it)) {
//                        lists.add(griddedData[indexX0 + ix][indexT0 + it]);
//                    }
//                }
//            }
//            nNeigbors++;
//            if (Iterables.size(Iterables.concat(lists)) == dataRep.data().size()) {
//                break;
//            }
//        } while (Iterables.size(Iterables.concat(lists)) < virtualGridConfig.getNDataMin());
//        // System.out.printf("(x0,t0)=(%.2f, %.2f) --> (ix,it)=(%d,%d) --> list.size=%d, maxSize=%d%n", x0, t0, ix, it, list.size(),
//        // dataRep.data().size());
//        return Iterables.concat(lists);
//    }

    private boolean isAvailable(int ix, int it) {
        return (ix >= 0 && ix < nx && it >= 0 && it < nt);
        // if(ret)System.out.printf("isAvailable: nx=%d, nt=%d:  ix=%d, it=%d : %s %n", nx, nt, ix, it, (ret)? "true":"false");
    }

    boolean isReverseDirection() {
        return dataRep.isReverseDirection();
    }

    int nDatapoints() {
        return dataRep.data().size();
    }

    void generateGriddedData(double dxSmooth, double dtSmooth) {
        if (!withVirtualGrid()) {
            nx = nt = 1;
            System.out.printf("######### no virtual grid ########%n");
            return;
        }

        Stopwatch stopwatch = Stopwatch.createStarted();
        this.dx = dxSmooth * virtualGridConfig.getNDxCutoff();
        this.dt = dtSmooth * virtualGridConfig.getNDtCutoff();
        this.nx = Math.max(1, (int) ((dataRep.xMax() - dataRep.xMin()) / dx));
        this.nt = Math.max(1, (int) ((dataRep.tMax() - dataRep.tMin()) / dt));

        System.out.printf("######### calculate Virtual Grid ########%n");
        System.out.printf("### dx=%.5f, dt=%.5f, grid size:  nx=%d, nt=%d %n", dx, dt, nx, nt);
        System.out.printf("#########################################%n");

        griddedData = new ArrayList[nx][nt];
        for (int i = 0; i < nx; i++) {
            for (int j = 0; j < nt; j++) {
                griddedData[i][j] = new ArrayList<>();
            }
        }

        for (Datapoint dp : dataRep.data()) {
            griddedData[indexX(dp.x())][indexT(dp.t())].add(dp);
        }

        System.out.println("gridded data view took " + stopwatch);
        // showGriddedData();
    }

    private int indexX(double x) {
        return Math.min(nx - 1, (int) ((x - dataRep.xMin()) / dx));
    }

    private int indexT(double t) {
        return Math.min(nt - 1, (int) ((t - dataRep.tMin()) / dt));
    }

//    private void showGriddedData() {
//        System.out.printf("dx=%.5f, dt=%.5f,  nx=%d,  nt=%d %n", dx, dt, nx, nt);
//        for (int i = 0; i < nx; i++) {
//            for (int j = 0; j < nt; j++) {
//                System.out.printf("(x=%5.1f,t=%5.1f)=%d ", i * dx, j * dt, griddedData[i][j].size());
//            }
//            System.out.printf("%n%n");
//        }
//    }
}