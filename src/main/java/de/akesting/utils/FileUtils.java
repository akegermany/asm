package de.akesting.utils;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FileUtils {

    private static final Logger LOG = LoggerFactory.getLogger(FileUtils.class);

    public static Writer getWriter(String filename) throws IOException {
        if (filename.endsWith(".gz")) {
            File file = new File(filename);
            return new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(file)), "UTF-8");
        }
        return new PrintWriter(new BufferedWriter(new FileWriter(filename, false)));
    }

    public static BufferedReader getReader(String filename) {
        try {
            return new BufferedReader(new FileReader(filename));
        } catch (Exception e) {
            LOG.error("Cannot open file={} ", filename);
            LOG.error("error: ", e);
        }
        return null;
    }

    public static String homeDirectory() {
        return System.getProperty("user.home");
    }

    public static boolean fileExists(String filename) {
        File file = new File(filename);
        return file.exists() && file.isFile();
    }

    public static boolean dirExists(String path) {
        File file = new File(path);
        return file.exists() && file.isDirectory();
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
        if (!dirExists(dirName)) {
            return;
        }
        File dir = new File(dirName);
        boolean success = deleteDir(dir);
        if (!success) {
            System.err.println("deleteDir: cannot delete directory " + dirName);
            System.err.println("exit now!!!");
            System.exit(-1);
        }
    }

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
        if (file != null) {
            for (int i = 0; i < file.length; i++) {
                // System.out.println("********* test = "+file[i]);
                deleteFile(file[i], "deleteFileList with regex = " + regex);
            }
        }
    }

}
