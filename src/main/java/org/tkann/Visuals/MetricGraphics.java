package Visuals;

import simulation.*;
import simulation.Record;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;

public class MetricGraphics extends JPanel implements ActionListener {
    boolean verbose = false;

    AgentHolder AgentHolder;
    Metrics Metrics;

    int totalRounds;

    Timer timer = new Timer(1000,this);
    int seconds = 0;

    final int graphicsScale = 1;
    int xBounds, yBounds;

    public MetricGraphics(int totalRounds, int xBounds, int yBounds, AgentHolder AgentHolder, Metrics Metrics){
        this.totalRounds = totalRounds;
        this.xBounds = xBounds;
        this.yBounds = yBounds;

        this.AgentHolder = AgentHolder;
        this.Metrics = Metrics;
    }

    public void startTimer(){
        timer.start();
    }

    public void paintComponent(Graphics g){

        super.paintComponent(g);
        this.setBackground(Color.WHITE);

        Graphics2D g2d = (Graphics2D) g;

        paintGraphBars(g2d);
        paintMetrics(g2d);
        paintLegend(g2d);
    }

    private void paintLegend(Graphics2D g2d) {
        int lengthOfLegend = 200;
        int lengthOfEntry = 20;
        int margin = 5;
        int legendHeight = (PartOfRound.values().length+1) * lengthOfEntry;
        int legendWidth = 200;
        final int Yoffset = 40;
        final int Xoffset = 25;

        boolean bottom = false;
        boolean left = true;
        int startingX;
        int startingY;

        if (left){
            startingX = margin;
        } else {
            startingX = this.xBounds - legendWidth - margin - Xoffset;
        }

        if (bottom) {
            startingY = this.yBounds - legendHeight - margin - Yoffset;
        } else {
            startingY = margin;
        }


        g2d.setColor(Color.BLACK);
        g2d.fillRect(startingX-1,startingY-1,legendWidth+2,legendHeight+2);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(startingX,startingY,legendWidth,legendHeight);
        g2d.setColor(Color.BLACK);
        //g2d.drawRect(0,0,this.xBounds, this.yBounds);

        startingY += lengthOfEntry;

        for (PartOfRound currentPart : PartOfRound.values()){
            g2d.setColor(Color.BLACK);
            g2d.drawString(currentPart.name(), startingX+(margin*3), startingY);
            g2d.setStroke(new BasicStroke(5));
            setColorBasedOnPlot(g2d, currentPart);
            g2d.drawLine(startingX+margin, startingY, startingX+(margin*2), startingY);

            startingY += lengthOfEntry;
        }
    }

    private void paintGraphBars(Graphics2D g2d){
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));

        g2d.drawLine(2,2,2,this.yBounds);
        g2d.drawString("percent error", 4, (int) this.yBounds/2);
        g2d.drawLine(2,this.yBounds-40, this.xBounds, this.yBounds-40);
        g2d.drawString("time instance", 300, this.yBounds-50);

        for (int plottingRound = 0; plottingRound < Metrics.getTotalNumberOfRounds(); plottingRound++){
            if (plottingRound % 5 == 0){
                g2d.drawLine(plottingRound*20, 0, plottingRound*20, this.yBounds);
                g2d.drawString(String.format("%d", plottingRound), plottingRound*20 +2, this.yBounds-70 );
            }
        }
    }

    public void paintMetrics(Graphics2D g2d){

        g2d.setStroke(new BasicStroke(5));

        int[] prevPlotY = new int[PartOfRound.values().length];

        //debug statement
        HashMap<PartOfRound,Double> totalErrorPerPart = new HashMap<PartOfRound, Double>();

        for (int plottingRound = 0; plottingRound < Metrics.getCurrentRound(); plottingRound++){
            int currentY = 0;
            for (PartOfRound currentPart : PartOfRound.values()){

                setColorBasedOnPlot(g2d, currentPart);

                //get record of round and subround
                ArrayList<Record> roundAndSubroundMetrics = Metrics.getAllRecords();
                roundAndSubroundMetrics = Metrics.filterByRound(roundAndSubroundMetrics,plottingRound);
                roundAndSubroundMetrics = Metrics.filterBySubround(roundAndSubroundMetrics, currentPart);
                double dataAtThisPoint = Metrics.getPercentCorrectGuessesOfCollection(roundAndSubroundMetrics) * 100;
                //double dataAtThisPoint = Metrics.getaveragePercentErrorOfCollection(roundAndSubroundMetrics)*100;

                //debugStatements
                {
                    if (plottingRound == 0) {
                        totalErrorPerPart.put(currentPart, dataAtThisPoint);
                    } else {
                        double newError = totalErrorPerPart.get(currentPart) + dataAtThisPoint;
                        totalErrorPerPart.put(currentPart, newError);
                    }
                }

                int toPlotY = (int) dataAtThisPoint*10;

                if (plottingRound == 0){

                } else {
                    g2d.drawLine((plottingRound-1)*20, yBounds - prevPlotY[currentY], plottingRound*20, yBounds - toPlotY);
                }

                prevPlotY[currentY] = toPlotY;
                currentY++;

                if ((plottingRound == Metrics.getCurrentRound()-1 ) ||
                        ((plottingRound%10 == 5) && (plottingRound-Metrics.getCurrentRound())<7)) {
                    String label = String.format("%3.2f", dataAtThisPoint);
                    g2d.drawString(label, plottingRound*20, yBounds - toPlotY - 20 + (40*currentY));
                }
            }

        }

        //debug statements
        if (verbose) {
            for (PartOfRound currentPart : PartOfRound.values()) {
                double avgError = totalErrorPerPart.get(currentPart) / (Metrics.getCurrentRound() + 1);
                System.out.println(avgError + " average error for " + currentPart + " at round " + Metrics.getCurrentRound());
            }
        }
    }

    private void setColorBasedOnPlot(Graphics2D g2d, PartOfRound part){
        switch (part){
            case initialGuess: g2d.setColor(Color.green); break;
            case OneRoundStressMajorization: g2d.setColor(Color.orange); break;
            //case neighborHeuristic: g2d.setColor(Color.RED);
            default: g2d.setColor(Color.MAGENTA); break;
        }
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        seconds++;
        System.out.println(seconds + " seconds");
    }
}
