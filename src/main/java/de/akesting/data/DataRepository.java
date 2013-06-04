package de.akesting.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;

import de.akesting.autogen.Data;
import de.akesting.autogen.Dataset;
import de.akesting.autogen.Input;
import de.akesting.autogen.SingleData;
import de.akesting.utils.FileUtils;
import de.akesting.utils.FormatUtils;

public final class DataRepository {

    private double xMin;
    private double xMax;
    private double tMin;
    private double tMax;
    private double vMin;
    private double vMax;
    private double flowMin; // flow
    private double flowMax;
    private double rhoMin; // density
    private double rhoMax;
    private double occMin; // occupancy
    private double occMax;

    private boolean withFlow;
    private boolean withRho;
    private boolean withOcc;

    private String filename;
    private String absolutePath;

    private String filenameFilter;
    private PrintWriter fstrFilter = null;

    private boolean isReverseDirection = false;

    public boolean isReverseDirection() {
        return this.isReverseDirection;
    }
    
    public void setReverseDirection(boolean value){
        this.isReverseDirection = value;
    }

    private ArrayList<Datapoint> data = new ArrayList<Datapoint>();

    public List<Datapoint> data() {
        return data;
    }

    private ArrayList<Element> elemDataList = new ArrayList<Element>();

    public int elementListSize() {
        return elemDataList.size();
    }

    public DataRepository(String outFilename, String outFilenameFilter, Input input, String path) {
        initialize();

        this.isReverseDirection = input.isReverseDirection();
        
        this.filename = outFilename;
        if (input.isSetOutputFilename()) {
            this.filename = input.getOutputFilename();
        }
        this.absolutePath = path;
        filenameFilter = outFilenameFilter;

        if (input.isWithOutput()) {
            System.out.println("DataRepository: write filtered data to file = \"" + filenameFilter + "\"");
            fstrFilter = FileUtils.getWriter(filenameFilter);
            writeHeaderString(fstrFilter);
        }

        readData(input);
        analyzeData();

        if (input.isWithOutput()) {
            writeRepository(new File(filename));
        }

        if (fstrFilter != null) {
            fstrFilter.close();
        }
    }
    
    // new constructor for California data, much more lean!
    public DataRepository() {
        initialize();
    }

    void initialize() {
        withFlow = withRho = withOcc = true;
        if (!data.isEmpty())
            data.clear();
        xMin = 0;
        xMax = 0;
        tMin = 0;
        tMax = 0;
        vMin = 0;
        vMax = 0;
        flowMin = 0;
        flowMax = 0;
        rhoMin = 0;
        rhoMax = 0;
    }

    public void analyzeData() {
        if (data.isEmpty()) {
            System.err.println(" !!! DataRepository: no data read !!! exit.... ");
            System.exit(-1);
        }
        xMin = xMax = data.get(0).x();
        tMin = tMax = data.get(0).t();
        vMin = vMax = data.get(0).v();
        flowMin = flowMax = data.get(0).q();
        rhoMin = rhoMax = data.get(0).rho();
        occMin = occMax = data.get(0).occ();

        for (Datapoint dp : data) {
            xMin = Math.min(xMin, dp.x());
            xMax = Math.max(xMax, dp.x());
            tMin = Math.min(tMin, dp.t());
            tMax = Math.max(tMax, dp.t());
            vMin = Math.min(vMin, dp.v());
            vMax = Math.max(vMax, dp.v());
            flowMin = Math.min(flowMin, dp.q());
            flowMax = Math.max(flowMax, dp.q());
            rhoMin = Math.min(rhoMin, dp.rho());
            rhoMax = Math.max(rhoMax, dp.rho());
            occMin = Math.min(occMin, dp.occ());
            occMax = Math.max(occMax, dp.occ());
            // quantities must be provided for EACH datapoint
            // otherwise no evaluation of these quantities !
            if (!dp.containsFlow())
                withFlow = false;
            if (!dp.containsDensity())
                withRho = false;
            if (!dp.containsOccupancy())
                withOcc = false;
        }
        System.out.printf(" DataRepository: %n");
        System.out.printf(" *  data.size() = %d *************** %n", data.size());
        System.out.printf("    xMin= %.2fm=%.2f km, xMax=%.2fm=%.2fkm%n", xMin, xMin / 1000., xMax,
                xMax / 1000.);
        System.out.printf("    tMin= %.2fs=%.2f h=%s, tMax=%.2fs=%.2fh=%s%n", tMin, tMin / 3600., FormatUtils.getFormatedTime(tMin), tMax,
                tMax / 3600., FormatUtils.getFormatedTime(tMax));
        System.out.printf("    speedMin= %.2fkm/h, speedMax=%.2fkm/h%n", vMin * 3.6, vMax * 3.6);
        System.out.printf("    flowMin= %.2f/h, flowMax=%.2f/h%n", flowMin / 3600, flowMax / 3600);
        System.out.printf("    densityMin= %.2f/km, densityMax=%.2f/km%n", rhoMin / 1000, rhoMax / 1000);
        System.out.printf("    occupancyMin= %.5f, occupancyMax=%.5f%n", occMin, occMax);
    }

    private void readData(Input input) {
        for (Dataset dataset : input.getDataset()) {
            elemDataList.clear();
            readSet(dataset);
        }
    }

