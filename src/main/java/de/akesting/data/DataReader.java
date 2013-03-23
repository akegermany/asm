package de.akesting.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StreamTokenizer;

import com.google.common.base.Preconditions;

import de.akesting.autogen.Data;
import de.akesting.autogen.Format;
import de.akesting.utils.FileUtils;

public class DataReader {

    private final Data data;
    private final Format dataFormat;

    private BufferedReader fileReader = null;
    private StreamTokenizer tokenizer = null;
    private int token;

    private double position = -1;
    private double weight;

    String absPath;
    String alternativePath = "";

    public DataReader(Data dataEntryConfig, Format format, double weight, String absPath) {
        this.data = Preconditions.checkNotNull(dataEntryConfig);
        this.dataFormat = Preconditions.checkNotNull(format);
        this.absPath = Preconditions.checkNotNull(absPath);
        this.weight = weight;

        // position is required whether directly or by column
        if (!format.isSetColPosition()) {
            position = 1000 * dataEntryConfig.getPosKm();
            if (format.isSetPositionOffsetKm()) {
                position += 1000 * format.getPositionOffsetKm();
            }
            System.out.println("position=" + position);
        }

        closeFileReader();

        // arne: alternativer suchpfad
        if (dataEntryConfig.isSetAlternativePath()) {
            this.alternativePath = dataEntryConfig.getAlternativePath();
            if (!alternativePath.endsWith(File.separator)) {
                alternativePath += File.separator;
                System.out.printf("added FileSeparator %s to end of path \"%s\" %n", File.separator, alternativePath);
            }

            if (alternativePath.startsWith("~")) {
                if (FileUtils.dirExists(FileUtils.homeDirectory(), "check if user.home exits")) {
                    String newPath = alternativePath.substring(1, alternativePath.length());
                    alternativePath = FileUtils.homeDirectory() + newPath;
                    System.out.printf(" replace ~ with user.home = %s --> path = \"%s\" %n", FileUtils.homeDirectory(),
                            alternativePath);
                } else {
                    System.err.printf(
                            "WARNING !!! user.home = %s is not set correctly (directory does not exist)! %n%n",
                            FileUtils.homeDirectory());
                    // System.exit(0);
                }
            }
        }

        if (dataEntryConfig.isSetFilename()) {
            String filename = dataEntryConfig.getFilename();
            String completeFilename = absPath + filename;
            if (FileUtils.fileExists(completeFilename, " first check for file in working directory ... found file")) {
                System.out
                        .printf(" ... okay, detector file \"%s\" found in working directory ... %n", completeFilename);
                if (alternativePath.length() > 0) {
                    System.out.printf(" ... and ignore alternative search path %s %n", alternativePath);
                }
            } else {
                filename = alternativePath + filename;
                System.out.printf(" second, check for file in alternative path = %s %n", alternativePath);
            }
            openFile(filename);
        }

    }

