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

import java.util.ArrayList;
import java.util.Random;

/**
a single class that holds our agent array as well as keeps
track of the sectors they're in and commands them all
*/

public class AgentHolder {

    //details about the field that the agents reside in
    double xBounds;
    double yBounds;

    //results that will be derived based on the details above
    int sectorBounds; //we use a sectorized system to avoid n^2 complexity of neighbor discovery
    int numberOfXSectors;
    int numberOfYSectors;
    public Sector[][] sectors;

    //details about the agents that we house
    int numberOfAgents, numberOfMaliciousAgents, currentNumberOfAgents;
    int currentNumberOfIdentities, currentNumberOfBenignIdentities, currentNumberOfMaliciousIdentities; //identities is used instead of agents because sybils appear as multiple agents
    int SybilIdentityCount;
    Malicious_Agent.strategy adversaryStrategy;
    public ArrayList<Agent> allAgents, allBenignAgents, allMaliciousAgents;

    final double agentMaxRange = 30;

    //static references to objects passed to the agent by the simulation to keep track of RSSIs and Metrics,
    //these are global references
    static Metrics Metrics;
    static RSSI_collector RSSI;
    static Random rand = new Random();
    public static long seed = 8008135;

    public static DistanceMeasure distanceMeasure;
    public static String precompiledDistances;

    //constructor
    public AgentHolder(int numberOfAgents, int numberOfMaliciousAgents, int maliciousSybilIdentities,
                       Malicious_Agent.strategy strategy,
                       double bounds, Metrics Metrics, RSSI_collector RSSI){
        this(numberOfAgents, numberOfAgents, maliciousSybilIdentities,
                            strategy,
                            bounds,bounds,Metrics,RSSI);
    }

    public AgentHolder(int numberOfAgents, int numberOfMaliciousAgents, int maliciousSybilIdentities,
                       Malicious_Agent.strategy strategy,
                       double Xbounds, double Ybounds, Metrics Metrics, RSSI_collector RSSI){
        this.numberOfAgents = numberOfAgents;
        this.numberOfMaliciousAgents = numberOfMaliciousAgents;
        this.SybilIdentityCount = maliciousSybilIdentities;
        this.adversaryStrategy = strategy;
        this.currentNumberOfAgents = 0;
        this.currentNumberOfIdentities = 0;
        this.currentNumberOfBenignIdentities = 0;
        this.currentNumberOfMaliciousIdentities = 0;

        this.xBounds = Xbounds;
        this.yBounds = Ybounds;

        this.Metrics = Metrics;
        this.RSSI = RSSI;

        rand = new Random();
        rand.setSeed(seed);

        allAgents = new ArrayList<Agent>();
        allBenignAgents = new ArrayList<Agent>();
        allMaliciousAgents = new ArrayList<Agent>();

        createSectors();
    }

    public ArrayList<Agent> getActiveAgents(){
        ArrayList<Agent> activeAgents = new ArrayList<Agent>();
        for (Agent a : allAgents){
            if (a.active){
                activeAgents.add(a);
            }
        }
        return activeAgents;
    }

    public ArrayList<Agent> getActiveBenignAgents(){
        ArrayList<Agent> activeAgents = new ArrayList<Agent>();
        for (Agent a : allBenignAgents){
            if (a.active){
                activeAgents.add(a);
            }
        }
        return activeAgents;
    }

    public ArrayList<Agent> getActiveMaliciousAgents(){
        ArrayList<Agent> activeAgents = new ArrayList<Agent>();
        for (Agent a : allMaliciousAgents){
            if (a.active){
                activeAgents.add(a);
            }
        }
        return activeAgents;
    }

