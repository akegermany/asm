package de.akesting;

import java.io.File;
import java.util.Locale;

import org.joda.time.DateTime;
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
            applyAsm(cmdLine, inputData, dataRep);
        }

        System.out.println("done.");
    }

    private void processCaliforniaData(ReadCommandline cmdLine, AdaptiveSmoothingMethodProject inputData) {
        InputCalifornia inputCalifornia = inputData.getInputCalifornia();
        CaliforniaInfrastructure californiaInfrastructure = new CaliforniaInfrastructure(inputCalifornia);
        CaliforniaDataReader reader =  new CaliforniaDataReader(inputCalifornia);
        
        for(Freeway freeway : inputCalifornia.getFreeways().getFreeway()){
            
            FreewayStretch freewayStretch = californiaInfrastructure.getFreewayStretch(freeway.getName());
            // TODO perhaps faster: open files and read for all freeways simultaneously
            	
        	// iterate over each day within interval  
            for (DateTime day = reader.getfromTime(); day.isBefore(reader.gettoTime()); day = day.plusDays(1))
            {
            DataRepository dataRep = reader.loadData(freewayStretch,day);
            applyAsm(cmdLine, inputData, dataRep);
            } // end file iteration
        }
        
    }

    private void applyAsm(ReadCommandline cmdLine, AdaptiveSmoothingMethodProject inputData, DataRepository dataRep) {
        DataView dataView = new DataView(inputData.getVirtualGrid(), dataRep);

        // TODO handling of many California freeways in batch
        OutputGrid outputGrid = new OutputGrid(cmdLine.defaultOutFilename(), inputData.getOutput()
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
            TrajectoryIntegration traj = new TrajectoryIntegration(cmdLine.defaultOutFilename(), inputData.getOutput()
                    .getTrajectories(), outputGrid);
        }

        // Timeseries from ASM fields:
        if (inputData.getOutput().isSetTimeSeriesOutput()) {
            TimeSeries tsOut = new TimeSeries(cmdLine.defaultOutFilename(),
                    inputData.getOutput().getTimeSeriesOutput(), outputGrid);
        }
        // LocationSeries from ASM fields:
        if (inputData.getOutput().isSetLocationSeriesOutput()) {
            LocationSeries lsOut = new LocationSeries(cmdLine.defaultOutFilename(), inputData.getOutput()
                    .getLocationSeriesOutput(), outputGrid);
        }
    }

}