    private void closeFileReader() {
        if (fileReader != null) {
            try {
                fileReader.close();
                fileReader = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isReady() {
        try {
            if (fileReader != null && fileReader.ready() && token != StreamTokenizer.TT_EOF) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void openFile(String filename) {
        // String absFilename = this.absPath + File.separator + filename;
        System.out.println("DataReader.openFile: open file \"" + filename + "\"");
        try {
            fileReader = new BufferedReader(new FileReader(filename));
            tokenizer = new StreamTokenizer(fileReader);
            // Do NOT ignore the end-of-line character:
            tokenizer.eolIsSignificant(true);
            tokenizer.commentChar('#');
            tokenizer.commentChar('%');
            tokenizer.slashSlashComments(true); // detects "//"
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public Datapoint readDatapoint() {
        if (fileReader == null) {
            throw new IllegalStateException("file reader not available");
        }
        return readFromFile();
    }

    // private Datapoint singleDataPoint() {
    // System.out.print(" read singleDataPoint: ");
    // Datapoint dp = new Datapoint();
    // dp.set_weight(weight);
    // dp.set_x(position);
    // dp.set_t(XmlUtils.getDoubleValue(dataElem, XmlElements.DatafileT_h) * 3600.);
    // dp.set_v(XmlUtils.getDoubleValue(dataElem, XmlElements.DatafileV_kmh) / 3.6);
    // if (XmlUtils.containsAttribute(dataElem, XmlElements.DatafileFlow_invh)) {
    // dp.set_q(XmlUtils.getDoubleValue(dataElem, XmlElements.DatafileFlow_invh) / 3600.);
    // }
    // if (XmlUtils.containsAttribute(dataElem, XmlElements.DatafileDensity_invkm)) {
    // dp.set_rho(XmlUtils.getDoubleValue(dataElem, XmlElements.DatafileDensity_invkm) / 1000.);
    // }
    // if (XmlUtils.containsAttribute(dataElem, XmlElements.DatafileOccupancy)) {
    // dp.set_occ(XmlUtils.getDoubleValue(dataElem, XmlElements.DatafileOccupancy));
    // }
    // dataElem = null;
    // return dp;
    // }

    // TODO: StreamTokenizer cannot handle exp-number formats --> better parser
    private Datapoint readFromFile() {
        Datapoint dp = new Datapoint();
        dp.set_weight(weight);
        dp.set_x(position);
        try {
            token = tokenizer.nextToken();
            int col = 0; // col counter
            while (token != StreamTokenizer.TT_EOL && token != StreamTokenizer.TT_EOF) {
                switch (token) {
                case StreamTokenizer.TT_NUMBER:
                    col++;
                    // System.out.println("token ="+token+" nval="+tokenizer.nval);
                    if (col == dataFormat.getColTime()) {
                        dp.set_t(tokenizer.nval * dataFormat.getFactor2S() + 3600 * dataFormat.getTimeOffsetH());
                    } else if (dataFormat.isSetColPosition() && col == dataFormat.getColPosition()) {
                        // System.out.printf("format.factor2m()=%.2f  nval=%.2f  %n", format.factor2m(), tokenizer.nval);
                        dp.set_x(tokenizer.nval * dataFormat.getFactor2M() + 1000 * dataFormat.getPositionOffsetKm());
                    } else if (col == dataFormat.getColSpeed()) {
                        dp.set_v(tokenizer.nval * dataFormat.getFactor2Ms());
                    } else if (dataFormat.isSetColFlow() && col == dataFormat.getColFlow()) {
                        dp.set_q(tokenizer.nval * dataFormat.getFactor2Invs());
                    } else if (dataFormat.isSetColDensity() && col == dataFormat.getColDensity()) {
                        dp.set_rho(tokenizer.nval * dataFormat.getFactor2Invm());
                    } else if (dataFormat.isSetColOccupancy() && col == dataFormat.getColOccupancy()) {
                        dp.set_occ(tokenizer.nval);
                    }
                    break;
                case StreamTokenizer.TT_WORD:
                    if (true) {
                        System.out.println("ParseError in getColumnOfNumbers:" + " Don't know the token: "
                                + tokenizer.sval + ". Exit now!");
                    }
                    break;
                default:
                    // System.out.println("\nOther token type: \nnextTok=" + token + "\ntokenizer.ttype= "
                    // + tokenizer.ttype + "\ntokenizer.sval=" + tokenizer.sval + "\ntokenizer.toString()="
                    // + tokenizer.toString() + " token==EOF?" + (token == StreamTokenizer.TT_EOF) + " token==EOL?"
                    // + (token == StreamTokenizer.TT_EOL));
                    // System.exit(-1);
                }
                token = tokenizer.nextToken();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dp;
    }

    boolean is_valid_dataline(String line) {
        return (!line.startsWith("#") && !line.startsWith("%"));
    }
}
