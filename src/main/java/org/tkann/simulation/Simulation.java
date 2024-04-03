/**
 ** Copyright 2024 Trevor Kann, Lujo Bauer, Robert K. Cunningham
 ** 
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 ** 
 **     http://www.apache.org/licenses/LICENSE-2.0
 ** 
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 **/
package simulation;

import Visuals.VisualInterface;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 *This is the head class that acts as a callable object for the main method without the main having to cary any programming load.
 * Also creates visual windows to display outputs
 * @author tkann@andrew.cmu.edu
 * @see AgentHolder
 * @see SceneBuilder
 * @see RSSI_collector
 * @see Metrics
 * @see VisualInterface
 **/

public class Simulation {

    boolean fromFileOrRandom = false;
    String settingFile = "";

    AgentHolder allAgents;
    RSSI_collector RSSI;
    SceneBuilder SceneBuilder;
    public Metrics Metrics;
    VisualInterface visuals;

    int numberOfAgents;
    double xBounds, yBounds;

    int numberOfRounds;
    int currentRound;

    public static boolean visualsON;



    /**
     * the constructor for a simulation without the use of a file to dictate the locations
     * @param numberOfAgents the total number of agents that are in the simulation
     * @param bounds the x/y bounds of the field in which the agents reside
     * @param numberOfRounds the total number of rounds that the simulation is to run for
     */
    public Simulation(int numberOfAgents, double bounds, int numberOfRounds, String settingFile) {
        this.numberOfAgents = numberOfAgents;
        setBothBounds(bounds);
        this.numberOfRounds = numberOfRounds;
        this.currentRound = 0;

        if (settingFile.equalsIgnoreCase("")) {
            fromFileOrRandom = false;
        } else {
            this.settingFile = settingFile;
        }

        /**
         * fixme there's a cyclic dependency issue here when a file is loaded and metrics doesn't know the proper max number of rounds
         */
        Metrics = new Metrics(0, numberOfRounds);

        try {
            RSSI = new RSSI_collector();
        } catch (IOException e) {
            System.out.println("ERROR ON RSSI");
            e.printStackTrace();
        }

        /*
        String directory = "C:\\Users\\trevo\\Documents\\GitHub\\Covid-Graph-ContactTracing\\AgentSimulationFiles\\";
        //String directory = "C:\\Users\\Trevor\\IdeaProjects\\Covid-Graph-ContactTracing\\AgentSimulationFiles\\";
        //String file = "testScene0.txt";
        String settingFile = "SALSA\\output.txt";
        //String file = "coctailPartyDataset\\output.txt";
        //String file = "gridTest.txt";
        settingFile = directory + settingFile;
*/
        if (settingFile == "") {
            fromFileOrRandom = false;
        } else {
            fromFileOrRandom = true;
        }

        if (fromFileOrRandom) {
            try {
                File fromFile = new File(settingFile);
                SceneBuilder = new SceneBuilder(fromFile, Metrics, RSSI);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            SceneBuilder = new SceneBuilder(bounds, numberOfAgents, numberOfRounds, Metrics, RSSI);
        }

        allAgents = SceneBuilder.buildScene();

        if (visualsON) {
            visuals = new VisualInterface(xBounds, yBounds, allAgents, Metrics);
        }
    }

    /**
     * runs the simulation from start to finish
     * @throws InterruptedException
     */
    public void runSim() throws InterruptedException {
        while (SceneBuilder.hasNextPosition()){

            runSingleRoundOfSim();
            //printRoundResults();
            currentRound++;
            Metrics.setRoundNumber(currentRound);
            if (visualsON) {
                visuals.repaintAll();
                TimeUnit.MILLISECONDS.sleep(100);
            }
        }
    }

    public void printRoundResults(){
        boolean verbose = false;
        boolean printNDCF = false;
        boolean printMoreNDCF = false;
        boolean printStats = false;
        boolean printPercents = false;

        ArrayList<Record> RoundList = Metrics.allRecords;
        RoundList = Metrics.filterByRound(RoundList, currentRound);
        if (verbose ||printNDCF || printMoreNDCF|| printStats || printPercents ) {
            for (PartOfRound currentPart : PartOfRound.values()) {
                String header = String.format("timeStep %3d part %30s", currentRound, currentPart.toString());
                System.out.print(header);

                ArrayList<Record> subroundList = Metrics.filterBySubround(RoundList, currentPart);
                if (printNDCF || verbose) {
                    double nDCF = Metrics.getNDCFOfCollection(subroundList, 6);

                    String NDCFString = String.format(", nDCF: %4.2f", nDCF);
                    System.out.print(NDCFString);
                }
                if (printMoreNDCF || verbose) {
                    double PMiss = Metrics.getPMissOfCollection(subroundList, 6);
                    double PFA = Metrics.getPFAOfCollection(subroundList, 6);

                    String moreNDCFString = String.format(", PMiss: %4.2f, PFA: %4.2f", PMiss, PFA);
                    System.out.print(moreNDCFString);
                }
                if (printPercents || verbose) {
                    double avgPcntError = Metrics.getAveragePercentErrorOfCollection(subroundList);
                    double percentCorrect = Metrics.getPercentCorrectGuessesOfCollection(subroundList);

                    String percentStrings = String.format(", %% error: %4.2f, %% correct: %4.2f", avgPcntError, percentCorrect);
                    System.out.print(percentStrings);
                }
                if (printStats || verbose) {
                    double bias = Metrics.getBiasOfCollectionError(subroundList);
                    double absBias = Metrics.getAbsoluteBiasOfCollectionError(subroundList);
                    double STD = Metrics.getSTDofCollectionError(subroundList, bias);

                    String statsString = String.format(", u: %4.2f, |u|: %4.2f, STD: %4.2f", bias, absBias, STD);
                    System.out.print(statsString);
                }
                System.out.println();


                String fileTarget = "C:\\Users\\Trevor\\IdeaProjects\\Covid-Graph-ContactTracing\\output.csv";
                //String fileTarget = "C:\\Users\\trevo\\IdeaProjects\\Covid-Graph-ContactTracing\\output.csv";
                File target = new File(fileTarget);
                try {
                    Metrics.appendCollectionToCSV(subroundList, target);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            System.out.println();
        }
    }

    /**
     * runs a single round of the simulation
     */
    public void runSingleRoundOfSim(){
        SceneBuilder.runSceneRound();
    }

    public void keepRedrawing(){
        while (true){
            visuals.repaintAll();
        }
    }

    private void setBothBounds(double bounds){
        xBounds = bounds;
        yBounds = bounds;
    }

    public ArrayList<Record> getRecords(){
        return Metrics.getAllRecords();
    }

}
