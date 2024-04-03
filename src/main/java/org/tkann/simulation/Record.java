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


/**
the bread and butter of the metrics class. This is just a complex struct which is used to house
information about the guesses that the agents make and keep track of accuracy over time.
 */

public class Record implements Comparable<Record>{

    //what we really care about in a record
    double trueDistance;
    double guessedDistance;
    boolean correctGuess;

    //some other useful stuff to search by in our records
    int UIDofGuessingAgent;
    int UIDofNeighbor;
    int numberOfNeighborsSeen;

    double xCoordinate, yCoordinate;
    Sector sectorItOccurredIn;
    int roundItOccurredIn;
    PartOfRound partOfRoundTheRecordOccurredIn;

    double averageStress;

    //constructor
    public Record(Agent recordingAgent, Agent neighborAgent, double guessedDistance, int roundNumber, PartOfRound part){
        UIDofGuessingAgent = recordingAgent.UID;
        UIDofNeighbor = neighborAgent.UID;

        roundItOccurredIn = roundNumber;
        this.partOfRoundTheRecordOccurredIn = part;

        xCoordinate = recordingAgent.xCoordinate;
        yCoordinate = recordingAgent.yCoordinate;
        sectorItOccurredIn = recordingAgent.currentSector;
        numberOfNeighborsSeen = recordingAgent.numberOfNeighbors();

        trueDistance = Utility.distanceBetween(recordingAgent, neighborAgent);
        this.guessedDistance = guessedDistance;

        //this isn't the TC4TL metric that we're ultimately going for, but the CDC has decided that 6 feet is
        //the magic number, so that's our target
        correctGuess = (guessedDistance<=6) == (trueDistance<=6);
    }

    public Record(Agent recordingAgent, Agent neighborAgent, double guessedDistance, int roundNumber, PartOfRound part, double averageStress){
        this(recordingAgent, neighborAgent,guessedDistance,roundNumber,part);
        this.averageStress = averageStress;
    }

    //get the error of the entry
    public double calculateGuessRawAbsoluteError(){
        return Math.abs(guessedDistance - trueDistance);
    }

    public double calculateGuessPercentError() {
        return Math.abs(guessedDistance - trueDistance) / trueDistance;
    }

    @Override
    public int compareTo(Record o) {
        Double thisError = this.calculateGuessRawAbsoluteError();
        Double oError = o.calculateGuessRawAbsoluteError();
        return thisError.compareTo(oError);
    }
}

