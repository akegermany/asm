package de.akesting.data;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jdom.Element;

import de.akesting.autogen.Data;
import de.akesting.autogen.Dataset;
import de.akesting.autogen.Input;
import de.akesting.autogen.SingleData;
import de.akesting.utils.FileUtils;
import de.akesting.utils.FormatUtils;

public class DataRepository {

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

    private String filename;
    private String absolutePath;

    private String filenameFilter;
    private PrintWriter fstrFilter = null;

    private boolean isReverseDirection = false;

    public boolean isReverseDirection() {
        return this.isReverseDirection;
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

        System.out.printf(" DataRepository: %n");
        System.out.printf(Locale.US, " *  data.size() = %d *************** %n", data.size());
        System.out.printf(Locale.US, "    xMin= %.2fm=%.2f km, xMax=%.2fm=%.2fkm%n", xMin, xMin / 1000., xMax,
                xMax / 1000.);
        System.out.printf(Locale.US, "    tMin= %.2fs=%.2f h, tMax=%.2fs=%.2fh%n", tMin, tMin / 3600., tMax,
                tMax / 3600.);
        System.out.printf(Locale.US, "    speedMin= %.2fkm/h, speedMax=%.2fkm/h%n", vMin * 3.6, vMax * 3.6);
        System.out.printf(Locale.US, "    flowMin= %.2f/h, flowMax=%.2f/h%n", flowMin / 3600, flowMax / 3600);
        System.out.printf(Locale.US, "    densityMin= %.2f/km, densityMax=%.2f/km%n", rhoMin / 1000, rhoMax / 1000);
        System.out.printf(Locale.US, "    occupancyMin= %.5f, occupancyMax=%.5f%n", occMin, occMax);

        if (input.isWithOutput()) {
            writeRepository();
        }

        if (fstrFilter != null) {
            fstrFilter.close();
        }

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

    private void analyzeData() {
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
            //System.err.println("single data points not yet implemented...");
            data.add(new Datapoint(singleData));
        }
        
        for (Data dataEntry : dataset.getDatalist().getData()) {
            DataReader dataReader = new DataReader(dataEntry, dataset.getFormat(), weight, absolutePath);

            while (dataReader.isReady()) {
                // System.out.print(".");

                Datapoint dp = dataReader.readDatapoint();
                // System.out.print("dp:"); dp.print();

                if ((!dp.isValid())) {
                    // System.out.print("dp not valid: "); dp.print();
                    writeDataPoint(dp, fstrFilter);
                    continue;
                }
                // else{
                // System.out.print("dp valid: ");
                // dp.print();
                // }
                if (filter != null && filter.dropData(dp)) {
                    // System.out.print("dp dropped: "); dp.print();
                    writeDataPoint(dp, fstrFilter);
                    continue;
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
            } // of while
              // System.out.println("... end of while");
        } // for file
    }

    // private void createDatalist(Element elem) {
    // // list of elements in tag DATA
    // List<Element> list = elem.getChildren();
    // for (Element f : list) {
    // if (f.getName() == XmlElements.DatafileElem) {
    // elemDataList.add(f); // whether filename or single datapoint...
    // System.out.print(" add to list ... ");
    // XmlUtils.outputElement(f);
    // }
    // }
    //
    // // read further elements from file:
    // if (XmlUtils.containsAttribute(elem, XmlElements.DatalistFilename)) {
    // String filename = XmlUtils.getStringValue(elem, XmlElements.DatalistFilename);
    // String basename = "";
    // if (XmlUtils.containsAttribute(elem, XmlElements.DatalistBasename)) {
    // basename = XmlUtils.getStringValue(elem, XmlElements.DatalistBasename);
    // }
    // // arne neu:
    // String alternativePath = "";
    // if (XmlUtils.containsAttribute(elem, XmlElements.DatalistAlternativePath)) {
    // alternativePath = XmlUtils.getStringValue(elem, XmlElements.DatalistAlternativePath);
    // if (!alternativePath.endsWith(File.separator)) {
    // alternativePath += File.separator;
    // System.out.printf(" added %s to path \"%s\" %n", File.separator, alternativePath);
    // }
    // // die Belegung ist dort "?"
    // // siehe gleiches Verfahren auch in DataRepository
    // if (alternativePath.startsWith("~")) {
    // if (FileUtils.dirExists(FileUtils.homeDirectory(), "check if user.home exits")) {
    // String newPath = alternativePath.substring(1, alternativePath.length());
    // alternativePath = FileUtils.homeDirectory() + newPath;
    // System.out.printf(" replace ~ with user.home = %s --> path = \"%s\" %n",
    // FileUtils.homeDirectory(), alternativePath);
    // } else {
    // System.err.printf(
    // "%nWARNING !!! user.home = %s is not set correctly (directory does not exist)! %n%n",
    // FileUtils.homeDirectory());
    // // System.exit(0);
    // }
    // }
    //
    // }
    //
    // createElementsFromFile(filename, basename, alternativePath);
    // }
    //
    // // read further elements from file given by a filtername:
    // if (XmlUtils.containsAttribute(elem, XmlElements.DatalistFiltername)) {
    // String filtername = XmlUtils.getStringValue(elem, XmlElements.DatalistFiltername);
    // createElementsFromFiltername(filtername);
    // }
    // }

    // private void createElementsFromFile(String filename, String basename, String path) {
    // // alternativer suchpfad
    // if (FileUtils.fileExists(absolutePath + filename, " first check for file in working directory ... found file")) {
    // System.out.printf(" ... okay, detector file \"%s\" found in working directory ... %n", absolutePath
    // + filename);
    // filename = absolutePath + filename;
    // if (path.length() > 0) {
    // System.out.printf(" ... and ignore alternative search path %s %n", path);
    // }
    // } else {
    // filename = path + filename;
    // System.out.printf(" second, check for file in alternative path = %s %n", path);
    // if (!FileUtils.fileExists(filename, "")) {
    // System.out.printf(" ... detector file \"%s\" also not found in alternative directory %n", filename);
    // }
    // }
    // // XmlReader xmlReader= new XmlReader(this.absolutePath/*+File.separator*/+filename);
    // XmlReader xmlReader = new XmlReader(filename);
    // List<Element> elemList = xmlReader.eRoot.getChildren();
    // for (Element elem : elemList) {
    // if (basename != "") {
    // // complete "filename" attribute
    // String fileEnding = XmlUtils.getStringValue(elem, XmlElements.DatalistFilenameCompletion);
    // String fName = basename + fileEnding;
    // System.out.println("filename completion: \"" + fName + "\"");
    // elem.setAttribute(XmlElements.DatalistFilename, fName);
    // if (path.length() > 0) {
    // elem.setAttribute(XmlElements.DatalistAlternativePath, path);
    // }
    // }
    // XmlUtils.outputElement(elem);
    // this.elemDataList.add(elem);
    // }
    // }

    // private void createElementsFromFiltername(String filtername) {
    // System.out.println("createElementsFromFiltername: \"" + filtername + "\" using Java's regex ... ");
    //
    // File dir = new File(".");
    //
    // File[] files = dir.listFiles();
    // if (files != null) {
    // for (int i = 0; i < files.length; i++) {
    // // Get filename of file or directory
    // if (files[i].isFile()) {
    // String filename = files[i].getName();
    // Pattern myPattern = Pattern.compile(filtername);
    // CharSequence cs = filename.subSequence(0, filename.length());
    // if (myPattern.matcher(cs).matches()) {
    // System.out.println("filtername = " + filtername + " --> matches filename = \"" + filename
    // + "\"");
    // Element elem = new Element(XmlElements.DataElem);
    // elem.setAttribute(XmlElements.DatafileFilename, filename);
    // this.elemDataList.add(elem); // add to list ...
    // }
    // }
    // }
    // }
    // }

    private void writeRepository() {
        System.out.println("DataRepository: write to file = \"" + filename + "\"");
        try {
            PrintWriter fstr = new PrintWriter(new BufferedWriter(new FileWriter(filename, false)));
            fstr.printf(Locale.US, "# some header information \n");
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
            writer.printf(Locale.US, "%.5e  %.5e  %.5e  %.5e  %.5e  %.5e  %.5e   %s\n", dp.x(), dp.t(), dp.v(), dp.q(),
                    dp.rho(), dp.weight(), dp.occ(), FormatUtils.getFormatedTime(dp.t()));
        }
    }

}
