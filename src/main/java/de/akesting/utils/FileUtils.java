package de.akesting.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FileUtils {

    /** The Constant logger. */
    private final static Logger Logger = LoggerFactory.getLogger(FileUtils.class);

    public static PrintWriter getWriter(String filename) {
        try {
            PrintWriter fstr = new PrintWriter(new BufferedWriter(new FileWriter(filename, false)));
            return fstr;
        } catch (java.io.IOException e) {
            e.printStackTrace();
            Logger.error("Cannot open file ={}", filename);
        }
        return null;
    }

    public static BufferedReader getReader(String filename) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            return reader;
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("Cannot open file={} ", filename);
        }
        return null;
    }

    public static String currentDirectory() {
        return System.getProperty("user.dir");
    }

    public static String homeDirectory() {
        String home = System.getProperty("user.home");
        return home;
    }

    // check for existing file
    public static boolean fileExists(String filename, String msg) {
        File file = new File(filename);
        if (file.exists() && file.isFile()) {
            return (true);
        }
        return (false);
    }

    // check if directory exists (in fact the same as file)
    public static boolean dirExists(String path, String msg) {
        File file = new File(path);
        if (file.exists() && file.isDirectory()) {
            return (true);
        }
        return (false);
    }

    // create directory
    public static void createDir(String path, String msg) {
        File file = new File(path);
        if (dirExists(path, msg)) {
            return;
        }
        System.out.println(msg + ": create directory \"" + path + "\"");
        boolean success = file.mkdir();
        if (!success) {
            System.err.println("createDir: cannot create directory " + path);
            System.err.println("msg from calling class" + msg);
            System.err.println("exit now!!!");
            System.exit(-5);
        }
    }

    // delete existing file
    public static void deleteFile(String filename, String msg) {
        File file = new File(filename);
        if (file.exists()) {
            System.out.println(msg + ": file\"" + file.getName() + "\" exists!");
            boolean success = file.delete();
            if (success)
                System.out.println("file " + filename + " successfully deleted ...");
        }
    }

    // Deletes all files and subdirectories under dir.
    // Returns true if all deletions were successful.
    // If a deletion fails, the method stops attempting to delete and returns false.
    // Deletes all files and subdirectories under dir.
    // Returns true if all deletions were successful.
    // If a deletion fails, the method stops attempting to delete and returns false.
    private static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        // The directory is now empty so delete it
        return dir.delete();
    }

    public static void deleteDir(String dirName) {
        if (!dirExists(dirName, "FileUtils...deleteDir..."))
            return;
        File dir = new File(dirName);
        boolean success = deleteDir(dir);
        if (!success) {
            System.err.println("deleteDir: cannot delete directory " + dirName);
            System.err.println("exit now!!!");
            System.exit(-1);
        }
    }

    // returns a String[] of files found in the path applying to the filter string
    public static String[] getFileList(String path, String regex) {
        File dir = new File(path);

        class PatternFilter implements FilenameFilter {
            String regex;

            public PatternFilter(String regex) {
                this.regex = regex;
                System.out.println("PatternFilter has regex  = " + regex);
            }

            @Override
            public boolean accept(File f, String name) {
                // String fString = f.toString().toLowerCase();
                // System.out.println("fString = "+fString);
                // pattern consists of two pattern given by user und ends with a
                // number!!!
                Pattern patternRegex = Pattern.compile(regex);
                Matcher matcher = patternRegex.matcher(name);
                boolean matches = matcher.matches();

                if (matches) {
                    // System.out.println("regex: \"" + regex + "\" matches in  \"" + name + "\"");
                }
                return (matches);
            }
        }

        String[] fileNames = dir.list(new PatternFilter(regex));

        if (fileNames == null) {
            // Either dir does not exist or is not a directory
        } else {
            for (int i = 0; i < fileNames.length; i++) {
                // Get filename of file or directory
                String f = fileNames[i];
                System.out.println("filename = " + f);
            }
        }
        return (fileNames);
    }

    public static void deleteFileList(String path, String regex) {
        String[] file = getFileList(path, regex);
        for (int i = 0; i < file.length; i++) {
            // System.out.println("********* test = "+file[i]);
            deleteFile(file[i], "deleteFileList with regex = " + regex);
        }
    }

}
