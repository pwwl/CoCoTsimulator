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

import GraphDrawer.graphDrawer;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

/**
 * the agent class for the agent based simulation
 * <p>
 * this is a concrete class and not an abstract because all agents do a similar enough thing that they
 * can be parameterized.
 * <p>
 * the agent is responsible for determining its distances from other agents based on RSSI and other
 * deployable information to simulate the real implementation of the algorithm. It utilizes other
 * helper classes like Utility and GraphDrawer to execute these but they are all static methods.
 */

public class Agent {

    static long simulationTime = 0;
    /**
     * UID is used to uniquely identify agents, it essentially acts as a mac address but it's just an integer
     */
    int UID;
    /**
     * This is a hotfix because the scene needs to look at UID but UIDs are already inflated due to sybil attacks.
     * this should fix the problem :)
     */
    int sceneID;

    /**
     * store x and y coordinates
     * and also store current simulation.sector for ease of access
     * the Active says if the agent is currently in the scene
     */
    boolean active;
    double xCoordinate;
    double yCoordinate;
    Sector currentSector;

    /**
     * some information about how the agent is able to see other agents
     */
    double range;
    Position phonePosition; //position is phone position, such is shirtPocket or purse

    /**
     * static references to objects passed to the agent by the simulation to keep track of RSSIs and Metrics,
     * which are global references in the simulation available to all agents
     */
    static Metrics Metrics;
    static RSSI_collector RSSI;

    //storage for the agent about other neighbors and how it builds information for graph drawings
    ArrayList<Agent> listOfNeighbors;
    HashMap<Integer, Integer> RSSI_pings; //these hashmaps map agentUIDs to something else, in this case RSSI pings
    HashMap<PartOfRound, HashMap<Integer, Double>> partOfRoundToDistancesFromRound;
    HashMap<Integer, Double> initialDistanceGuesses; //in this case it maps it to distances
    HashMap<Integer, Double> finalDistanceGuesses;
    HashMap<Integer, ArrayList<Double>> previousGuesses; //similarly this one maps agents to a list of their previous guesses

    //some utility stuff
    static DistanceMeasure howInitialIsGuessed = DistanceMeasure.ML;

    /**
     * constructor for a single agent
     *
     * @param UID           unique ID for this agent, MUST BE UNIQUE, acts as a MAC address
     * @param xCoord        starting x coordinate
     * @param yCoord        starting y coordinate
     * @param range         maximum range at which the agent can see another agent
     * @param phonePosition the starting position that the agent's phone is held
     * @param Metrics       static reference to the metric instance
     * @param RSSI          static reference to the RSSI instance
     */
    public Agent(int UID, int sceneID, double xCoord, double yCoord, double range,
                 Position phonePosition, Metrics Metrics, RSSI_collector RSSI){
        this.UID = UID;
        this.sceneID = sceneID;
        setCoordinates(xCoord, yCoord);
        this.range = range;
        this.phonePosition = phonePosition;
        this.Metrics = Metrics;
        this.RSSI = RSSI;



        //initialize lists
        listOfNeighbors = new ArrayList<Agent>();
        RSSI_pings = new HashMap<Integer, Integer>();

        partOfRoundToDistancesFromRound = new HashMap<PartOfRound, HashMap<Integer, Double>>();

        for (PartOfRound initializingPart : PartOfRound.values()) {
            partOfRoundToDistancesFromRound.put(initializingPart, new HashMap<Integer, Double>());
        }

        initialDistanceGuesses = new HashMap<Integer, Double>();
        finalDistanceGuesses = new HashMap<Integer, Double>();
        previousGuesses = new HashMap<Integer, ArrayList<Double>>();
    }

    protected void resetAgentForRound() {
        listOfNeighbors.clear();
        initialDistanceGuesses.clear();
        finalDistanceGuesses.clear();
    }

    public void putIntoSector(Sector currentSector) {
        this.currentSector = currentSector;
    }

    public void addNeighbor(Agent neighbor) {
        if (!listOfNeighbors.contains(neighbor)) {
            listOfNeighbors.add(neighbor);
        }
    }

    public void removeNeighbor(Agent neighbor) {
        if (listOfNeighbors.contains(neighbor)) {
            listOfNeighbors.remove(neighbor);
        }
    }

