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
import javax.swing.*;

public class MetricFrame extends JFrame{

    MetricGraphics Graphics;
    AgentHolder AgentHolder;
    Metrics Metrics;

    int xBounds = 1000;
    int yBounds = 1000;

    public MetricFrame(AgentHolder AgentHolder, Metrics Metrics){
        int xBounds = Metrics.getTotalNumberOfRounds()*20;
        this.setSize(xBounds, yBounds);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.AgentHolder = AgentHolder;
        this.Metrics = Metrics;
        Graphics = new MetricGraphics(Metrics.getTotalNumberOfRounds(), xBounds, yBounds , AgentHolder, Metrics);

        this.add(Graphics);
        this.setVisible(true);
    }

    public void updateGraphics(){
        Graphics.repaint();
    }
}