    /*
    we use sectors to avoid the findNeighbors() function being n^2 time and instead make it roughly constant time.

    the sectors are as large as the largest agent's range and therefore garuntee that all neighbors lie at most 1 sector
    away from the current agent.
     */
    private void createSectors(){
        //sectors are based on the agent max range, therefore we overshoot on the sectors to ensure we're covered
        //this tells us how many sectors we need
        numberOfXSectors = (int) Math.ceil(xBounds/agentMaxRange);
        numberOfYSectors = (int) Math.ceil(yBounds/agentMaxRange);

        //instantiate the array of sectors
        sectors = new Sector[numberOfXSectors][numberOfYSectors];

        for (int x = 0; x < numberOfXSectors; x++) {
            for (int y = 0; y < numberOfYSectors; y++) {
                sectors[x][y] = new Sector(x*agentMaxRange,(x+1)*agentMaxRange,y*agentMaxRange,(y+1)*agentMaxRange,
                        x, y, Metrics);
            }
        }
    }

    public boolean addAgent(double newAgentXCoord, double newAgentYCoord,
                         double newAgentRange, Position newAgentPosition){
        int maliciousAgentsLeft = numberOfMaliciousAgents - currentNumberOfMaliciousIdentities;
        int totalAgentsLeft = numberOfAgents - 1 - currentNumberOfIdentities;

        double probMalicious =  (double) maliciousAgentsLeft / (double) totalAgentsLeft;
        boolean malicious = rand.nextDouble() <= probMalicious;

        int sceneID = currentNumberOfIdentities;

        if (malicious){
            /*
             * make S bad agents at the same location. functionally much easier than a unique
             * sybil type agent.
             */
            for(int i = 0; i < SybilIdentityCount; i++) {
                Agent agentToAdd = new Malicious_Agent(currentNumberOfAgents, sceneID, newAgentXCoord, newAgentYCoord,
                        newAgentRange, newAgentPosition, adversaryStrategy,
                        Metrics, RSSI);
                allAgents.add(agentToAdd);
                allMaliciousAgents.add(agentToAdd);
                currentNumberOfAgents++;
            }
            currentNumberOfMaliciousIdentities++;
            currentNumberOfIdentities++;
        } else {
            //make one benign agent
            Agent agentToAdd = new Agent(currentNumberOfAgents, sceneID, newAgentXCoord, newAgentYCoord,
                    newAgentRange, newAgentPosition,
                    Metrics, RSSI);
            allAgents.add(agentToAdd);
            allBenignAgents.add(agentToAdd);
            currentNumberOfAgents++;
            currentNumberOfBenignIdentities++;
            currentNumberOfIdentities++;
        }
        return malicious;
    }

    public void purgeAgentsFromSectors(){
        for (int xSector = 0; xSector < sectors.length; xSector++){
            for (int ySector = 0; ySector < sectors[xSector].length; ySector++){
                sectors[xSector][ySector].purge();
            }
        }
    }

    //just matches agents up with where they physically are into a sector that contains them
    //this is necesarry each round since the agents move around
    protected void fitAgentsIntoSectors(){

        purgeAgentsFromSectors();

        for (Agent currentAgent : getActiveAgents()){
            //the sectors know their bounds and can verify that the agent does reside in it, but this way is faster
            int xSector = (int) Math.floor(currentAgent.xCoordinate /agentMaxRange);
            int ySector = (int) Math.floor(currentAgent.yCoordinate /agentMaxRange);
            sectors[xSector][ySector].add(currentAgent);
            currentAgent.putIntoSector(sectors[xSector][ySector]);
        }
    }

