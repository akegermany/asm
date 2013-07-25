package de.akesting.california;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;

import com.google.common.collect.Lists;

import de.akesting.AdaptiveSmoothingMethodMain;
import de.akesting.ReadCommandline;
import de.akesting.autogen.Freeway;
import de.akesting.autogen.Freeways;
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
    
    public DateTime getfromTime() {
    	return fromTime;
    	}
    
    public DateTime gettoTime() {
    	return toTime;
    	}
    
    /**
     * Get all data repositories for all freeway and lanetypes on a given day 
     * @param freewayStretches Freeways to look for (filter)
     * @param datetime Date/day (filter)
     * @param lanetypeGroups Lanetypes to look for (filter); grouping possible
     * @param allDistricts Preselected list of districts to take into account (filter)
     * @return List of list of data repositories
     */
	public HashMap<Freeway, HashMap<String[], DataRepository>> loadRepos(Map<Freeway, FreewayStretch> freewayStretches, String datetime, String[][] lanetypeGroups, Set<String> allDistricts) {
		// Create return collection
		HashMap<Freeway, HashMap<String[], DataRepository>> dataRepos = new HashMap<Freeway, HashMap<String[], DataRepository>>();		
		for (Entry<Freeway, FreewayStretch> entry : freewayStretches.entrySet()) {
			HashMap<String[], DataRepository> laneGroupRepos = new HashMap<String[], DataRepository>();
			dataRepos.put(entry.getKey(), laneGroupRepos);
		}
		// Create temp hashset of all data repositories to be created
		HashSet<DataRepository> dataReposSet = new HashSet<DataRepository>();
		// Create set of all lanetypes 
		Set<String> lanetypeGroupsFlat = new HashSet<String>();
		for (int i=0; i<lanetypeGroups.length; i++) {
			for (int j=0; j<lanetypeGroups[i].length; j++) {
				lanetypeGroupsFlat.add(lanetypeGroups[i][j]);
			}
		}		
		
		// Data sources are distributed based on districts
		// Iterate over every district file
        for (String district : allDistricts) {
        	// Expected filename {district}_text_station_5min_{datetime}.txt
        	File file = getInputFile(district, datetime);
            if (!file.exists()) { 
            	// Gracefully skip missing data files
                System.err.println("cannot find data file=" + file);
                continue;
            }
            System.out.println("read file="+file);
            
            // Build index of possible freeways for this district. Ignore others while parsing.
            ArrayList<Freeway> searchFreeways = new ArrayList<Freeway>();
            for (Entry<Freeway, FreewayStretch> entry : freewayStretches.entrySet()) {
            	if (Lists.newArrayList(entry.getValue().getDistricts()).contains(district)) {
            		searchFreeways.add(entry.getKey());
            	}
            }
            if (searchFreeways.isEmpty()) {
            	// Don't even look at the input file as no freeways are to be found based on infrastructure data
            	continue;
            }
            // parse file line per line          
            Scanner scanner = null;
            try {
                scanner = new Scanner(new FileReader(file));
                // first use a Scanner to get each line
                outerloop:
                while (scanner.hasNextLine()) {
                    String[] line = scanner.nextLine().split(",");
                    if (line[0].startsWith("#")) {
                        continue; // ignore comments
                    }
                    DateTime timeStamp = LocalDateTime.parse(line[0], DateTimeFormat.forPattern(TIME_FORMAT)).toDateTime(DateTimeZone.UTC);               
					// Make sure that the datapoint lays within stated time interval. Old notation with "interval" does not
					// work here anymore when iterating over several days because each datapoint after "toTime" of the
					// first day still lays within the interval in second day and so on.
					if (timeStamp.getMillisOfDay() <= getfromTime().getMillisOfDay() || timeStamp.getMillisOfDay() >= gettoTime().getMillisOfDay()) {
						continue;
					}
					String stationId = line[1].trim();
					// Create list of freeways that this datapoint will be added to
					ArrayList<Freeway> freeWaysWithStation = new ArrayList<Freeway>();
					String lanetype = null;
					Datapoint datapoint = null;
					// Iterate over all freeways and check if datapoint should be added to corresponding data repository
					for (Freeway freeway : searchFreeways) {
						FreewayStretch fs = freewayStretches.get(freeway);
						FreewayStation station = fs.getStation(stationId);
						if (station != null) {
							lanetype = station.lanetype();
							// only use datapoints in accordance to lanetype_flag, say HOV in first and ML in second run
							if (!lanetypeGroupsFlat.contains(lanetype)) {
								// Do not consider this datapoint for any freeway. Jump to nextline
								// Assumption: station data among freeways are consistent [SM]
								continue outerloop;
							}
							// Create datapoint!
							if (datapoint == null) datapoint = createDataPoint(station, timeStamp, line);
							freeWaysWithStation.add(freeway);
							// TODO If one station belongs to max 1 freeway we can break the loop [SM]
							// break;
						}
					}
					if (freeWaysWithStation.isEmpty() || lanetype == null) {
						// No freeways found for this station or lanetype
						continue;
					}
					// Add datapoint to freeways that list this station 
					for (Freeway freeway : freeWaysWithStation) {
						// For all lanetype groups for this freeway...
						for (String[] group : lanetypeGroups) {
							// If group has found lanetype, create data repository and add datapoint to it
							if (Arrays.asList(group).contains(lanetype)) {
								if (!dataRepos.get(freeway).containsKey(group)) {
									DataRepository dataRepository = new DataRepository();
									// Set direction
									dataRepository.setReverseDirection(freewayStretches.get(freeway).isReverseDirection());
									// Add data repository to overall collection and temporary list
									dataRepos.get(freeway).put(group, dataRepository);
									dataReposSet.add(dataRepository);
								}
								// Add data point
								dataRepos.get(freeway).get(group).addDataPoint(datapoint);
							}
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
        // Set contains all nested data repositories. Iterate and analyze data.
        for (DataRepository dataRepo : dataReposSet) {
        	dataRepo.analyzeData();
        }
		return dataRepos;		
	}

    private void addDataFromFileParsing(File file, FreewayStretch freewayStretch, DataRepository dataRepo, String lanetype_flag) {
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
                      // Make sure that the datapoint lays within stated time interval. Old notation with "interval" does not
                      // work here anymore when iterating over several days because each datapoint after "toTime" of the
                      // first day still lays within the interval in second day and so on.
                      if (timeStamp.getMillisOfDay() > getfromTime().getMillisOfDay() && 
                    	  timeStamp.getMillisOfDay() < gettoTime().getMillisOfDay()) {
                    	  // only use datapoints in accordance to lanetype_flag, say HOV in first and ML in second run
                    	  if(station.lanetype().equals(lanetype_flag)) {    
                    			dataRepo.addDataPoint(createDataPoint(station, timeStamp, line)); 
                    	  }
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
//         System.out.println("created dp=" + dp + " at time="+timestamp.getMillis());
        return dp;
    }

    private File getInputFile(String district, String timePattern) {
        StringBuilder sb = new StringBuilder();
        sb.append("d");
        if(Integer.parseInt(district)<10){
            sb.append(0);
        }
        
        sb.append(district);        
        
        // 30s
        if (ReadCommandline.aggregation.equals("30s")) {
        sb.append("_text_station_raw_");
        sb.append(timePattern);
        sb.append(".txt");
        } 
        
        // 5min
        if (ReadCommandline.aggregation.equals("5min")) {
        sb.append("_text_station_5min_");
        sb.append(timePattern);
        sb.append(".txt");
        }
        
        // 1hour
        else if (ReadCommandline.aggregation.equals("1hour")) {
        	System.out.println(timePattern);
        sb.append("_text_station_hour_");
        sb.append(timePattern.substring(0,timePattern.length()-3));
        sb.append(".txt");
        }
        
        File file = new File(dataPath, sb.toString());
        return file; }

}
