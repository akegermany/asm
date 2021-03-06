package de.akesting;

import java.io.File;

import de.akesting.utils.FileUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.StringUtils;

public final class ReadCommandline {

    private static final String PROGRAM_NAME = "asm";
    private static final String COPYRIGHT = "Copyright " + '\u00A9' + " by Arne Kesting <mail@akesting.de>";

    private String filename = "";
    private String workingDirectory = "";

    private String dataInputFilename = "";

    public String xmlFilename() {
        return filename;
    }

    public String absolutePath() {
        return workingDirectory;
    }

    private String getWithoutEnding(String name) {
        return name.substring(0, name.lastIndexOf('.'));
    }

    // TODO also allow for .gz
    public String defaultOutFilename() {
        String f = getWithoutEnding(filename);
        // f += ".out.csv.gz";
        f += ".out.csv";
        return f;
    }

    public String defaultReposFilename() {
        String f = getWithoutEnding(filename);
        f += ".rep_dat";
        return f;
    }

    public String defaultFilteredDataFilename() {
        String f = getWithoutEnding(filename);
        f += ".filtered_dat";
        return f;
    }

    public String defaultKernelFilename() {
        String f = getWithoutEnding(filename);
        f += ".kernel_dat";
        return f;
    }

    // check if not empty
    public String getDataInputFilename() {
        return dataInputFilename;
    }


    public String defaultTraveltimeFilename() {
        return defaultOutFilename() + ".tt";
    }

    public String defaultConsumptionFilename() {
        return defaultOutFilename() + ".fuel";
    }

    private Options options;

    // constructor:
    public ReadCommandline(String[] args) {
        System.out.println(COPYRIGHT);

        System.out.println(" ### current working directory is =" + System.getProperty("user.dir"));

        // create the Options (definition of options)
        options = new Options();
        options.addOption("h", "help", false, "print this message");
        options.addOption("f", "file", true, "project (xml) file name ");
        options.addOption("a", "aggregation", true, "data aggregation ");
        options.addOption("i", "input", true, "input file overwriting xml ");

        parseAndInterrogate(args);

        File file = new File(filename);
        file = file.getAbsoluteFile();
        File parentPath = file.getParentFile();
        if (parentPath != null) {
            workingDirectory = parentPath.toString() + File.separator;
        }
        filename = workingDirectory + file.getName();
        System.out.println("projectDirectory = " + workingDirectory);
        System.out.println("projectFilename  = " + filename);
        // check if file exist on filesystem:
        System.out.println("check for xml input file " + xmlFilename());
        if (!FileUtils.fileExists(xmlFilename())) {
            System.err.println("no xml inputfile " + xmlFilename());
            System.exit(-1);
        }
        if (StringUtils.isNotBlank(dataInputFilename)) {
            System.out.println("data input file overwriting xml = " + dataInputFilename);
        }

    }

    // issue an help message if first command-line parameter help or -help
    // command line options
    private void parseAndInterrogate(String[] args) {
        // create the command line parser
        CommandLineParser parser = new PosixParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);

            if (line.hasOption('h')) {
                help();
            }
            if (line.hasOption('f')) {
                filename = line.getOptionValue('f');
            }
            if (line.hasOption('i')) {
                dataInputFilename = line.getOptionValue('i');
            }
        } catch (ParseException exp) {
            System.err.println("Unexpected exception:" + exp.getMessage());
        }

    }

    private void help() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(PROGRAM_NAME + COPYRIGHT, options);
        System.exit(0);
    }

}
