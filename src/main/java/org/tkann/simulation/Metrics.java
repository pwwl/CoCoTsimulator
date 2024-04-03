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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
* the metrics class is used to hold records, which is how the simulation keeps track of it's accuracy.
* the Records have a bunch of information about when the guess took place and how it was so that we can
* keep close tabs on what is and isn't working and where exactly it is or isn't working.
*
 *The metrics class is just a holder and interface for all of those records.
 */

public class Metrics {

    //this will hold all of the records
    ArrayList<Record> allRecords;


    //it's important to keep track of the rounds so we can inspect the records later based on round
    private int currentRound;


    private int totalNumberOfRounds;

    //constructor
    public Metrics(int currentRound, int totalNumberOfRounds){
        setRoundNumber(currentRound);
        this.totalNumberOfRounds = totalNumberOfRounds;

        allRecords = new ArrayList<Record>();
    }

    public void setRoundNumber(int roundNumber){
        currentRound = roundNumber;
    }

    public boolean addRecord(Agent recordingAgent, Agent neighborAgent, double guessedDistance, PartOfRound part){
        Record newRecord = new Record(recordingAgent, neighborAgent, guessedDistance, currentRound, part, 0);
        allRecords.add(newRecord);
        return newRecord.correctGuess;
    }
    public boolean addRecord(Agent recordingAgent, Agent neighborAgent, double guessedDistance, PartOfRound part, double stress){
        Record newRecord = new Record(recordingAgent, neighborAgent, guessedDistance, currentRound, part, stress);
        allRecords.add(newRecord);
        return newRecord.correctGuess;
    }

    public void purgeAllRecords(){
        for (Record inspecting : allRecords){
            inspecting.sectorItOccurredIn = null;
        }
        allRecords.clear();
    }
    public ArrayList<Record> getAllRecords(){
        return (ArrayList<Record>) allRecords.clone();
    }


    /**
     *  gets accuracy of all records
     * @return total accuracy of all records
     */




    public static ArrayList<Record> filterByRecordingAgent(ArrayList<Record> initialList, int recordingAgentUID){
        ArrayList<Record> filteredList = new ArrayList<Record>();

        for (Record inspecting : initialList){
            if (inspecting.UIDofGuessingAgent == recordingAgentUID){
                filteredList.add(inspecting);
            }
        }
        return filteredList;
    }

    public static ArrayList<Record> filterByRecordingAgent(ArrayList<Record> initialList, Agent recordingAgent){
        return filterByRecordingAgent(initialList, recordingAgent.UID);
    }

    public static ArrayList<Record> filterByReceivingAgent(ArrayList<Record> initialList, int receivingAgentUID){
        ArrayList<Record> filteredList = new ArrayList<Record>();

        for (Record inspecting : initialList){
            if (inspecting.UIDofNeighbor == receivingAgentUID){
                filteredList.add(inspecting);
            }
        }
        return filteredList;
    }

    public static ArrayList<Record> filterByReceivingAgent(ArrayList<Record> initialList, Agent receivingAgent){
        return filterByReceivingAgent(initialList, receivingAgent.UID);
    }

    public static ArrayList<Record> filterBySector(ArrayList<Record> initialList, Sector SectorInQuestion){
        ArrayList<Record> filteredList = new ArrayList<Record>();

        for (Record inspecting : initialList){
            if (inspecting.sectorItOccurredIn.equals(SectorInQuestion)){
                filteredList.add(inspecting);
            }
        }
        return filteredList;
    }

    public static ArrayList<Record> filterByRound(ArrayList<Record> initialList, int roundItOccurredIn){
        ArrayList<Record> filteredList = new ArrayList<Record>();

        for (Record inspecting : initialList){
            if (inspecting.roundItOccurredIn == roundItOccurredIn){
                filteredList.add(inspecting);
            }
        }
        return filteredList;
    }

    public static ArrayList<Record> filterBySubround(ArrayList<Record> initialList, PartOfRound PartToFilterBy){
        ArrayList<Record> filteredList = new ArrayList<Record>();

        for (Record inspecting : initialList){
            if (inspecting.partOfRoundTheRecordOccurredIn == PartToFilterBy){
                filteredList.add(inspecting);
            }
        }
        return filteredList;
    }

