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
package Visuals;

import simulation.*;

public class VisualInterface {
    //TODO: complete class

    double xBounds, yBounds;
    AgentHolder AgentHolder;
    Metrics Metrics;

    AgentFrame AgentFrameInstance;
    MetricFrame MetricFrameInstance;

    public VisualInterface(double xBounds, double yBounds, AgentHolder AgentHolder, Metrics Metrics){
            this.xBounds = xBounds;
            this.yBounds = yBounds;
            this.AgentHolder = AgentHolder;
            this.Metrics = Metrics;

            AgentFrameInstance = new AgentFrame((int) xBounds, (int) yBounds, AgentHolder, Metrics);
            //TODO uncomment and fix visuals
            MetricFrameInstance = new MetricFrame(AgentHolder, Metrics);
    }

    public void repaintAll(){
        AgentFrameInstance.updateGraphics();
        MetricFrameInstance.updateGraphics();
    }
}