    /**
     * will remove seen neighbors from the list of neighbors, this can be used to simulate a few unseen BLE devices
     *
     * @param dropRate  the percentage at which agents drop neighbors, ranges from [0,1]
     * @param symmetric symmetric input determines whether both neighbors will lose sight of each other or just uni-directional
     **/
    public void randomlyDropNeighbors(double dropRate, boolean symmetric) {
        //because we're using iterators, we need to create a different list
        ArrayList<Agent> neighborsToDrop = new ArrayList<Agent>();

        //accumulate a random set of neighbors to drop
        for (Agent neighborToPotentiallyDrop : listOfNeighbors) {
            if (Math.random() < dropRate) {
                neighborsToDrop.add(neighborToPotentiallyDrop);
            }
        }

        //drop those neighbors
        for (Agent neighborToDrop : neighborsToDrop) {
            removeNeighbor(neighborToDrop);
            if (symmetric) {
                neighborToDrop.removeNeighbor(this);
            }
        }
    }

    public void setPhonePosition(Position position) {
        this.phonePosition = position;
    }

    /**
     * uses the RSSI object to have it randomly assign a phone position.
     * there's no real reason for this other than it was convenient, it could've been done here
     */
    public void setRandomPhonePosition() {
        this.phonePosition = RSSI.assignRandomPosition();
    }

    public void setCoordinates(double xCoord, double yCoord) {
        active = !Double.isNaN(xCoord) && !Double.isNaN(xCoord);
        this.xCoordinate = xCoord;
        this.yCoordinate = yCoord;
    }

    /**
     * returns the number of neighbors seen
     */
    public int numberOfNeighbors() {
        return listOfNeighbors.size();
    }

    /**
     * used for getting an RSSI value of a nearby neighbor, doesn't actually alert the other neighbor
     */
    private int pingNeighbor(Agent neighbor) {
        int numberOfSamples = 150 * 4;
        int totalSampleSum = 0;
        for (int i = 0; i < numberOfSamples; i++) {
            totalSampleSum += RSSI.getRSSI(phonePosition, neighbor.phonePosition, Utility.distanceBetween(this, neighbor));
        }
        int pingRSSIValue = totalSampleSum / numberOfSamples;
        RSSI_pings.put(neighbor.UID, pingRSSIValue);
        return pingRSSIValue;
    }

    /**
     * uses the Utility function to calculate the euclidean distance to another agent
     **/
    private double getActualDistanceTo(Agent neighbor) {
        return Utility.distanceBetween(this, neighbor);
    }

    //a lot of functions do the same things, i just wanted to give them different function signatures so it would be
    //easier in the future if something was going to be added to just add the easiest version and minimize
    //instance variables

    /**
     * this function just uses a linear approximation to guess the distance to a neighbor
     **/
    double guessInitialDistanceToSingleNeighbor(Agent neighbor) {
        double guess = Utility.guessDistanceFromRSSI(this, neighbor, RSSI_pings.get(neighbor.UID));
        initialDistanceGuesses.put(neighbor.UID, guess);
        return guess;
    }


    static public void setInitialGuessMethod(DistanceMeasure guessMethod) {
        howInitialIsGuessed = guessMethod;
    }

    protected double guessInitialDistanceToSingleNeighbor(Agent neighbor, int NeighborRSSIPing) {

        /** able to work with RSSI-esque errors or percent errors **/
        double guess;
        double actualDistance = getActualDistanceTo(neighbor);
        switch (howInitialIsGuessed) {
            case RSSI:
                guess = Utility.guessDistanceFromRSSI(this, neighbor, NeighborRSSIPing);
                break;
            case Rand:
                //inputs to random error
                double percentErrorBounds = 0.50; //as a percent error
                double bias = 0;
                double rawErrorBounds = 0.0; //in feet
                //create randomGuess
                double randomPart = Math.random() / Math.nextDown(1.0); //a random float in [0,1]
                //double percentError = -1 * percentErrorBounds * (1.0 - randomPart) + percentErrorBounds * randomPart; //this should do the same as the next line but the next line is easier to read
                double percentError = percentErrorBounds * (-1.0 - 2 * randomPart);
                randomPart = 2 * (Math.random() / Math.nextDown(1.0)) - 1; //grab a new random float in [-1,1]
                guess = actualDistance * (1 + percentError) + bias + randomPart * rawErrorBounds; // gives the distance as a % of the actual distance, i.e. 90-110%
                //guess = actualDistance - (1 + percentError); // gives the distances + a positive error on top
                break;
            case ML:
                guess = RSSI.getDistanceFromRSSIML(this.phonePosition, neighbor.phonePosition, Utility.distanceBetween(this, neighbor));
                break;
            case MLRSSIHybrid:
                double guess1 = Utility.guessDistanceFromRSSI(this, neighbor, NeighborRSSIPing);
                double guess2 = RSSI.getDistanceFromRSSIML(this.phonePosition, neighbor.phonePosition, Utility.distanceBetween(this, neighbor));
                guess = (guess1 + guess2) / 2;
                break;
            case DATASET:
                guess = RSSI.getDistanceFromPrecompiledMeasurements(simulationTime, this.UID, neighbor.UID);
                if (Double.isNaN(guess) || (guess != guess) || Objects.isNull(guess)) {
                    System.out.println("WE GOT A NAN GUESSER" + guess);
                }
                break;
            default:
                guess = actualDistance;
                break;
        }

        //diagnostic checks on guess
        if (Double.isNaN(guess) || (guess != guess) || Objects.isNull(guess)) {
            System.out.println("WE GOT A NAN GUESSER" + guess);
        }
        if (guess < 0) {
            guess = 0;
        }

        //after guess is approved, then record guess
        initialDistanceGuesses.put(neighbor.UID, guess);
        Metrics.addRecord(this, neighbor, guess, PartOfRound.initialGuess);
        return guess;
    }

