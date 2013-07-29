package de.akesting;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import com.google.common.collect.Lists;

import de.akesting.autogen.AdaptiveSmoothingMethodProject;
import de.akesting.autogen.Freeway;
import de.akesting.autogen.InputCalifornia;
import de.akesting.california.CaliforniaDataReader;
import de.akesting.california.CaliforniaInfrastructure;
import de.akesting.california.FreewayStretch;
import de.akesting.data.AdaptiveSmoothingMethod;
import de.akesting.data.DataRepository;
import de.akesting.data.DataView;
import de.akesting.output.LocationSeries;
import de.akesting.output.OutputGrid;
import de.akesting.output.TimeSeries;
import de.akesting.output.TrajectoryIntegration;
import de.akesting.xml.XmlInputLoader;

public class AdaptiveSmoothingMethodMain {

	public static final String[][] LANETYPE_GROUPS = {{ "ML" }, { "HOV" }};
	//public static final String[][] LANETYPE_GROUPS = {{ "ML" }, { "HOV" }, { "ML", "HOV" }};
	public static final String FILE_DATEFORMAT= "yyyy_MM_dd";
	
	public Long loadTimeTotal = new Long(0);
	public Long smoothWriteBackTimeTotal = new Long(0);
	
	
	public static void main(String[] args) {
        new AdaptiveSmoothingMethodMain().run(args);
    }
    
    public void run(String[] args) {
        // Set the default locale
        Locale.setDefault(Locale.US);

        // input handling
        ReadCommandline cmdLine = new ReadCommandline(args);
        File xmlFile = new File(cmdLine.xmlFilename());
        AdaptiveSmoothingMethodProject inputData = XmlInputLoader.getInputData(xmlFile);

        // handling two different input formats
        if (inputData.isSetInputCalifornia()) {
           	processCaliforniaData(cmdLine, inputData);            	
        } else {
            DataRepository dataRep = new DataRepository(cmdLine.defaultReposFilename(),
                    cmdLine.defaultFilteredDataFilename(), inputData.getInput(), cmdLine.absolutePath());
            // TODO Intended? Might break! [SM]
            applyAsm(cmdLine, inputData, dataRep);
        }        
        System.out.println("done.");
    }

    private void processCaliforniaData(final ReadCommandline cmdLine, final AdaptiveSmoothingMethodProject inputData) {
        InputCalifornia inputCalifornia = inputData.getInputCalifornia();
        CaliforniaInfrastructure californiaInfrastructure = new CaliforniaInfrastructure(inputCalifornia);
        final CaliforniaDataReader reader =  new CaliforniaDataReader(inputCalifornia);
    	
        // Initialize thread pool
        ExecutorService pool = Executors.newFixedThreadPool(cmdLine.getNumThreads());
    	
        // Get pairs of freeway and stretches for fast lookup
        final Map<Freeway, FreewayStretch> freewayStretches = Collections.synchronizedMap(new HashMap<Freeway, FreewayStretch>());        
        for(Freeway freeway : inputCalifornia.getFreeways().getFreeway()){
        	freewayStretches.put(freeway, californiaInfrastructure.getFreewayStretch(freeway.getName()));
        }
        // Get list of all districts for selected freeways (positive filter)
		final Set<String> allDistricts = new HashSet<String>();
		for (FreewayStretch fs : freewayStretches.values()) allDistricts.addAll(Lists.newArrayList(fs.getDistricts()));
		
		long startTimeOverall = System.currentTimeMillis();
    	// iterate over each day within interval  
        for (DateTime day = reader.getfromTime(); day.isBefore(reader.gettoTime()); day = day.plusDays(1)) {
        	final String datetime = day.toString(DateTimeFormat.forPattern(FILE_DATEFORMAT));
        	// Start/Enqueue new thread for given day
			// TODO Consider different thread types for division of labor (read vs asm) [SM]
        	pool.execute(new Runnable() {
				public void run() {
					long startTime = System.currentTimeMillis();
					// Get data repositories for each freeway and lane group (list of lists)
					// Only read files once for all freeways at a time
        			HashMap<Freeway, HashMap<String[], DataRepository>> dataRepos = reader.loadRepos(freewayStretches, datetime, LANETYPE_GROUPS, allDistricts);
        			long loadTime = System.currentTimeMillis();
        			// iterate over each freeway...
					for (Entry<Freeway, HashMap<String[], DataRepository>> freewayRepos : dataRepos.entrySet()) {
						// Construct file name along the way
						String freewayName = freewayRepos.getKey().getName();
						File freewayDir = new File(cmdLine.absolutePath(), freewayName);
						// Create freeway sub directory (synchronized)
						if (!AdaptiveSmoothingMethodMain.createDir(freewayDir)) {
							System.err.println("Error creating dir: "+freewayName);
							continue;
						}
						// ...and each lane group for this freeway
						for (Entry<String[], DataRepository>  laneGroupRepo : freewayRepos.getValue().entrySet()) {
							DataRepository dataRepository = laneGroupRepo.getValue();
							String laneGroupName = "";
							for (String lane : laneGroupRepo.getKey()) laneGroupName += lane;
							// Write back data repository
							// TODO Is this really necessary? [SM]
							File repoOutputFile = new File(freewayDir, datetime + "-loadedData-" + freewayName + "-" + laneGroupName + ".dat");
							if (!(repoOutputFile).exists()) {
								dataRepository.writeRepository(repoOutputFile);
							} else {
								System.out.println("Skip existing data repository: "+repoOutputFile.getParentFile().getName()+File.separator+repoOutputFile.getName());
							}
							// Do adaptive smoothing
							File asmOutputFile = new File(freewayDir, datetime + "-" + freewayName + "-" + laneGroupName + ".GASM");
							if (!(asmOutputFile).exists()) {
								applyAsm(cmdLine, inputData, dataRepository, asmOutputFile.getAbsolutePath());
							} else {
								System.out.println("Skip existing ASM: "+asmOutputFile.getParentFile().getName()+File.separator+asmOutputFile.getName());
							}							
						}
					}					
					long endTime = System.currentTimeMillis();
					// Performance measurement
					synchronized(loadTimeTotal) {
						loadTimeTotal += (loadTime - startTime); 
					}
					synchronized(smoothWriteBackTimeTotal) {
						smoothWriteBackTimeTotal += (endTime - loadTime);
					}					
				}
        	});
        }
    	pool.shutdown();
    	try {
    		// Basically wait for all threads to complete their work
    		pool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    	} catch (InterruptedException e) {
    		System.exit(1);
    	}
    	long endTimeOverall = System.currentTimeMillis();
    	System.out.println("run time: " + (endTimeOverall-startTimeOverall)/1000 + "s");
    	// Return aggregated thread time (!= run time).
        System.out.println("threads: "+cmdLine.getNumThreads()+" | loadTimeTotal: "+(loadTimeTotal / 1000)+"s  | smoothWriteBackTimeTotal: "+(smoothWriteBackTimeTotal / 1000)+"s");    	
    }
    