    private void readSet(Dataset dataset) {
        System.out.println("  *************** DataRepository: readSet ... *************");
        // DataFormat dataFormat = new DataFormat(dataset.getFormat());
        // System.out.println("No element \"" + XmlElements.FormatElem + "\" given... expect single data points ..");

        double weight = dataset.getWeight();

        DataRandomizer dataRandomizer = dataset.isSetRandomErrors() ? new DataRandomizer(dataset.getRandomErrors())
                : null;

        DataFilter filter = dataset.isSetFilter() ? new DataFilter(dataset.getFilter()) : null;

        for (SingleData singleData : dataset.getDatalist().getSingleData()) {
            // System.err.println("single data points not yet implemented...");
            data.add(new Datapoint(singleData));
        }

        for (Data dataEntry : dataset.getDatalist().getData()) {
            DataReader dataReader = new DataReader(dataEntry, dataset.getFormat(), weight, absolutePath);

            while (dataReader.isReady()) {
                Datapoint dp = dataReader.readDatapoint();
                addDataPoint(dp, dataRandomizer, filter);
            } 
        } 
    }

    // for California data
    public boolean addDataPoint(Datapoint dp){
        return addDataPoint(dp, null, null);
    }
    
    private boolean addDataPoint(Datapoint dp, DataRandomizer dataRandomizer, DataFilter filter) {
        if ((!dp.isValid())) {
            // System.out.print("dp not valid: "); dp.print();
            writeDataPoint(dp, fstrFilter);
            return false;
        }
        // else{
        // System.out.print("dp valid: ");
        // dp.print();
        // }
        if (filter != null && filter.dropData(dp)) {
            // System.out.print("dp dropped: "); dp.print();
            writeDataPoint(dp, fstrFilter);
            return false;
        }
        // testweise:
        // if(dp.x()==0 && dp.t()==0){
        // dp.print();
        // }
        if (dataRandomizer != null) {
            dataRandomizer.randomize(dp);
        }

        // add density OR flow from hydrodyn relation:
        if (dp.containsFlow() && !dp.containsDensity()) {
            double rho = (dp.v() == 0) ? 0 : dp.q() / dp.v();
            dp.set_rho(rho);
        }
        if (!dp.containsFlow() && dp.containsDensity()) {
            double flow = dp.v() * dp.rho();
            dp.set_q(flow);
        }
        data.add(dp);
        return true;
    }

    public void writeRepository(File file) {
        System.out.println("DataRepository: write to file = \"" + filename + "\"");
        try {
            PrintWriter fstr = new PrintWriter(new BufferedWriter(new FileWriter(file, false)));
            fstr.printf("# some header information \n");
            fstr.printf("# number of data elements: %d\n", data.size());
            fstr.printf("# min and max intervals of datapoints: \n");
            fstr.printf("# Location x: [%.2f, %.2f]m   = [%.2f, %.2f]km \n", xMin, xMax, xMin / 1000, xMax / 1000);
            fstr.printf("# Time t    : [%.2f, %.2f]s   = [%.3f, %.3f]h \n", tMin, tMax, tMin / 3600, tMax / 3600);
            fstr.printf("# Speed v   : [%.3f, %.3f]m/s = [%.2f, %.2f]km/h \n", vMin, vMax, vMin * 3.6, vMax * 3.6);
            fstr.printf("# Flow q    : [%.5f, %.5f]/s  = [%.2f, %.2f]/h \n", flowMin, flowMax, 3600 * flowMin,
                    3600 * flowMax);
            fstr.printf("# Density r : [%.5f, %.5f]/m  = [%.2f, %.2f]/km \n", rhoMin, rhoMax, 1000 * rhoMin,
                    1000 * rhoMax);
            fstr.printf("# Occupancy : [%.5f, %.5f]    \n", occMin, occMax);

            writeHeaderString(fstr);
            for (Datapoint dp : data) {
                writeDataPoint(dp, fstr);

            }
            fstr.close();
        } catch (java.io.IOException e) {
            System.err.println("Error  " + "Cannot open file " + filename);
            e.printStackTrace();
        }
    }

    private void writeHeaderString(PrintWriter writer) {
        if (writer != null) {
            writer.printf("# position[m]  time[s]  speed[m/s]  flow[1/s]  density[1/m]  weight[1]  occupancy[1]  time[hh:mm:ss]\n");
        }
    }

    private void writeDataPoint(Datapoint dp, PrintWriter writer) {
        if (writer != null) {
            writer.printf("%.2f  %.2f  %.2f  %.5e  %.5e  %.5e  %.5e   %s\n", dp.x(), dp.t(), dp.v(), dp.q(),
                    dp.rho(), dp.weight(), dp.occ(), FormatUtils.getFormatedTime(dp.t()));
        }
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

    public double xMin() {
        return xMin;
    }

    public double xMax() {
        return xMax;
    }

    public double tMin() {
        return tMin;
    }

    public double tMax() {
        return tMax;
    }

    public double vMin() {
        return vMin;
    }

    public double vMax() {
        return vMax;
    }

    public double flowMin() {
        return flowMin;
    }

    public double flowMax() {
        return flowMax;
    }

    public double rhoMin() {
        return rhoMin;
    }

    public double rhoMax() {
        return rhoMax;
    }

    public double occMin() {
        return occMin;
    }

    public double occMax() {
        return occMax;
    }


}
