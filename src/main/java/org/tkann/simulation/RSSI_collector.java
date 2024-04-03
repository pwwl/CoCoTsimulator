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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

/**
class used to get RSSI samples from the NIST datasets
 @author tkann@andrew.cmu.edu
 */

public class RSSI_collector {


    static Random rand = new Random();
    public static long seed = 8008135;

    static final int[] ranges = {3,4,5,6,8,10,12,15};//list of ranges that the BLE RSSI samples are taken from

    //we use reservoir sampling to prevent having loaded in >100k samples during our sim and for quick reference
    static final int reservoirSampleSize = 1000;

    //the first axis of the array is based on what combination of phone positions the two requesting agents have
    //the second axis is the nearest distance they're at
    //the third axis is a list of samples for that range-position combo, and we take a random one for the RSSI
    static int[][][] RSSIValues = new int[Position.values().length*(Position.values().length+1)/2][ranges.length][reservoirSampleSize];

    //todo: delete this probably, this is kinda a bandage thing
    HashMap<Integer, Double[]> MLpredictions;

    //folder containing all the data scraped from the NIST sets
    //static String fileLocation = "C:\\Users\\trevo\\Documents\\GitHub\\Covid-Graph-ContactTracing\\dataDump\\";
    public static String fileLocation = "C:\\Users\\Trevor\\IdeaProjects\\Covid-Graph-ContactTracing\\dataDump\\";
    public static String MLLocations = null;
    //static String MLLocations = fileLocation;
    public static String precompiledDistancesLocation = null;
    protected static PrecompiledDistanceHolder precompiledDistances;
    public static boolean defaultSymmetry = false;

    //constructor
    public RSSI_collector() throws FileNotFoundException,IOException {
        rand = new Random();
        rand.setSeed(seed);
        Position positions[] = Position.values();
//        initialFillPoolSamples();  /* commented out for speed */
        if (MLLocations != null) {
            initializeML_CDFs();
        }
        if (precompiledDistancesLocation != null){
            precompiledDistances = new PrecompiledDistanceHolder();
            precompiledDistances.loadPrecompiledDistanceCSV(precompiledDistancesLocation);
        }
    }

    public RSSI_collector(long seed) throws FileNotFoundException,IOException {
        this();
        rand.setSeed(seed);
        this.seed = seed;
    }

    //fill our array with reservoir sampled samples of RSSI data set from NIST set
    private void initialFillPoolSamples() throws IOException {
        //Reservoir Sampling based on PureFerret's code at https://stackoverflow.com/questions/2218005/how-to-get-a-random-line-of-a-text-file-in-java

        //iterate through the different position combinations
        for (Position pos1 : Position.values()) { for (Position pos2 : Position.values()) {
            //get the combination ID
            int comboID = getPositionCombination(pos1,pos2);

            for (int range=0; range<ranges.length;range++) {

                //check to see if we have seen this combo before
                if (RSSIValues[comboID][range][0]!=0) {
                    //0 is not a value found in the RSSI samples, therefore we can be assured this is hit
                    break;
                }

                String currentLine = null;
                int count = 0;
                int randomNumber = 0;
                String filename = RangeAndPositionsToFileName(range,pos1,pos2);
                Scanner SC = new Scanner(new File(fileLocation+filename)).useDelimiter("\n");

                //do the reservoir sample to fill the given RSSI combination-range array
                while (SC.hasNext()) {
                    currentLine = SC.next();
                    if (count<reservoirSampleSize) {
//						System.out.println(currentLine.replaceAll("[^-0-9]","")); // a debug line to make sure regex was working
                        RSSIValues[comboID][range][count] = Integer.parseInt(currentLine.replaceAll("[^-0-9]",""));
                    } else if ((randomNumber = rand.nextInt(count)) < reservoirSampleSize){
                        RSSIValues[comboID][range][randomNumber] = 	Integer.parseInt(currentLine.replaceAll("[^-0-9]",""));
                    }
                    count ++;
                }
                SC.close();
            }
        }}
    }