    /**
     * goes through all neighbors and guesses distances for the first part of the algorithm
     **/
    public void GetInitialDistanceGuessesToNeighbors() {
        for (Agent neighbor : listOfNeighbors) {
            int RSSIPingValue = pingNeighbor(neighbor);
            //int RSSIPingValue = 0;
            guessInitialDistanceToSingleNeighbor(neighbor, RSSIPingValue);
        }
    }

    public void AverageOutMethod() {
        HashMap<Integer, Double> averageOutGuesses = new HashMap<>();
        for (Agent neighbor : listOfNeighbors) {
            double myGuess = initialDistanceGuesses.get(neighbor.UID);
            double neighborGuess = neighbor.initialDistanceGuesses.get(this.UID);
            double AverageGuess = (myGuess + neighborGuess) / 2;
            averageOutGuesses.put(neighbor.UID, AverageGuess);
        }

        compileRecords(PartOfRound.averageOut, averageOutGuesses);
    }

    /**
     * second part of the algorithm -
     * uses graph drawing
     **/
    public void OneRoundStressMajorization() {
        // if there are fewer than 2 neighbors seen then the graph drawing doesn't apply, so we just skip it
        if (listOfNeighbors.size() > 1) {
            //we create a mapping of mappings for distances to be used with the graph drawer
            HashMap<Integer, HashMap<Integer, Double>> neighborsDistanceGuessesForGraphDrawing = new HashMap<Integer, HashMap<Integer, Double>>();

            neighborsDistanceGuessesForGraphDrawing.put(UID, initialDistanceGuesses);
            for (Agent neighbor : listOfNeighbors) {
                neighborsDistanceGuessesForGraphDrawing.put(neighbor.UID, neighbor.initialDistanceGuesses);
            }

            //fixme debug delete me
            //for some reason a bunch of NaNs are appearing in the initialDistanceGuess lists
            for (int key : neighborsDistanceGuessesForGraphDrawing.keySet()) {
                HashMap<Integer, Double> inspecting = neighborsDistanceGuessesForGraphDrawing.get(key);
                if (inspecting.size() == 0) {
                    for (Agent neighbor : listOfNeighbors) {
                        neighbor.GetInitialDistanceGuessesToNeighbors();
                    }
                }
                for (Double entry : inspecting.values()) {
                    if (entry.isNaN()) {
                        //fixme figure out why sometimes the distance guesses are just nan, so wonky
                        System.out.println("NAN!!!!");
                        for (Agent neighbor : listOfNeighbors) {
                            neighbor.GetInitialDistanceGuessesToNeighbors();
                        }
                    }
                }
            }

            //fixme sometimes the graph drawing just doesn't work so i poorly use a try catch to run it back with a breakpoint
            try {
                //finalDistanceGuesses is needed to keep track of what the agents would have heard during the broadcast state
                finalDistanceGuesses = graphDrawer.getBetterDistancesFromGraphDrawing_ORMDS(UID, (HashMap<Integer, HashMap<Integer, Double>>) neighborsDistanceGuessesForGraphDrawing.clone());
            } catch (Exception e) {
                System.out.println("error in graph drawing");
                e.printStackTrace();
                finalDistanceGuesses = graphDrawer.getBetterDistancesFromGraphDrawing_ORMDS(UID, (HashMap<Integer, HashMap<Integer, Double>>) neighborsDistanceGuessesForGraphDrawing.clone());
                finalDistanceGuesses = (HashMap<Integer, Double>) initialDistanceGuesses.clone();
            }
            //debug
            for (Double values : finalDistanceGuesses.values()) {
                if (values.isNaN()) {
                    System.out.println("nanGuess");
                    finalDistanceGuesses = graphDrawer.getBetterDistancesFromGraphDrawing_ORMDS(UID, neighborsDistanceGuessesForGraphDrawing);
                }
            }
        } else {
            //if it's just 1 or 0 neighbors, keep the initial guess since drawings don't help
            finalDistanceGuesses = (HashMap<Integer, Double>) initialDistanceGuesses.clone();
        }

        compileRecords(PartOfRound.OneRoundStressMajorization, finalDistanceGuesses);
    }

