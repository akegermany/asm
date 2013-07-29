package de.akesting;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import de.akesting.autogen.AdaptiveSmoothingMethodProject;
import de.akesting.california.CaliforniaDataReader;
import de.akesting.california.FreewayStation;
import de.akesting.california.FreewayStretch;

public final class ReadCommandline {

    public static final String programName = "asm";
    public static final String copyrightString = "Copyright " + '\u00A9'
            + " by Arne Kesting <mail@akesting.de> (2008-2013)";

    private String filename = "";
    private String workingDirectory = "";
	// Effectively number of days to work on simultaneously
    private int numThreads = 2;

    public String xmlFilename() {
        return filename;
    }

    public String absolutePath() {
        return workingDirectory;
    }
    
    public int getNumThreads() {
		return numThreads;
	}

	// TODO Reset old defaultOutFilename function [SM]
    public String defaultOutFilename() {
        String f = filename.substring(0, filename.lastIndexOf("."));        
        f += ".out";
        return f;
    }    

    public String defaultReposFilename() {
        String f = filename.substring(0, filename.lastIndexOf("."));
        f += ".rep_dat";
        return f;
    }

    public String defaultFilteredDataFilename() {
        String f = filename.substring(0, filename.lastIndexOf("."));
        f += ".filtered_dat";
        return f;
    }

    public String defaultKernelFilename() {
        String f = filename.substring(0, filename.lastIndexOf("."));
        f += ".kernel_dat";
        return f;
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
        System.out.println(copyrightString);

        System.out.println(" ### current working directory is =" + System.getProperty("user.dir"));

        // create the Options (definition of options)
        options = new Options();
        options.addOption("h", "help", false, "print this message");
        options.addOption("f", "file", true, "project (xml) file name ");
        options.addOption("t", "numthreads", true, "number of worker threads ");

        parseAndInterrogate(args);

        File file = new File(filename);
        file = file.getAbsoluteFile();
        File parentPath = file.getParentFile();
        if (parentPath != null)
            workingDirectory = parentPath.toString() + File.separator;
        // OLD: filename = file.getName();
        filename = workingDirectory + file.getName();
        System.out.println("projectDirectory = " + workingDirectory);
        System.out.println("projectFilename  = " + filename);
        // check if file exist on filesystem:
        System.out.println("check for xml input file " + xmlFilename());
        if (!fileExists(xmlFilename(), "ReadCommandline")) {
            System.err.println("no xml inputfile " + xmlFilename());
            System.exit(-1);
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
            if (line.hasOption('t')) {
            	try {
            		numThreads = Integer.parseInt(line.getOptionValue('t'));
            	} catch (NumberFormatException e) {
            		// Use default value
            	}
            }
        } catch (ParseException exp) {
            System.out.println("Unexpected exception:" + exp.getMessage());
        }

    }

    private void help() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(programName + copyrightString, options);
        System.exit(0);
    }

    private boolean fileExists(String filename, String msg) {
        File file = new File(filename);
        if (file.exists() && file.isFile()) {
            return true;
        }
        return false;
    }
}