    private void initializeML_CDFs() throws IOException {

        MLpredictions = new HashMap<>();
        for (int range : ranges){
            File CDFLocation = new File(MLLocations+range+"inverseCDF.csv");
            Scanner SC = new Scanner(CDFLocation).useDelimiter("\n");
            String CDFLine = SC.nextLine();
            String[] ReadValues = CDFLine.split(",");
            Double[] numericValues = new Double[ReadValues.length];
            for (int i = 0; i < ReadValues.length; i++){
                numericValues[i] = Double.parseDouble(ReadValues[i]);
            }
            MLpredictions.put(range,numericValues);
        }

    }

    public double getDistanceFromRSSIML(Position nodePosition, Position neighborPosition, double distance) {
        int roundedDistance = nearestDist(distance);
        Double[] distanceEstimateCDFForRoundedDistanceInFeet = MLpredictions.get(roundedDistance);
        int randomIndex = rand.nextInt(distanceEstimateCDFForRoundedDistanceInFeet.length);
        return distanceEstimateCDFForRoundedDistanceInFeet[randomIndex];
    }

    public double getDistanceFromPrecompiledMeasurements(long time, int recieverID, int broadcasterID, boolean symmetric){
        return precompiledDistances.getPrecompiledDistanceEstimate(time, recieverID, broadcasterID, symmetric);
    }

    public double getDistanceFromPrecompiledMeasurements(long time, int recieverID, int broadcasterID){
        return getDistanceFromPrecompiledMeasurements(time, recieverID, broadcasterID, defaultSymmetry);
    }

    public boolean checkIfAgentsAreNeighbors(long time, int recieverID, int broadcasterID, boolean symmetric){
        return precompiledDistances.checkIfNeighbors(time, recieverID, broadcasterID, symmetric);
    }

    //determine the ID of a combination (like NchooseM) of phone positions. I.e. inPocket to inPocket is 1, inPocket to shirt is 2...
    public int getPositionCombination(Position pos1, Position pos2) {
        //input: 2 positions
        //output: the combination ID of both inputs.
        //Order does not matter, i.e. Hand,Purse is the same as Purse,Hand
        if (pos1.ID <= pos2.ID) {
            //you can derive this non-linear equation to enumerate the Nchoose2 for any two chosen positions
            //assuming that pos1.ID is less than pos2.ID
            return (Position.values().length * pos1.ID) + pos2.ID - ((pos1.ID*pos1.ID+pos1.ID)/2);
        } else {
            //if it's not ordered correctly, reorder them (recursively)
            return getPositionCombination(pos2,pos1);
        }
    }

    //given a two positions and a range, find the filename of the dataset for that set
    public String RangeAndPositionsToFileName(int range, Position pos1, Position pos2) {
        if (pos1.ID<=pos2.ID) {
            return ranges[range]+"-"+pos1.name()+"-"+pos2.name()+".txt";
        } else {
            return ranges[range]+"-"+pos2.name()+"-"+pos1.name()+".txt";
        }
    }

    //the command that this is built for, given two positions and an approximate range, gets an RSSI sample
    public int getRSSI(Position nodePosition, Position neighborPosition, double distance) {

        int positionCombination = getPositionCombination(nodePosition, neighborPosition);
        int distID = nearestDistIndex(distance);
        int randIndex = rand.nextInt(reservoirSampleSize);
        return RSSIValues[positionCombination][distID][randIndex];
    }

    //round a true distance to an integer distance that is represented by one of the data sets
    public int nearestDist(double distance) {
        //to make this independent of the ranges array and instead of doing an if...elseif...
        //use a for loop and divide between the halfway point between each point.
        for (int i = 0; i<ranges.length-1; i++){
            if (distance <= (((double) (ranges[i]+ranges[i+1]))/2)){
                return ranges[i];
            }
        } // since ranges[i+1] is used, end at ranges.len-1 and then return the last one if it doesn't match any others
        return ranges[ranges.length-1];
    }