    public void ORMDS_weighted_dropOne() {
        // if there are fewer than 2 neighbors seen then the graph drawing doesn't apply, so we just skip it
        if (listOfNeighbors.size() > 1) {
            //we create a mapping of mappings for distances to be used with the graph drawer
            HashMap<Integer, HashMap<Integer, Double>> neighborsDistanceGuessesForGraphDrawing = new HashMap<Integer, HashMap<Integer, Double>>();

            //fill up hashmap for neighbor estimate
            neighborsDistanceGuessesForGraphDrawing.put(UID, initialDistanceGuesses);
            for (Agent neighbor : listOfNeighbors) {
                neighborsDistanceGuessesForGraphDrawing.put(neighbor.UID, neighbor.initialDistanceGuesses);
            }

            //fixme debug delete me
            //for some reason a bunch of NaNs are appearing in the initialDistanceGuess lists
            for (int key : neighborsDistanceGuessesForGraphDrawing.keySet()) {
                HashMap<Integer, Double> inspecting = neighborsDistanceGuessesForGraphDrawing.get(key);
                if (inspecting.size() == 0) {
                    for (Agent neighbor : listOfNeighbors) {
                        neighbor.GetInitialDistanceGuessesToNeighbors();
                    }
                }
                for (Double entry : inspecting.values()) {
                    if (entry.isNaN()) {
                        //fixme figure out why sometimes the distance guesses are just nan, so wonky
                        System.out.println("NAN!!!!");
                        for (Agent neighbor : listOfNeighbors) {
                            neighbor.GetInitialDistanceGuessesToNeighbors();
                        }
                    }
                }
            }

            HashMap<Integer, Double>[] listOfAttempts = (HashMap<Integer, Double>[]) new HashMap<?, ?>[listOfNeighbors.size() + 1];
            int[] listOfAttemptsIDS = new int[listOfNeighbors.size() + 1];

            //fixme sometimes the graph drawing just doesn't work so i poorly use a try catch to run it back with a breakpoint
            try {
                //finalDistanceGuesses is needed to keep track of what the agents would have heard during the broadcast state
                Pair<HashMap<Integer,Double>,Double> results  = graphDrawer.getBetterDistancesFromGraphDrawing_weightedORMDS(UID, (HashMap<Integer, HashMap<Integer, Double>>) neighborsDistanceGuessesForGraphDrawing.clone());
                listOfAttempts[0] = results.getKey();
                //try dropping one and then remove the one with the highest variance
                for (int i = 1; i < listOfNeighbors.size() + 1; i++) {
                    int neighborID = listOfNeighbors.get(i - 1).UID;
                    results = graphDrawer.getBetterDistancesFromGraphDrawing_weightedORMDS(UID, dropOneNeigbhbor(neighborsDistanceGuessesForGraphDrawing, neighborID));
                    listOfAttempts[i] = results.getKey();
                    listOfAttemptsIDS[i] = neighborID;
                }
                int worstNeighbor = getHighestDifference(listOfAttempts);
                int worstNeighborID = listOfAttemptsIDS[worstNeighbor];
                results = graphDrawer.getBetterDistancesFromGraphDrawing_weightedORMDS(UID, dropOneNeigbhbor(neighborsDistanceGuessesForGraphDrawing, worstNeighborID));
                finalDistanceGuesses = results.getKey();
                finalDistanceGuesses.put(worstNeighborID, initialDistanceGuesses.get(worstNeighborID));
            } catch (Exception e) {
                System.out.println("error in graph drawing");
                e.printStackTrace();
//                Pair<HashMap<Integer,Double>,Double> results  = graphDrawer.getBetterDistancesFromGraphDrawing_weightedORMDS(UID, (HashMap<Integer, HashMap<Integer, Double>>) neighborsDistanceGuessesForGraphDrawing.clone());
//                finalDistanceGuesses = results.getKey();
                finalDistanceGuesses = (HashMap<Integer, Double>) initialDistanceGuesses.clone();
            }
            //debug
            for (Double values : finalDistanceGuesses.values()) {
                if (values.isNaN()) {
                    System.out.println("nanGuess");
                    finalDistanceGuesses = graphDrawer.getBetterDistancesFromGraphDrawing_ORMDS(UID, neighborsDistanceGuessesForGraphDrawing);
                }
            }
        } else {
            //if it's just 1 or 0 neighbors, keep the initial guess since drawings don't help
            finalDistanceGuesses = (HashMap<Integer, Double>) initialDistanceGuesses.clone();
        }

        compileRecords(PartOfRound.weightedORMDS_drop, finalDistanceGuesses);
    }