    // Apply ASM with defaultOutputName
    private void applyAsm(ReadCommandline cmdLine, AdaptiveSmoothingMethodProject inputData, DataRepository dataRep) {
    	applyAsm(cmdLine, inputData, dataRep, cmdLine.defaultOutFilename());
    }

	private void applyAsm(ReadCommandline cmdLine, AdaptiveSmoothingMethodProject inputData, DataRepository dataRep, String outFileName) {
        DataView dataView = new DataView(inputData.getVirtualGrid(), dataRep);

        // TODO handling of many California freeways in batch
        OutputGrid outputGrid = new OutputGrid(outFileName, inputData.getOutput()
                .getSpatioTemporalContour(), dataRep);

        AdaptiveSmoothingMethod asmAlgo = new AdaptiveSmoothingMethod(inputData.getParameterASM());
        if (inputData.getParameterASM().isWithKerneltest()) {
            System.out.println(" GeneralASM: kernelTestOutput ... ");
            asmAlgo.kernelTestOutput(cmdLine.defaultKernelFilename(), outputGrid);
            System.out.println(" GeneralASM: kernelTestOutput ... finished ...");
            System.exit(0);
        }

        // Smoothing kernel: do calculations
        asmAlgo.doSmoothing(dataView, outputGrid);

        if (outputGrid.withFileOutput())
            outputGrid.write(dataRep);

        if (inputData.getOutput().isSetTrajectories()) {
            TrajectoryIntegration traj = new TrajectoryIntegration(outFileName, inputData.getOutput()
                    .getTrajectories(), outputGrid);
        }

        // Timeseries from ASM fields:
        if (inputData.getOutput().isSetTimeSeriesOutput()) {
            TimeSeries tsOut = new TimeSeries(outFileName,
                    inputData.getOutput().getTimeSeriesOutput(), outputGrid);
        }
        // LocationSeries from ASM fields:
        if (inputData.getOutput().isSetLocationSeriesOutput()) {
            LocationSeries lsOut = new LocationSeries(outFileName, inputData.getOutput()
                    .getLocationSeriesOutput(), outputGrid);
        }
    }
	
	public static synchronized boolean createDir(File dir) {
		try {
			if (!dir.exists()) dir.mkdir();
			return dir.isDirectory();			
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

}
