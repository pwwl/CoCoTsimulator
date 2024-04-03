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