    public void ORMDS_weighted_dropLink() {
        // if there are fewer than 2 neighbors seen then the graph drawing doesn't apply, so we just skip it
        if (listOfNeighbors.size() > 1) {
            //we create a mapping of mappings for distances to be used with the graph drawer
            HashMap<Integer, HashMap<Integer, Double>> neighborsDistanceGuessesForGraphDrawing = new HashMap<Integer, HashMap<Integer, Double>>();

            neighborsDistanceGuessesForGraphDrawing.put(UID, initialDistanceGuesses);
            for (Agent neighbor : listOfNeighbors) {
                neighborsDistanceGuessesForGraphDrawing.put(neighbor.UID, neighbor.initialDistanceGuesses);
            }

            //fixme debug delete me
            //for some reason a bunch of NaNs are appearing in the initialDistanceGuess lists
            for (int key : neighborsDistanceGuessesForGraphDrawing.keySet()) {
                HashMap<Integer, Double> inspecting = neighborsDistanceGuessesForGraphDrawing.get(key);
                if (inspecting.size() == 0) {
                    for (Agent neighbor : listOfNeighbors) {
                        neighbor.GetInitialDistanceGuessesToNeighbors();
                    }
                }
                for (Double entry : inspecting.values()) {
                    if (entry.isNaN()) {
                        //fixme figure out why sometimes the distance guesses are just nan, so wonky
                        System.out.println("NAN!!!!");
                        for (Agent neighbor : listOfNeighbors) {
                            neighbor.GetInitialDistanceGuessesToNeighbors();
                        }
                    }
                }
            }

            HashMap<Integer, Double>[] listOfAttempts = (HashMap<Integer, Double>[]) new HashMap<?, ?>[1 + listOfNeighbors.size() * listOfNeighbors.size()];
            int[] listOfAttemptsIDS1 = new int[1 + listOfNeighbors.size() * listOfNeighbors.size()];
            int[] listOfAttemptsIDS2 = new int[1 + listOfNeighbors.size() * listOfNeighbors.size()];


            //fixme sometimes the graph drawing just doesn't work so i poorly use a try catch to run it back with a breakpoint
            try {
                //finalDistanceGuesses is needed to keep track of what the agents would have heard during the broadcast state
                Pair<HashMap<Integer,Double>,Double> results  = graphDrawer.getBetterDistancesFromGraphDrawing_weightedORMDS(UID, (HashMap<Integer, HashMap<Integer, Double>>) neighborsDistanceGuessesForGraphDrawing.clone());
                listOfAttempts[0] = results.getKey();

                //try dropping one and then remove the one with the highest variance
                int count = 1;
                for (int i = 0; i < listOfNeighbors.size(); i++) {
                    int neighborID1 = listOfNeighbors.get(i).UID;
                    for (int j = 0; j < listOfNeighbors.size(); j++) {
                        int neighborID2 = listOfNeighbors.get(j).UID;

                        results  = graphDrawer.getBetterDistancesFromGraphDrawing_weightedORMDS(UID, dropOneLink(neighborsDistanceGuessesForGraphDrawing, neighborID1, neighborID2));
                        listOfAttempts[count] = results.getKey();

                        listOfAttemptsIDS1[count] = neighborID1;
                        listOfAttemptsIDS2[count] = neighborID2;

                        count++;
                    }
                }
                int worstAttemp = getHighestDifference(listOfAttempts);
                int worstNeighborID1 = listOfAttemptsIDS1[worstAttemp];
                int worstNeighborID2 = listOfAttemptsIDS2[worstAttemp];
                results  =  graphDrawer.getBetterDistancesFromGraphDrawing_weightedORMDS(UID, dropOneLink(neighborsDistanceGuessesForGraphDrawing, worstNeighborID1, worstNeighborID2));
                finalDistanceGuesses = results.getKey();
                //finalDistanceGuesses.put(worstNeighborID,initialDistanceGuesses.get(worstNeighborID));
            } catch (Exception e) {
                System.out.println("error in graph drawing");
                e.printStackTrace();
                Pair<HashMap<Integer,Double>,Double> results  = graphDrawer.getBetterDistancesFromGraphDrawing_weightedORMDS(UID, (HashMap<Integer, HashMap<Integer, Double>>) neighborsDistanceGuessesForGraphDrawing.clone());
                finalDistanceGuesses = results.getKey();
                finalDistanceGuesses = (HashMap<Integer, Double>) initialDistanceGuesses.clone();
            }
            //debug
            for (Double values : finalDistanceGuesses.values()) {
                if (values.isNaN()) {
                    System.out.println("nanGuess");
                    finalDistanceGuesses = graphDrawer.getBetterDistancesFromGraphDrawing_ORMDS(UID, neighborsDistanceGuessesForGraphDrawing);
                }
            }
        } else {
            //if it's just 1 or 0 neighbors, keep the initial guess since drawings don't help
            finalDistanceGuesses = (HashMap<Integer, Double>) initialDistanceGuesses.clone();
        }

        compileRecords(PartOfRound.weightedORMDS_dropLink, finalDistanceGuesses);
    }

