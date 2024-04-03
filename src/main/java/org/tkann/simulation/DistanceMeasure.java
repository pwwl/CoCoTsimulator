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

public enum DistanceMeasure {
    RSSI,
    Rand,
    ML,
    MLRSSIHybrid,
    DATASET;

    public static DistanceMeasure getDistanceMeasure(String measure) {
        switch (measure) {
            case "RSSI":
                return RSSI;
            case "Rand":
                return Rand;
            case "ML":
                return ML;
            case "MLRSSIHybrid":
                return MLRSSIHybrid;
            case "DATASET":
                return DATASET;
            default:
                return RSSI;
        }
    }
}