    public static ArrayList<Record> filterByTrueDistance(ArrayList<Record> initialList, double minDistance, double maxDistance){
        ArrayList<Record> filteredList = new ArrayList<Record>();

        for (Record inspecting : initialList){
            double trueDist = inspecting.trueDistance;
            if ((minDistance < trueDist) && (trueDist < maxDistance)){
                filteredList.add(inspecting);
            }
        }
        return filteredList;
    }

    public static ArrayList<Record> filterByGuessedDistance(ArrayList<Record> initialList, double minDistance, double maxDistance){
        ArrayList<Record> filteredList = new ArrayList<Record>();

        for (Record inspecting : initialList){
            double guessDist = inspecting.guessedDistance;
            if ((minDistance < guessDist) && (guessDist < maxDistance)){
                filteredList.add(inspecting);
            }
        }
        return filteredList;
    }

    /**
     once the proper collection has been made, then statistics can be done on the collection
     **/

    public static double getBiasOfCollectionError(ArrayList<Record> inputList){
        if (inputList.size() == 0){
            return 0;
        }
        double totalError = 0;
        for (Record inspecting : inputList){
            totalError += inspecting.trueDistance - inspecting.guessedDistance;
        }
        return totalError/inputList.size();
    }

    public static double getAbsoluteBiasOfCollectionError(ArrayList<Record> inputList){
        if (inputList.size() == 0){
            return 0;
        }
        double totalABSError = 0;
        for (Record inspecting : inputList){
            totalABSError += Math.abs(inspecting.trueDistance - inspecting.guessedDistance);
        }
        return totalABSError/inputList.size();
    }

    public static double getSTDofCollectionError(ArrayList<Record> inputList){
        if (inputList.size() == 0){
            return 0;
        }
        //calculate mean error
        double totalError = 0;
        for (Record inspecting : inputList){
            double error = inspecting.trueDistance - inspecting.guessedDistance;
            totalError += error;
        }
        double meanError =  totalError/inputList.size();
        return getSTDofCollectionError(inputList, meanError);
    }


    public static double getSTDofCollectionError(ArrayList<Record> inputList, double mean){
        if (inputList.size() == 0){
            return 0;
        }
        //calculate mean deviance
        double totalDeviance = 0;
        for (Record inspecting : inputList){
            double error = inspecting.trueDistance - inspecting.guessedDistance;
            double squareDeviance = (error - mean)*(error - mean);
            totalDeviance += squareDeviance;
        }
        double variance =  totalDeviance/(inputList.size()-1);
        double STD = Math.sqrt(variance);
        return STD;
    }
    public static double getSTDofCollectionAbsError(ArrayList<Record> inputList){
        if (inputList.size() == 0){
            return 0;
        }
        //calculate mean error
        double totalError = 0;
        for (Record inspecting : inputList){
            double error = Math.abs(inspecting.trueDistance - inspecting.guessedDistance);
            totalError += error;
        }
        double meanError =  totalError/inputList.size();

        return getAbsSTDofCollectionError(inputList, meanError);
    }
    public static double getAbsSTDofCollectionError(ArrayList<Record> inputList, double mean){
        if (inputList.size() == 0){
            return 0;
        }
        //calculate mean deviance
        double totalDeviance = 0;
        for (Record inspecting : inputList){
            double error = Math.abs(inspecting.trueDistance - inspecting.guessedDistance);
            double squareDeviance = (error - mean)*(error - mean);
            totalDeviance += squareDeviance;
        }
        double variance =  totalDeviance/(inputList.size()-1);
        double STD = Math.sqrt(variance);
        return STD;
    }


    public static double getPercentCorrectGuessesOfCollection(ArrayList<Record> inputList){
        if (inputList.size() == 0){
            return 0;
        }
        int totalCorrect = 0;
        for (Record inspecting : inputList){
            if (inspecting.correctGuess){
                totalCorrect++;
            }
        }
        return (double) totalCorrect / inputList.size();

    }