    private int getHighestDifference(HashMap<Integer, Double>[] listOfAttempts) {
        HashMap<Integer, Double> baseline = listOfAttempts[0];
        double[] sumOfRelativeDeltas = new double[listOfAttempts.length];
        double maxDiff = -1;
        int idOfMaxDiff = -1;
        for (int i = 1; i < listOfAttempts.length; i++) {
            HashMap<Integer, Double> comparator = listOfAttempts[i];
            double relativeDeltaSum = 0;
            for (Integer neighborID : baseline.keySet()) {
                if (comparator.containsKey(neighborID)) {
                    double base = baseline.get(neighborID);
                    double newMeasure = comparator.get(neighborID);
                    relativeDeltaSum += Math.abs((base - newMeasure) / ((base + newMeasure) / 2));
                }
            }
            sumOfRelativeDeltas[i] = relativeDeltaSum;
            if (relativeDeltaSum > maxDiff) {
                maxDiff = relativeDeltaSum;
                idOfMaxDiff = i;
            }
        }
        return idOfMaxDiff;
    }

    private HashMap<Integer, HashMap<Integer, Double>> dropOneNeigbhbor(HashMap<Integer, HashMap<Integer, Double>> neighborsDistanceGuessesForGraphDrawing, int UID) {
        HashMap<Integer, HashMap<Integer, Double>> localDistances = graphDrawer.safeCopyEntryList(neighborsDistanceGuessesForGraphDrawing);
        localDistances.remove(UID);
        return localDistances;
    }

    private HashMap<Integer, HashMap<Integer, Double>> dropOneLink(HashMap<Integer, HashMap<Integer, Double>> neighborsDistanceGuessesForGraphDrawing, int UID1, int UID2) {
        HashMap<Integer, HashMap<Integer, Double>> localDistances = graphDrawer.safeCopyEntryList(neighborsDistanceGuessesForGraphDrawing);
        localDistances.get(UID1).remove(UID2);
        localDistances.get(UID2).remove(UID1);
        return localDistances;
    }


