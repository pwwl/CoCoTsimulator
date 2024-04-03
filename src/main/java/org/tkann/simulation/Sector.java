package simulation;

import java.util.ArrayList;

/**
a class used for holding agents for our nearby neighbor search to run efficiently
 @author tkann@andrew.cmu.edu
 */

public class Sector {

    ArrayList<Agent> agents;

    /**it's usefull for the sector to know certain things about what the simulation thinks of it*/
    double xMin, xMax, yMin, yMax;
    int xSectorCoordinate, ySectorCoordinate;

    static Metrics Metrics;

    /**
     * standard constructor for an individual sector
     * @param xMin the left x limit of the sector
     * @param xMax the right x limit of the sector
     * @param yMin the upper y limit of the sector
     * @param yMax the lower y limit of the sector
     * @param xSectorCoordinate the x index of the sector in its holding 2d array
     * @param ySectorCoordinate the y index of the sector
     * @param Metrics static reference to the metrics instance
     */
    public Sector(double xMin, double xMax, double yMin, double yMax, int xSectorCoordinate, int ySectorCoordinate, Metrics Metrics){
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;

        this.xSectorCoordinate = xSectorCoordinate;
        this.ySectorCoordinate = ySectorCoordinate;

        agents = new ArrayList<Agent>();

        Sector.Metrics = Metrics;
    }

    public void add(Agent toAdd) {
        agents.add(toAdd);
    }

    public void purge() {
        agents.clear();
    }

    public int getNumberOfAgents(){
        return agents.size();
    }

    public double getAccuracyOfSector(){
        ArrayList<Record> myRecords = Metrics.getAllRecords();
        myRecords = Metrics.filterBySector(myRecords, this);
        return Metrics.getPercentCorrectGuessesOfCollection(myRecords);
    }


    public double getxMin(){
        return xMin;
    }
    public double getyMin(){
        return yMin;
    }

    public double getxMax() {
        return xMax;
    }
    public double getyMax(){
        return yMax;
    }
}
