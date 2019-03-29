package de.akesting;

import de.akesting.autogen.AdaptiveSmoothingMethodProject;
import de.akesting.data.AdaptiveSmoothingMethod;
import de.akesting.data.DataRepository;
import de.akesting.data.DataView;
import de.akesting.output.LocationSeries;
import de.akesting.output.OutputGrid;
import de.akesting.output.TimeSeries;
import de.akesting.output.TrajectoryIntegration;
import de.akesting.xml.XmlInputLoader;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class AdaptiveSmoothingMethodMain {

    public static void main(String[] args) throws IOException {
        new AdaptiveSmoothingMethodMain().run(args);
    }

    public void run(String[] args) throws IOException {
        // set the default locale
        Locale.setDefault(Locale.US);

        // input handling
        ReadCommandline cmdLine = new ReadCommandline(args);
        File xmlFile = new File(cmdLine.xmlFilename());
        AdaptiveSmoothingMethodProject inputData = XmlInputLoader.getInputData(xmlFile);

        DataRepository dataRep = new DataRepository(cmdLine.defaultReposFilename(),
                cmdLine.defaultFilteredDataFilename(), inputData.getInput(), cmdLine.absolutePath());
        applyAsm(cmdLine, inputData, dataRep);

        System.out.println("done.");
    }

    // Apply ASM with defaultOutputName
    private void applyAsm(ReadCommandline cmdLine, AdaptiveSmoothingMethodProject inputData, DataRepository dataRep) throws IOException {
        applyAsm(cmdLine, inputData, dataRep, cmdLine.defaultOutFilename());
    }

    private void applyAsm(ReadCommandline cmdLine, AdaptiveSmoothingMethodProject inputData, DataRepository dataRep, String outFileName) throws IOException {
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

        if (outputGrid.withFileOutput()) {
            outputGrid.write(dataRep);
        }

        if (inputData.getOutput().isSetTrajectories()) {
            new TrajectoryIntegration(outFileName, inputData.getOutput()
                    .getTrajectories(), outputGrid);
        }

        // Timeseries from ASM fields:
        if (inputData.getOutput().isSetTimeSeriesOutput()) {
            new TimeSeries(outFileName,
                    inputData.getOutput().getTimeSeriesOutput(), outputGrid);
        }
        // LocationSeries from ASM fields:
        if (inputData.getOutput().isSetLocationSeriesOutput()) {
            new LocationSeries(outFileName, inputData.getOutput()
                    .getLocationSeriesOutput(), outputGrid);
        }
    }

}
