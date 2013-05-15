package de.akesting.california;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;

import de.akesting.autogen.InputCalifornia;
import de.akesting.data.DataRepository;
import de.akesting.data.Datapoint;

// hard-coded column numbers not very beautiful ;-)
public class CaliforniaDataReader {

    private static final String TIME_FORMAT = "MM/dd/YYYY' 'HH:mm:ss";
    private static final double MILES_PER_H_TO_M_PER_S = 0.44704;
    
    private final DateTime fromTime;
    private final DateTime toTime;
    private final Interval interval;

    private final File dataPath;

    public CaliforniaDataReader(InputCalifornia input) {
        fromTime = LocalDateTime.parse(input.getFrom(), DateTimeFormat.forPattern(TIME_FORMAT)).toDateTime(
                DateTimeZone.UTC);
        toTime = LocalDateTime.parse(input.getTo(), DateTimeFormat.forPattern(TIME_FORMAT))
                .toDateTime(DateTimeZone.UTC);
        interval = new Interval(fromTime, toTime);
        System.out.println("time interval [from, to]=" + interval);

        dataPath = new File(input.getPath());
        if (!dataPath.exists() || !dataPath.isDirectory()) {
            throw new IllegalArgumentException("cannot find path to data=" + dataPath);
        }
    }

    public DataRepository loadData(FreewayStretch freewayStretch) {
        
        System.out.println("************ read data for freeway="+freewayStretch.getFreewayName() +" with stations="+freewayStretch.getStations().size());
        DataRepository dataRepo = new DataRepository();

        // TODO iterate over inputfiles in timeInterval: split per day needed!!! here hard-coded example
        
        for (String district : freewayStretch.getDistricts()) {
            File file = getInputFile(district, "2013_01_01");
            if (!file.exists()) {
                // perhaps one want to just log the error 
                throw new IllegalArgumentException("cannot find data file=" + file);
            }
            System.out.println("read file="+file);
            // parse file line per line
            addDataFromFileParsing(file, freewayStretch, dataRepo);
        }
        dataRepo.setReverseDirection(freewayStretch.isReverseDirection());
        dataRepo.analyzeData();
        
        // TODO quickhack for logging:
        File repoOutputFile = new File("loadedData.dat");
        dataRepo.writeRepository(repoOutputFile);
        return dataRepo;
    }

    private void addDataFromFileParsing(File file, FreewayStretch freewayStretch, DataRepository dataRepo) {
        Scanner scanner = null;
        try {
            scanner = new Scanner(new FileReader(file));
            // first use a Scanner to get each line
            while (scanner.hasNextLine()) {
                String[] line = scanner.nextLine().split(",");
                if (line[0].startsWith("#")) {
                    continue; // ignore comments
                }
                // TODO here further filtering possible: laneTypes etc
                String stationId = line[1].trim();
                FreewayStation station = freewayStretch.getStation(stationId);
                if (station != null){
                    DateTime timeStamp = LocalDateTime.parse(line[0], DateTimeFormat.forPattern(TIME_FORMAT)).toDateTime(
                            DateTimeZone.UTC);
                    if(interval.contains(timeStamp.getMillis())){
                        dataRepo.addDataPoint(createDataPoint(station, timeStamp, line));
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }

    private Datapoint createDataPoint(FreewayStation station, DateTime timestamp, String[] line) {
        Datapoint dp = new Datapoint();
        dp.set_t(TimeUnit.MILLISECONDS.toSeconds(timestamp.getMillis()));
        dp.set_x(station.absolutePostmileMeters());
        dp.set_v(Double.parseDouble(line[11])*MILES_PER_H_TO_M_PER_S);
        // System.out.println("created dp=" + dp + " at time="+timestamp.getMillis());
        return dp;
    }

    private File getInputFile(String district, String timePattern) {
        StringBuilder sb = new StringBuilder();
        sb.append("d");
        if(Integer.parseInt(district)<10){
            sb.append(0);
        }
        sb.append(district);
        sb.append("_text_station_5min_");
        sb.append(timePattern);
        sb.append(".txt");
        File file = new File(dataPath, sb.toString());
        return file;
    }
}