    public void ORMDS_weighted() {
        double stress = 0;
        // if there are fewer than 2 neighbors seen then the graph drawing doesn't apply, so we just skip it
        if (listOfNeighbors.size() > 2) {
            //we create a mapping of mappings for distances to be used with the graph drawer
            HashMap<Integer, HashMap<Integer, Double>> neighborsDistanceGuessesForGraphDrawing = new HashMap<Integer, HashMap<Integer, Double>>();

            neighborsDistanceGuessesForGraphDrawing.put(UID, initialDistanceGuesses);
            for (Agent neighbor : listOfNeighbors) {
                neighborsDistanceGuessesForGraphDrawing.put(neighbor.UID, neighbor.initialDistanceGuesses);
            }

            //fixme debug delete me
            //for some reason a bunch of NaNs are appearing in the initialDistanceGuess lists
            for (int key : neighborsDistanceGuessesForGraphDrawing.keySet()) {
                HashMap<Integer, Double> inspecting = neighborsDistanceGuessesForGraphDrawing.get(key);
                if (inspecting.size() == 0) {
                    for (Agent neighbor : listOfNeighbors) {
                        neighbor.GetInitialDistanceGuessesToNeighbors();
                    }
                }
                for (Double entry : inspecting.values()) {
                    if (entry.isNaN()) {
                        //fixme figure out why sometimes the distance guesses are just nan, so wonky
                        System.out.println("nan in initialGuess");
                        for (Agent neighbor : listOfNeighbors) {
                            neighbor.GetInitialDistanceGuessesToNeighbors();
                        }
                    }
                }
            }
            //fixme sometimes the graph drawing just doesn't work so i poorly use a try catch to run it back with a breakpoint
            try {
                //finalDistanceGuesses is needed to keep track of what the agents would have heard during the broadcast state
                Pair<HashMap<Integer, Double>, Double> results = graphDrawer.getBetterDistancesFromGraphDrawing_weightedORMDS(UID, (HashMap<Integer, HashMap<Integer, Double>>) neighborsDistanceGuessesForGraphDrawing.clone());
                finalDistanceGuesses = results.getKey();
                stress = results.getValue();

                //debug for figure
                boolean visualExperiment = false;
                if (visualExperiment) {
                    RealMatrix locationsOfNeighbors = graphDrawer.WORMDSGrid(UID, (HashMap<Integer, HashMap<Integer, Double>>) neighborsDistanceGuessesForGraphDrawing.clone());
                    HashMap<Integer, Integer> UIDtoIndex = graphDrawer.listsToUIDtoIndex(neighborsDistanceGuessesForGraphDrawing);
                    System.out.println("estimations from agent " + this.UID);
                    System.out.println(String.format("%6s, %6s, %6s, %6s, %6s", "UID", "trueX", "trueY", "estX", "estY"));
                    RealVector correspRow = locationsOfNeighbors.getRowVector(0);
                    for (Integer iUID : UIDtoIndex.keySet()) {
                        correspRow = locationsOfNeighbors.getRowVector(iUID);
                        Agent neighborI = getagentByID(iUID);
                        if (!Objects.isNull(neighborI)) {
                            System.out.println(String.format("%6d, %6f, %6f, %6f, %6f", neighborI.UID, neighborI.xCoordinate, neighborI.yCoordinate,
                                    correspRow.getEntry(0), correspRow.getEntry(1)));
                        }
                    }
                }

            } catch (Exception e) {
                System.out.println("error in graph drawing");
                e.printStackTrace();
                stress = 0;
            }
            //debug
            for (Double value : finalDistanceGuesses.values()) {
                if (value.isNaN()) {
                    // numerical isntability or impossible configuration. Just keep the initial guess as a fallback
//                    System.out.println("nan in final weieghtedORMDS");
                    finalDistanceGuesses = (HashMap<Integer, Double>) initialDistanceGuesses.clone();
                }
            }
        } else {
            //if there are fewer than 4 neighbors, keep the initial guess since drawings don't help
            finalDistanceGuesses = (HashMap<Integer, Double>) initialDistanceGuesses.clone();
            return;
        }

        compileRecords(PartOfRound.weightedORMDS, finalDistanceGuesses, stress);
    }

    private Agent getagentByID(int UID){
        if (UID == this.UID){
            return this;
        } else {
            for (Agent neighbor : listOfNeighbors){
                if (neighbor.UID == UID){
                    return neighbor;
                }
            }
            return null;
        }
    }

    public void CliqueMDS() {
        HashMap<Integer, Double> cliqueDistances = new HashMap<>();
        if (listOfNeighbors.size() > 2) {
            HashMap<Integer, ArrayList<Agent>> maxCliques = findMaxCliques();
            HashMap<Integer, Double> newCliqueDistances = getDistancesOfCliques(maxCliques);
            cliqueDistances = updateDistances(initialDistanceGuesses, newCliqueDistances);
        } else {
            cliqueDistances = (HashMap<Integer, Double>) initialDistanceGuesses.clone();
        }

        compileRecords(PartOfRound.cliqueMDS, cliqueDistances);
    }

    public HashMap<Integer, ArrayList<Agent>> findMaxCliques() {
        HashMap<Integer, ArrayList<Agent>> maxCliquesForEachNeighbor = new HashMap<>();
        for (Agent neighbor : listOfNeighbors) {
            ArrayList<Agent> CliqueMembers = new ArrayList<>();
            CliqueMembers.add(this);
            //the neighbor is added in the function findCliques below
            ArrayList<Agent> clique = findCliques(CliqueMembers, neighbor, (ArrayList<Agent>) listOfNeighbors.clone());
            for (Agent cliqueMember : clique) {
                int agentID = cliqueMember.UID;
                if (!maxCliquesForEachNeighbor.containsKey(agentID) || maxCliquesForEachNeighbor.get(agentID).size() < clique.size()) {
                    maxCliquesForEachNeighbor.put(agentID, clique);
                }
            }
        }
        return maxCliquesForEachNeighbor;
    }

