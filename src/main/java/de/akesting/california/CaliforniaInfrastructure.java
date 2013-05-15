package de.akesting.california;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.com.bytecode.opencsv.CSVReader;

import com.google.common.base.Preconditions;

import de.akesting.autogen.InputCalifornia;

public class CaliforniaInfrastructure {
    
    // FreewayId used as key
    private final Map<String, FreewayStretch> freeways = new HashMap<>();
    
    public CaliforniaInfrastructure(InputCalifornia inputCalifornia) {
        Preconditions.checkNotNull(inputCalifornia);
        File infrastructureFile = new File(inputCalifornia.getInfrastructureFile());
        parseInfrastructure(infrastructureFile);
    }

    public FreewayStretch getFreewayStretch(String name){
        Preconditions.checkArgument(freeways.containsKey(name), "unknown freeway="+name);
        return freeways.get(name);
    }
    
    private void parseInfrastructure(File file) {
        final char separator = ',';
        final String comment = "#";
        if(file==null || !file.exists()){
            throw new IllegalArgumentException("cannot find file="+file.toString());
        }
        List<String[]> lines = readData(file, separator);
        int countStations = 0;
        for(String[] line :lines){
            if (line.length != FreewayStation.STATION_COLUMNS.length) {
                System.out.println("expected "+FreewayStation.STATION_COLUMNS.length+" columns, cannot parse data. Ignore line=" + Arrays.toString(line));
                continue;
            }
            if(line[0].startsWith(comment)){
                System.out.println("ignore line. cannot parse station=" + Arrays.toString(line));
                continue;
            }
            FreewayStation station = FreewayStation.parse(line);
            if(station == null){
                System.out.println("ignore line. cannot parse station=" + Arrays.toString(line));
                continue;
            }
            String id = station.freeway();
            FreewayStretch freewayStretch = freeways.get(id);
            if(freewayStretch == null){
                freewayStretch = new FreewayStretch();
                freeways.put(id, freewayStretch);
            }
            freewayStretch.add(station);
            countStations++;
        }
        
        System.out.println("read="+countStations+" stations from input file with lines="+lines.size());
    }
    
    private static List<String[]> readData(File file, char separator) {
        System.out.println("read data from file="+ file.getAbsolutePath());
        List<String[]> myEntries = new ArrayList<>();
        CSVReader reader = null;
        try {
            reader = new CSVReader(new FileReader(file), separator);
            myEntries = reader.readAll();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return myEntries;
    }

    
    
    
    

}