    //used when gathering neighbors to get the sectors that are adjacent or cornering any given sector,
    //works modularly since it is cleaner that way even though sometimes i over deliver
    public Sector[] getAdjacentSectorsToSector(int xSectorIndex, int ySectorIndex){
        Sector[] arrayOfNearbySectors = new Sector[9]; //because it's modular always return 9 sectors, including the one given

        arrayOfNearbySectors[0] = sectors[xSectorIndex][ySectorIndex];
        arrayOfNearbySectors[1] = sectors[(xSectorIndex-1+ numberOfXSectors)% numberOfXSectors][ySectorIndex];
        arrayOfNearbySectors[2] = sectors[(xSectorIndex-1+ numberOfXSectors)% numberOfXSectors][(ySectorIndex+1)% numberOfYSectors];
        arrayOfNearbySectors[3] = sectors[xSectorIndex][(ySectorIndex+1)% numberOfYSectors];
        arrayOfNearbySectors[4] = sectors[(xSectorIndex+1)% numberOfXSectors][(ySectorIndex+1)% numberOfYSectors];
        arrayOfNearbySectors[5] = sectors[(xSectorIndex+1)% numberOfXSectors][ySectorIndex];
        arrayOfNearbySectors[6] = sectors[(xSectorIndex+1)% numberOfXSectors][(ySectorIndex-1+ numberOfYSectors)% numberOfYSectors];
        arrayOfNearbySectors[7] = sectors[xSectorIndex][(ySectorIndex-1+ numberOfYSectors)% numberOfYSectors];
        arrayOfNearbySectors[8] = sectors[(xSectorIndex-1+ numberOfXSectors)% numberOfXSectors][(ySectorIndex-1+ numberOfYSectors)% numberOfYSectors];

        return arrayOfNearbySectors;
    }

    //same as function above but can be used by an agent or in reference to an agent via the function signature
    public Sector[] getAdjacentSectorsToSector(Agent agentInQuestion){
        int xSector = (int) Math.floor(agentInQuestion.xCoordinate /agentMaxRange);
        int ySector = (int) Math.floor(agentInQuestion.yCoordinate /agentMaxRange);
        return getAdjacentSectorsToSector(xSector, ySector);
    }

    //returns a list of all the agents that are contained in an array of given sectors
    //this makes finding neighbors a lot easier since we can itterate through just one list instead of two nested lists
    public ArrayList<Agent> getAgentsFromSectors(Sector[] sectorsToLookAt){
        ArrayList<Agent> agentsFromSectors = new ArrayList<Agent>();
        for (int i = 0; i < sectorsToLookAt.length; i++){
            agentsFromSectors.addAll(sectorsToLookAt[i].agents);
        }
        return agentsFromSectors;
    }

    //purgest all agents' lists of neighbors
    //used at the start of a round
    protected void resetAgentsForRound(){
        for (Agent currentAgent : getActiveAgents()){
            currentAgent.resetAgentForRound();
        }
    }

    protected void findAgentNeighbors(boolean symmetric){
        findAgentNeighbors(symmetric, 0);
    }

    protected void findAgentNeighbors(boolean symmetric, long time){
        if (distanceMeasure == DistanceMeasure.DATASET){
            findAgentNeighborsByPrecompiledDistances(symmetric, time);
        } else {
            findAgentNeighborsByLocality(symmetric);
        }

    }

    //gathers all neighbors that are within an agent's range
    //this does not drop neighbors, for that use dropRandomNeighbors()
    //symmetric adds neighbors symmetrically, i.e. if i see you and you don't see me we can't be neighbors since it's
    //asymmetric vision. if it's off, that's allowed and one neighbor will see the other but not vice versa.
    //a third option could be added to make it so that the distance has to be less than the maximum vision but this violates
    //the assumption that all seen neighbors are within a range r
    protected void findAgentNeighborsByLocality(boolean symmetric){
        for (Agent currentAgent : getActiveAgents()){
            //iterate through all agents
            //sectorized aproach prevents us from needing another for all agents
            Sector[] potentialSectors = getAdjacentSectorsToSector(currentAgent);
            ArrayList<Agent> allPotentialNeighbors = getAgentsFromSectors(potentialSectors);

            for (Agent potentialNeighbor : allPotentialNeighbors) {
                //make sure we're not considering ourself a neighbor
                //although we need to know the distance to ourself = 0, this is implicit and should/can not be done
                //through RSSI or any other means, therefore we do it explicitly later
                if (potentialNeighbor.UID != currentAgent.UID) {

                    double distanceBetween = Utility.distanceBetween(currentAgent, potentialNeighbor);

                    if (symmetric) {
                        if ((distanceBetween < currentAgent.range) && (distanceBetween < potentialNeighbor.range)) {
                            currentAgent.addNeighbor(potentialNeighbor);
                            potentialNeighbor.addNeighbor(currentAgent);
                        }
                    } else {
                        if (distanceBetween < currentAgent.range) {
                            currentAgent.addNeighbor(potentialNeighbor);
                        }
                    }
                }
            }
        }
    }

