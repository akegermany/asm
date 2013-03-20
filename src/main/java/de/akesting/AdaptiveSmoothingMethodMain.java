package de.akesting;

import java.io.File;
import java.util.Locale;

import de.akesting.autogen.AdaptiveSmoothingMethodProject;
import de.akesting.data.AdaptiveSmoothingMethod;
import de.akesting.data.DataRepository;
import de.akesting.data.DataView;
import de.akesting.output.LocationSeries;
import de.akesting.output.OutputGrid;
import de.akesting.output.TimeSeries;
import de.akesting.output.TrajectoryIntegration;
import de.akesting.output.Traveltime;
import de.akesting.xml.XmlInputLoader;

public class AdaptiveSmoothingMethodMain {

    public void run(String[] args) {
        // Set the default locale to pre-defined locale
        Locale.setDefault(Locale.US);

        // input handling
        ReadCommandline cmdLine = new ReadCommandline(args);
        File xmlFile = new File(cmdLine.xmlFilename());
        AdaptiveSmoothingMethodProject inputData = XmlInputLoader.getInputData(xmlFile);

        DataRepository dataRep = new DataRepository(cmdLine.defaultReposFilename(),
                cmdLine.defaultFilteredDataFilename(), inputData.getInput(), cmdLine.absolutePath());

        DataView dataView = new DataView(inputData.getVirtualGrid(), dataRep);

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

        if (inputData.getOutput().isSetTraveltime()) {
            Traveltime tt = new Traveltime(cmdLine.defaultTraveltimeFilename(), inputData.getOutput().getTraveltime(),
                    outputGrid);
        }

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

    public static void main(String[] args) {
        new AdaptiveSmoothingMethodMain().run(args);
    }
}
