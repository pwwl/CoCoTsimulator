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
collection of static methods that can be used throughout the program (mainly by agents) that are better implementation-wise
or spatially in a different class.
 @author tkann@andrew.cmu.edu
 **/
public class Utility {

    public static double distanceBetween(Agent agent1, Agent agent2){
        return distanceBetween(agent1.xCoordinate, agent1.yCoordinate, agent2.xCoordinate, agent2.yCoordinate);
    }

    public static double distanceBetween(double xCoord1, double yCoord1, double xCoord2, double yCoord2){
        return Math.sqrt(Math.pow(xCoord1-xCoord2,2)+Math.pow(yCoord1-yCoord2,2));
    }

    public static double guessDistanceFromRSSI(Agent agent1, Agent agent2, int RSSI){
        return (-0.14 * RSSI) - 1.8722;
    }
}
