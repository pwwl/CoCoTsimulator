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

import java.util.HashMap;
import java.util.Objects;

public class Malicious_Agent extends Agent {
    static final double CUTOFF_FT = 6;

    static double fudgeFactor = 0.1; //used for nDCF breaker strategy

    public enum strategy {
        underGuess,
        overGuess,
        nDCFBreak;
    }
    protected strategy maliciousType;
    /**
     * constructor for a single agent
     *
     * @param UID           unique ID for this agent, MUST BE UNIQUE, acts as a MAC address
     * @param sceneID       possibly non-unique ID for tracking the agent throughout the scene
     * @param xCoord        starting x coordinate
     * @param yCoord        starting y coordinate
     * @param range         maximum range at which the agent can see another agent
     * @param phonePosition the starting position that the agent's phone is held
     * @param Metrics       static reference to the metric instance
     * @param RSSI          static reference to the RSSI instance
     */
    public Malicious_Agent(int UID, int sceneID, double xCoord, double yCoord, double range,
                           Position phonePosition, Malicious_Agent.strategy maliciousType,
                           Metrics Metrics, RSSI_collector RSSI) {
        super(UID, sceneID, xCoord, yCoord, range, phonePosition, Metrics, RSSI);
        this.maliciousType = maliciousType;
    }

    @Override
    public void GetInitialDistanceGuessesToNeighbors() {
        for (Agent neighbor : listOfNeighbors) {
            guessInitialDistanceToSingleNeighbor(neighbor);
        }
    }

    protected double guessInitialDistanceToSingleNeighbor(Agent neighbor) {

        /** able to work with RSSI-esque errors or percent errors **/
        double guess;
        switch (maliciousType){
            case underGuess:
                guess = 1;
                break;

            case overGuess:
                guess = 15;
                break;

            case nDCFBreak:
                guess = nDCFBreak(neighbor);
                break;
            default:
                guess = 1;
                break;
        }

        initialDistanceGuesses.put(neighbor.UID, guess);
        return guess;
    }

    public double nDCFBreak(Agent neighbor){
        double guessToReturn = 0;
        if (neighbor instanceof Malicious_Agent){
            //nearby neighbor is known to be another attacker
            if (this.sceneID==neighbor.sceneID){
                guessToReturn = 0;
            } else {
                guessToReturn = super.guessInitialDistanceToSingleNeighbor(neighbor, 0);
            }
        } else {
            //nearby neighbor is benign, malicious users intend to trick them
            double benignGuess = neighbor.initialDistanceGuesses.get(this.UID);
            if (benignGuess <= CUTOFF_FT){
                guessToReturn = CUTOFF_FT + (CUTOFF_FT - benignGuess) + fudgeFactor;
            } else {
                guessToReturn = CUTOFF_FT - (benignGuess - CUTOFF_FT) - fudgeFactor;
                if (guessToReturn < 0){
                    guessToReturn = 0;
                }
            }
        }
        return guessToReturn;
    }

    @Override
    public void ORMDS_weighted() {
        finalDistanceGuesses = (HashMap<Integer, Double>) initialDistanceGuesses.clone();
    }


    public static strategy parseStrategy(String strategy){
        if (Objects.equals(strategy, "close")){
            return Malicious_Agent.strategy.underGuess;
        } else if (Objects.equals(strategy, "far")){
            return Malicious_Agent.strategy.overGuess;
        } else if (Objects.equals(strategy, "nDCFBreak")){
            return Malicious_Agent.strategy.nDCFBreak;
        } else { //default
            return Malicious_Agent.strategy.underGuess;
        }
    }

}
