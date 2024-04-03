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