    //same thing as above but it gets the index of the array entry instead of the value
    //which is a lot more useful since our RSSI array uses the same structure as our ranges array
    public int nearestDistIndex(double distance) {
        for (int i = 0; i<ranges.length-1; i++){
            if (distance <= ((double) (ranges[i]+ranges[i+1])/2)){
                return i;
            }
        }
        return ranges.length-1;
    }

    //gives a random phone position
    //this could be in util but it doesn't really matter, we can instantiate a random instance here and
    //eventually use a better seen than just "new rand()"
    public Position assignRandomPosition() {
        return Position.values()[rand.nextInt(Position.values().length)];
    }

    private class PrecompiledDistanceHolder{
        private Map<Integer, Map<Integer, Map<Long, Double>>> RecieverToBroadcasterToTimeToDistance;
        int USUAL_TIME_TO_SEE = 30000;
        int roundedTimeThreshold = (int) (USUAL_TIME_TO_SEE * 1.5/2);

        PrecompiledDistanceHolder(){
            RecieverToBroadcasterToTimeToDistance = new HashMap<>();
        }

        protected void loadPrecompiledDistanceCSV(String precompiledDistancesLocation) throws IOException {
            File precompiledDistancesFile = new File(precompiledDistancesLocation);
            Scanner SC = new Scanner(precompiledDistancesFile).useDelimiter("\n");
            while (SC.hasNext()){
                String currentLine = SC.nextLine();
                // header = time,receiver,broadcaster,prediction
                if (currentLine.contains("time")){
                    continue;
                }
                String[] splitLine = currentLine.split(",");
                Long time = Long.parseLong(splitLine[0]);
                int recieverID = Integer.parseInt(splitLine[1]);
                int broadcasterID = Integer.parseInt(splitLine[2]);
                double distance = Double.parseDouble(splitLine[3]);
                if (!RecieverToBroadcasterToTimeToDistance.containsKey(recieverID)){
                    RecieverToBroadcasterToTimeToDistance.put(recieverID, new HashMap<>());
                }
                Map<Integer, Map<Long, Double>> BroadcasterToTimeToDistance = RecieverToBroadcasterToTimeToDistance.get(recieverID);
                if (!BroadcasterToTimeToDistance.containsKey(broadcasterID)){
                    BroadcasterToTimeToDistance.put(broadcasterID, new HashMap<>());
                }
                Map<Long, Double> TimeToDistance = BroadcasterToTimeToDistance.get(broadcasterID);
                TimeToDistance.put(time, distance);
            }
        }

        public boolean checkIfNeighbors(long time, int recieverID, int broadcasterID, boolean symmetric){
            Double distanceEstimate = getPrecompiledDistanceEstimate(time, recieverID, broadcasterID, symmetric);
            return !Double.isNaN(distanceEstimate);
        }

        protected Double getPrecompiledDistanceEstimate(long time, int recieverID, int broadcasterID, boolean symmetric){
            if (!RecieverToBroadcasterToTimeToDistance.containsKey(recieverID)){
                if (symmetric){
                    return getPrecompiledDistanceEstimate(time, broadcasterID, recieverID, false);
                }
                return Double.NaN;
            }
            Map<Integer, Map<Long, Double>> BroadcasterToTimeToDistance = RecieverToBroadcasterToTimeToDistance.get(recieverID);
            if (!BroadcasterToTimeToDistance.containsKey(broadcasterID)){
                if (symmetric){
                    return getPrecompiledDistanceEstimate(time, broadcasterID, recieverID, false);
                }
                return Double.NaN;
            }
            Map<Long, Double> TimeToDistance = BroadcasterToTimeToDistance.get(broadcasterID);
            long bestTime = 0;
            for (long recordedTime : TimeToDistance.keySet()){
                if (Math.abs(recordedTime-time) < Math.abs(bestTime-time)){
                    bestTime = recordedTime;
                }
            }
            if (Math.abs(bestTime-time) > roundedTimeThreshold){
                if (symmetric){
                    return getPrecompiledDistanceEstimate(time, broadcasterID, recieverID, false);
                }
                return Double.NaN;
            }
            return TimeToDistance.get(bestTime);
        }
    }
}