    private void findAgentNeighborsByPrecompiledDistances(boolean symmetric, long time){
        for (Agent reciever : getActiveAgents()){
            for (Agent broadcaster : getActiveAgents()){
                boolean neigbhors = RSSI.checkIfAgentsAreNeighbors(time, reciever.UID, broadcaster.UID, symmetric);
                if (!neigbhors) {
                    continue;
                }

                reciever.addNeighbor(broadcaster);
                if (symmetric){
                    broadcaster.addNeighbor(reciever);
                }
            }
        }
    }

    //drops neighbors randomly based on a drop rate double that represents a percentage.
    //symmetric means that *both* neighbors will drop one another if one chooses to drop the other
    protected void randomlyDropNeighborsForAllAgents(double dropRate, boolean symmetric){
        for (Agent currentAgent : getActiveAgents()){
            currentAgent.randomlyDropNeighbors(dropRate, symmetric);
        }
    }


    //Starts the simulation round.
    //this is commanded by the simulation which controlls the rounds but what the agents do is decided by the agent holder.
    public void startRound(long simulationTime,
                           boolean averageOut, boolean stressMajorization,
                           boolean cliqueMDS, boolean weightedSprings,
                           boolean stressMajDropNeighbor, boolean stressMajDropLink){
        /**
        at the start of the round all agents will ping their neighbors and get an initial distance guess
        based on the RSSI value and the position of the agents' phones
         */

        for (Agent currentAgent : getActiveBenignAgents()){
            currentAgent.GetInitialDistanceGuessesToNeighbors();
        }
        for (Agent currentAgent : getActiveMaliciousAgents()){
            currentAgent.GetInitialDistanceGuessesToNeighbors();
        }
        //debug: check to see why some agents have null values in their neighbors
        for (Agent currentAgent : getActiveAgents()){
            if (currentAgent.initialDistanceGuesses.size() != currentAgent.listOfNeighbors.size()){
                currentAgent.GetInitialDistanceGuessesToNeighbors();
            }
        }
        if(averageOut) {
            for (Agent currentAgent : getActiveAgents()) {
                currentAgent.AverageOutMethod();
            }
        }

        /**
         once the agents have a rough guess of their neighbors, they gather the information their neighbors know
         and create a graph drawing to see if they can learn anything else from their neighbors
         as well as mitigate noise from the RSSi measurements
        **/
        if (stressMajorization) {
            for (Agent currentAgent : getActiveAgents()){
                //TODO uncomment once timing issue is fixed
                currentAgent.OneRoundStressMajorization();
            }
        }

        if (cliqueMDS) {
            for (Agent currentAgent : getActiveAgents()){
                currentAgent.CliqueMDS();
            }
        }
        if (weightedSprings) {
            for (Agent currentAgent : getActiveAgents()){
                currentAgent.ORMDS_weighted();
            }
        }
        if (stressMajDropNeighbor) {
            for (Agent currentAgent : getActiveAgents()){
                currentAgent.ORMDS_weighted_dropOne();
            }
        }

        if (stressMajDropLink) {
            for (Agent currentAgent : getActiveAgents()){
                currentAgent.ORMDS_weighted_dropLink();
            }
        }
    }

    //removes all the agents from the simulation
    private void purgeAgentsFromExistence(){
        for (Agent currentAgent : getActiveAgents()){
            //sector is the only thing that the agents point to that I though might be useful to purge
            //even though java has a garbage collector this might avoid circular references and make it faster idk,
            //can't really hurt
            currentAgent.currentSector = null;
        }

        //it is a necessity that the sectors do not contain old agents, so purging each sector is a must
        purgeAgentsFromSectors();

        //at this point, the agent holder should be the only reference to all of the agents
        //and therefore we can just purge the list.
        allAgents.clear();
    }


}