    public static double getAveragePercentErrorOfCollection(ArrayList<Record> inputList){
        if (inputList.size() == 0){
            return 0;
        }
        double totalPercentError = 0;
        for (Record inspecting : inputList){
            totalPercentError += inspecting.calculateGuessPercentError();
        }
        return totalPercentError / inputList.size();
    }

    public static double getAverageStressOfCollection(ArrayList<Record> inputList){
        if (inputList.size() == 0){
            return 0;
        }
        double totalStress = 0;
        for (Record inspecting : inputList){
            totalStress += inspecting.averageStress;
        }
        return totalStress / inputList.size(); //average averageStress

    }

    public static double getPMissOfCollection(ArrayList<Record> inputList, double cutoff){
        if (inputList.size() == 0){
            return 0;
        }

        int actualTooCloseEncounters = 0;
        int missedTooCloseEncounters = 0;

        for (Record inspecting : inputList){
            if (inspecting.trueDistance < cutoff){
                actualTooCloseEncounters++;
                if(inspecting.guessedDistance > cutoff){
                    missedTooCloseEncounters++;
                }
            }
        }


        double pMiss = ( (double) missedTooCloseEncounters) / (Math.max(1,(double) actualTooCloseEncounters));
        return pMiss;
    }

    public static double getPFAOfCollection(ArrayList<Record> inputList, double cutoff){

        if (inputList.size() == 0){
            return 0;
        }
        int actualNotTooCloseEncounters = 0;
        int missedNotTooCloseEncounters = 0;

        for (Record inspecting : inputList){
            if (inspecting.trueDistance > cutoff){
                actualNotTooCloseEncounters++;
                if (inspecting.guessedDistance < cutoff){
                    missedNotTooCloseEncounters++;
                }
            }
        }
        double pFA = ( (double) missedNotTooCloseEncounters) / Math.max(1,(double) actualNotTooCloseEncounters);
        return pFA;
    }

    public static double getNDCFOfCollection(ArrayList<Record> inputList, double cutoff){

        double weightMiss = 1;
        double weightFA = 1;

        double pMiss = getPMissOfCollection(inputList, cutoff);
        double pFA = getPFAOfCollection(inputList, cutoff);

        double nDCF = (weightMiss * pMiss + weightFA * pFA) / Math.min(weightFA, weightMiss);

        return nDCF;
    }

    public int getTotalNumberOfRounds() {
        return totalNumberOfRounds;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setTotalNumberOfRounds(int totalNumberOfRounds) {
        this.totalNumberOfRounds = totalNumberOfRounds;
    }

    public static boolean isTooClose(Record inspecting){
        if ((inspecting.trueDistance < 6)){
            return true;
        } else {
            return false;
        }
    }

    public void appendCollectionToCSV(ArrayList<Record> inputList, File fileTarget) throws IOException {

        if (!fileTarget.exists()){
            fileTarget.createNewFile();
        }
        FileWriter fw = new FileWriter(fileTarget, true);
        BufferedWriter bw = new BufferedWriter(fw);

        String header = String.format("round,part,recording,neighbor,X,Y,reference,guess\n");
        bw.write(header);
        String skeleton = "%d,%s,%d,%d,%f,%f,%f,%f\n";

        for (Record inspecting : inputList){
            String recordValues = String.format(skeleton, inspecting.roundItOccurredIn, inspecting.partOfRoundTheRecordOccurredIn, inspecting.UIDofGuessingAgent, inspecting.UIDofNeighbor, inspecting.xCoordinate, inspecting.yCoordinate, inspecting.trueDistance, inspecting.guessedDistance );
            bw.write(recordValues);
        }

        bw.close();

    }

    public static void sortCollection(ArrayList<Record> inputList){
        Collections.sort(inputList);
    }

    public static double getQError(ArrayList<Record> inputList, double percentile){
        sortCollection(inputList);
        Record TargetRecord;
        if (percentile <=0){
            TargetRecord = inputList.get(0);
        } else if (percentile >=1){
            TargetRecord =inputList.get(inputList.size()-1);
        } else {
            TargetRecord = inputList.get((int) (inputList.size() * percentile));
        }
        return TargetRecord.calculateGuessRawAbsoluteError();
    }
}
