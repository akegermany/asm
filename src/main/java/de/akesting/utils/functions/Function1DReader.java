package de.akesting.utils.functions;

import java.io.BufferedReader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.List;

import de.akesting.utils.FileUtils;

public class Function1DReader {

    private Function1DReader() {
    }

    public static List<DataPoint1D> parseDataPoints(String filename, int xCol, int yCol) {
        System.out.println("parseDataPoints from file \"" + filename + "\" and xCol=" + xCol + ", yCol=" + yCol);
        List<DataPoint1D> dataPoints = new ArrayList<DataPoint1D>();
        BufferedReader fileReader = FileUtils.getReader(filename);
        StreamTokenizer tokenizer = new StreamTokenizer(fileReader);
        // Do NOT ignore the end-of-line character:
        tokenizer.eolIsSignificant(true);
        tokenizer.commentChar('%');
        tokenizer.commentChar('#');
        tokenizer.slashSlashComments(true); // detects "//"
        final int initVal = -99;
        int token;
        double xVal, yVal;
        xVal = yVal = initVal;
        int col = 0; // col counter: first colum starts with 1
        try {
            token = tokenizer.nextToken();
            while (token != StreamTokenizer.TT_EOF) {
                switch (token) {
                case StreamTokenizer.TT_NUMBER:
                    col++;
                    // System.out.println("token ="+token+" nval="+tokenizer.nval);
                    if (col == xCol) {
                        xVal = tokenizer.nval;
                    } else if (col == yCol) {
                        yVal = tokenizer.nval;
                    }
                    break;
                case StreamTokenizer.TT_EOL:
                    col = 0; // reset row counter
                    break;
                case StreamTokenizer.TT_WORD:
                    if (false) {
                        System.out.println("ParseError in getColumnOfNumbers:" + " Don't know the token: "
                                + tokenizer.sval + ". Exit now!");
                    }
                    break;
                default:
                    if (false)
                        System.out.println("\nOther token type: \nnextTok=" + token + "\ntokenizer.ttype= "
                                + tokenizer.ttype + "\ntokenizer.sval=" + tokenizer.sval + "\ntokenizer.toString()="
                                + tokenizer.toString());
                    // System.exit(-1);
                } // switch
                if (xVal != initVal && yVal != initVal) {
                    dataPoints.add(new DataPoint1D(xVal, yVal));
                    xVal = yVal = initVal;
                }
                token = tokenizer.nextToken(); // get next token
            }// while
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            fileReader.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error  " + "Cannot close file " + filename + " for reading...");
        }
        return dataPoints;
    }

}