    public ArrayList<Agent> findCliques(ArrayList<Agent> membersOfClique, Agent neighbor, ArrayList<Agent> potentialMembersOfClique) {
        ArrayList<Agent> recursiveMembers = (ArrayList<Agent>) membersOfClique.clone();
        recursiveMembers.add(neighbor);

        //intersect the current clique with the neighbors of the newest member to see our available members
        potentialMembersOfClique.retainAll(neighbor.listOfNeighbors);
        ArrayList<Agent> lookThrough = (ArrayList<Agent>) potentialMembersOfClique.clone();
        lookThrough.removeAll(membersOfClique);
        ArrayList<Agent> recursiveMaximalClique = (ArrayList<Agent>) membersOfClique.clone();
        for (Agent recursiveStepAgent : lookThrough) {
            ArrayList<Agent> recursiveClique = findCliques(recursiveMembers, recursiveStepAgent, (ArrayList<Agent>) potentialMembersOfClique.clone());
            if (recursiveClique.size() > recursiveMaximalClique.size()) {
                recursiveMaximalClique = recursiveClique;
            }
            if (recursiveClique.size() > ((double) lookThrough.size() + 1) / 2) {
                break;
            }
        }
        return recursiveMaximalClique;
    }

    public HashMap<Integer, Double> getDistancesOfCliques(HashMap<Integer, ArrayList<Agent>> cliques) {
        HashMap<Integer, Double> cliqueGuesses = new HashMap<>();
        for (Agent neighbor : listOfNeighbors) {
            HashMap<Integer, HashMap<Integer, Double>> cliqueDistances = new HashMap<>();
            ArrayList<Agent> cliqueMembers = cliques.get(neighbor.UID);
            if (!Objects.isNull(cliqueMembers) && cliqueMembers.size() > 3) {
                for (Agent member : cliqueMembers) {
                    int memberUID = member.UID;
                    HashMap<Integer, Double> memberGuesses = new HashMap<>();

                    for (Agent otherMember : cliqueMembers) {
                        if (otherMember.UID != memberUID) {
                            memberGuesses.put(otherMember.UID, member.initialDistanceGuesses.get(otherMember.UID));
                        }
                    }

                    cliqueDistances.put(memberUID, memberGuesses);
                }
                //todo the caller isn't getting added to cliqueDistances for some reason, figure out why.
                HashMap<Integer, Double> thisCliqueGuess;
                try {
                    thisCliqueGuess = graphDrawer.getBetterDistancesFromGraphDrawing_ORMDS(UID, cliqueDistances);
                } catch (Exception e) {
                    e.printStackTrace();
                    thisCliqueGuess = graphDrawer.getBetterDistancesFromGraphDrawing_ORMDS(UID, cliqueDistances);
                }
                cliqueGuesses.put(neighbor.UID, thisCliqueGuess.get(neighbor.UID));
            }
        }
        return cliqueGuesses;
    }

    public HashMap<Integer, Double> updateDistances(HashMap<Integer, Double> initialGuesses, HashMap<Integer, Double> updatedGuesses) {
        HashMap<Integer, Double> toReturn = new HashMap<>();
        toReturn.putAll(initialGuesses);
        toReturn.putAll(updatedGuesses);
        return toReturn;
    }

    public double measureAccuracy() {
        ArrayList<Record> myRecords = Metrics.getAllRecords();
        myRecords = Metrics.filterByRecordingAgent(myRecords, this.UID);
        return Metrics.getPercentCorrectGuessesOfCollection(myRecords);
    }


    private void compileRecords(PartOfRound recordingPart, HashMap<Integer, Double> distanceGuesses) {
        for (Agent neighbor : listOfNeighbors) {
            if (distanceGuesses.containsKey(neighbor.UID)) {
                Metrics.addRecord(this, neighbor, distanceGuesses.get(neighbor.UID), recordingPart);
            }
        }
    }
    private void compileRecords(PartOfRound recordingPart, HashMap<Integer, Double> distanceGuesses, double finalStress){
        for (Agent neighbor : listOfNeighbors) {
            if (distanceGuesses.containsKey(neighbor.UID)) {
                Metrics.addRecord(this, neighbor, distanceGuesses.get(neighbor.UID), recordingPart, finalStress);
            }
        }
    }
    //a few random getters and setters

    public double getGuessedDistanceTo(Agent neighbor) {
        return initialDistanceGuesses.get(neighbor.UID);
    }

    public double getXCoord() {
        return xCoordinate;
    }

    public double getYCoordinate() {
        return yCoordinate;
    }

    public ArrayList<Agent> getListOfNeighbors() {
        return listOfNeighbors;
    }
}
