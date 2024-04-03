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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.*;

public class SceneBuilder {
    /*
    simulation.sceneBuilder is a class that is used for returning an agent holder class
     and is used also for updating that agent holder class

     the main benefit of using this over the agent holder is that this has added utility to read
     an agent listing from a file instead of just randomly putting the agents everywhere as well
     as providing a more human friendly interface when accessing the agent holder and auto-calling
     methods that need to be rand together.
     */

    final int HEADER_INFORMATION_INDEX = -1;

    AgentHolder AgentHolder;

    //information about the file we are reading from, if we are even reading from it
    private boolean fromFile; //everything is parameterized to be random if we're not using a file
    private File sceneFile;
    private Scanner SC;

    private HashMap<Integer, HashMap<Integer, Double[]>> roundMappings;

    //this might be used later if the simulation uses information about the RSSI such as "indoors" or "outdoors",
    //for now it's unused
    private Scene currentScene;

    //information about the rounds
    private int roundNumber;
    private long simulationTime = 0;
    private int totalRounds;
    private double totalNumberOfMinutes;

    //information for the agent holder and agents
    private double xBounds, yBounds;
    private int numberOfAgents,  numberOfCurrentMaliciousAgents;
    public static int numberOfMaliciousAgents = 1; //todo should be in agent file but parameterizer needs to be updated first
    public static int  maliciousIDCount = 1;//number of IDs each malicious agents get for sybil attacks
    public static Malicious_Agent.strategy strategy = Malicious_Agent.strategy.underGuess;
    public static boolean averageOut, stressMajorization, cliqueMDS, weightedSprings, stressMajDropNeighbor, stressMajDropLink;
    public static boolean symmetricDefault = false;
    public static void setParams(int numberOfMaliciousAgents, int maliciousIDCount, Malicious_Agent.strategy strategy,
                                 boolean averageOut, boolean stressMajorization,
                                 boolean cliqueMDS, boolean weightedSprings,
                                 boolean stressMajDropNeighbor, boolean stressMajDropLink){
        SceneBuilder.numberOfMaliciousAgents = numberOfMaliciousAgents;
        SceneBuilder.maliciousIDCount = maliciousIDCount;
        SceneBuilder.strategy = strategy;
        SceneBuilder.averageOut = averageOut;
        SceneBuilder.stressMajorization = stressMajorization;
        SceneBuilder.cliqueMDS = cliqueMDS;
        SceneBuilder.weightedSprings = weightedSprings;
        SceneBuilder.stressMajDropNeighbor = stressMajDropNeighbor;
        SceneBuilder.stressMajDropLink = stressMajDropLink;
    }

    //static references to objects passed to the simulation.agent by the simulation.simulation to keep track of RSSIs and Metrics
    static Metrics Metrics;
    static RSSI_collector RSSI;

    /**constructor if the simulation uses a file**/
    public SceneBuilder(File sceneFile, Metrics Metrics, RSSI_collector RSSI) throws FileNotFoundException {
        this.sceneFile = sceneFile;
        this.fromFile = true;

        SC = new Scanner(sceneFile).useDelimiter("\n");

        String numberWithoutDecimal = "\\d+";
        String numberWithOptionalDecimal = "\\d+\\.\\d*";

        Pattern totalMinutesPattern = Pattern.compile("(" + numberWithOptionalDecimal + ")\\s+minutes");
        Pattern totalAreaPattern = Pattern.compile("(" + numberWithOptionalDecimal + ")\\s+(" + numberWithOptionalDecimal + ")\\s+area");
        Pattern settingPattern = Pattern.compile("(initialize|(round)\\s+(" + numberWithoutDecimal + ")):(\\s+time\\s+(" +  numberWithoutDecimal + "))?");
        Pattern createAgentPattern = Pattern.compile("agent\\s+(" + numberWithoutDecimal + "):");
        Pattern setRangePattern = Pattern.compile("(\\d+.\\d*)\\s+range");
        Pattern setPositionPattern = Pattern.compile("(\\w+)\\s+phone");
        Pattern updateAgentPattern = Pattern.compile("(" + numberWithoutDecimal + ")\\s+("
                + numberWithOptionalDecimal + "|nan)\\s+(" + numberWithOptionalDecimal + "|nan)");


        int initializingRound = HEADER_INFORMATION_INDEX;
        int agent = 0;
        int smallestAgent = 0;
        int largestAgent = 0;
        int largestRound = 0;

        roundMappings = new HashMap<>();

        //parser for agent file
        while (SC.hasNext()){
            String currentLine = SC.nextLine();

            Matcher totalMinutesMatcher = totalMinutesPattern.matcher(currentLine);
            Matcher totalAreaMatcher = totalAreaPattern.matcher(currentLine);
            Matcher settingMatcher = settingPattern.matcher(currentLine);
            Matcher createAgentMatcher = createAgentPattern.matcher(currentLine);
            Matcher setRangeMatcher = setRangePattern.matcher(currentLine);
            Matcher setPositionMatcher = setPositionPattern.matcher(currentLine);
            Matcher updateAgentMatcher = updateAgentPattern.matcher(currentLine);

            if (totalMinutesMatcher.find()) {
                this.totalNumberOfMinutes = Integer.parseInt(totalMinutesMatcher.group(1));
            } else if (totalAreaMatcher.find()){
                this.xBounds = Double.parseDouble(totalAreaMatcher.group(1));
                this.yBounds = Double.parseDouble(totalAreaMatcher.group(2));
            } else if (settingMatcher.find()){
                if (settingMatcher.group(0).contains("initialize")){
                    initializingRound = HEADER_INFORMATION_INDEX;
                    roundMappings.put(initializingRound,new HashMap<>());
                } else {
                    initializingRound = Integer.parseInt(settingMatcher.group(3));
                    roundMappings.put(initializingRound,new HashMap<>());
                    if (initializingRound > largestRound){
                        largestRound = initializingRound;
                    }

                    double roundTime;
                    if (settingMatcher.group(4)==null || settingMatcher.group(4).isEmpty()) {
                        roundTime = Double.NaN;
                    } else {
                            roundTime = (double) Long.parseLong(settingMatcher.group(5));
                    }
                    roundMappings.get(initializingRound).put(HEADER_INFORMATION_INDEX, new Double[] {roundTime});
                }
            } else if (createAgentMatcher.find()){
                agent = Integer.parseInt(createAgentMatcher.group(1));
                if (agent > largestAgent){
                    largestAgent = agent;
                }
                if (agent < smallestAgent){
                    smallestAgent = agent;
                }
                Double[] agentProperties = {0.0,0.0};
                roundMappings.get(initializingRound).put(agent,agentProperties);
            } else if (setRangeMatcher.find()){
                Double[] agentProperties = roundMappings.get(initializingRound).get(agent);
                agentProperties[0] = Double.parseDouble(setRangeMatcher.group(1));
                roundMappings.get(initializingRound).put(agent,agentProperties);
            } else if (setPositionMatcher.find()){
                Double[] agentProperties = roundMappings.get(initializingRound).get(agent);
                Position position = Position.valueOf(setPositionMatcher.group(1));
                agentProperties[1] = (double) position.ID;
                roundMappings.get(initializingRound).put(agent,agentProperties);
            } else if (updateAgentMatcher.find()){
                agent = Integer.parseInt(updateAgentMatcher.group(1));
                Double[] agentProperties;
                try {
                    agentProperties = new Double[] {Double.parseDouble(updateAgentMatcher.group(2)), Double.parseDouble(updateAgentMatcher.group(3))};
                } catch (NumberFormatException e){
                    agentProperties = new Double[] {Double.NaN, Double.NaN};
                }
                roundMappings.get(initializingRound).put(agent,agentProperties);
            }
        }

        SC.close();

        this.totalRounds = largestRound+1;
        Metrics.setTotalNumberOfRounds(this.totalRounds);
        this.numberOfAgents = largestAgent+1;
        this.roundNumber = 0;

        this.Metrics = Metrics;
        this.RSSI = RSSI;

        AgentHolder = new AgentHolder(numberOfAgents, numberOfMaliciousAgents, maliciousIDCount, strategy, this.xBounds, this.yBounds, Metrics, RSSI);
    }

    /**
     * creates an agent. This is a function because it can conditionally return a malicious agent.
     * @param newAgentUID UID of agent to be created
     * @param sceneID UID of agent for scene-builder purpose
     * @param newAgentXCoord starting x coordinate of agent
     * @param newAgentYCoord starting y coordinate of agent
     * @param newAgentRange max radius of nearby agents that this agent can see
     * @param newAgentPosition position that the agent will be holding their phone in
     * @param maliciousAgentCount the total number of malicious agents desired. The agent that is malicious
     *                            will be randomly assigned but this guarantees that this many agents will be
     *                            malicious once all have been created.
     *                            p = malicious agents left / total agents left to create
     * @return an agent that is probabilistically malicious or not.
     */
/*    private agent createAgent(int newAgentUID, int sceneID, double newAgentXCoord, double newAgentYCoord,
                              double newAgentRange, Position newAgentPosition, int maliciousAgentCount,
                              simulation.malicious_Agent.strategy type, int sybilIdentityCount) {
        boolean malicious;
        int maliciousAgentsLeft = maliciousAgentCount - numberOfCurrentMaliciousAgents;
        int agentsLeft = numberOfAgents - 1 - newAgentUID;
        double probMalicious =  (double) maliciousAgentsLeft / (double) agentsLeft;
        malicious = Math.random() < probMalicious;
        if (malicious){
            numberOfCurrentMaliciousAgents++;
            return new malicious_Agent(newAgentUID, newAgentXCoord, newAgentYCoord,
                    newAgentRange, newAgentPosition,type,
                    Metrics, RSSI);
        } else {
            return new agent(newAgentUID, newAgentXCoord, newAgentYCoord, newAgentRange,
                    newAgentPosition, Metrics, RSSI);
        }
    }*/

    /**constructor for the use without a simulation file and to just randomly disperse the agents**/
    public SceneBuilder(double bounds, int numberOfAgents, int numberOfRounds, Metrics Metrics, RSSI_collector RSSI){
        fromFile = false;

        this.xBounds = bounds;
        this.yBounds = bounds;

        this.numberOfAgents = numberOfAgents;
        this.totalRounds = numberOfRounds;
        this.roundNumber = 0;

        this.Metrics = Metrics;
        this.RSSI = RSSI;

        AgentHolder = new AgentHolder(numberOfAgents, numberOfMaliciousAgents, maliciousIDCount,  strategy, bounds,  bounds, Metrics, RSSI);
    }

    //used to fill out the agentHolder once the sceneBuilder has been instantiated
    public AgentHolder buildScene(){
        this.numberOfCurrentMaliciousAgents = 0;
        if (fromFile == false){
            for (int newAgentUID = 0; newAgentUID < numberOfAgents; newAgentUID++){
                double newAgentXCoord = Math.random()*xBounds;
                double newAgentYCoord = Math.random()*yBounds;
                double newAgentRange = (Math.random() * .3 + 0.7) * AgentHolder.agentMaxRange;
                Position newAgentPosition = RSSI.assignRandomPosition();

                AgentHolder.addAgent(newAgentXCoord, newAgentYCoord,
                                    newAgentRange, newAgentPosition);
            }
        } else {
            for (int newAgentUID = 0; newAgentUID < numberOfAgents; newAgentUID++){
                Double[] agentProperties = roundMappings.get(HEADER_INFORMATION_INDEX).get(newAgentUID);
                Double[] agentPropertiesRound0 = roundMappings.get(0).get(newAgentUID);

                double newAgentRange;
                Position newAgentPosition;
                double newAgentXCoord, newAgentYCoord;

                if (agentProperties == null){
                    newAgentRange = 0;
                    newAgentPosition = RSSI.assignRandomPosition();
                } else {
                    newAgentRange = agentProperties[0];
                    int positionIndex = (int) Math.floor(agentProperties[1]);
                    newAgentPosition = Position.values()[positionIndex];
                }

                if (agentPropertiesRound0 == null){
                    newAgentXCoord = Double.NaN;
                    newAgentYCoord = Double.NaN;
                } else {
                    newAgentXCoord = agentPropertiesRound0[0];
                    newAgentYCoord = agentPropertiesRound0[1];
                }

                AgentHolder.addAgent(newAgentXCoord, newAgentYCoord,
                        newAgentRange, newAgentPosition);

            }
        }
        return AgentHolder;
    }

    public void resetScene(){
        //TODO: complete method

    }

    /** sets the scene to a specific round*/
    public void setSceneToRound(int roundNumber){
        updateAgentsLocations(roundNumber);
        AgentHolder.resetAgentsForRound();
        AgentHolder.fitAgentsIntoSectors();
        AgentHolder.findAgentNeighbors(symmetricDefault);
        this.roundNumber = roundNumber;
    }

    /**
     *the scene can be used by the simulation to act as an iterator throughout the rounds
     */
    public boolean hasNextPosition(){
        return roundNumber < totalRounds;
    }

    /**
    this runs a single round of the scene, during each round the agents
    poll one another initially for a guess and then broadcast those guesses to
    their neighbors for them to update their old guesses with a new
    graph drawing. this is subject to change during different iterations and
    that is why a single function can be called by the sceneBuilder to run
    and entire round.
     */
    public void runSceneRound(){
        updateAgentsLocations(roundNumber);
        AgentHolder.resetAgentsForRound();
        AgentHolder.fitAgentsIntoSectors();
        AgentHolder.findAgentNeighbors(symmetricDefault, simulationTime);
        //AgentHolder.randomlyDropNeighborsForAllAgents(0,true);
        AgentHolder.startRound(simulationTime,
                averageOut, stressMajorization,
                cliqueMDS, weightedSprings,
                stressMajDropNeighbor, stressMajDropLink);

        roundNumber++;
    }


    private void updateAgentsLocations(int roundNumber){
        if (fromFile == false){
            updateAgentsLocationsFromRandom(roundNumber);
        } else {
            updateAgentsLocationsFromFile(roundNumber);
        }
    }

    private void updateAgentsLocationsFromFile(int roundNumber){
        Map<Integer, Double[]> mappingsForThisRound = roundMappings.get(roundNumber);

        if (mappingsForThisRound.containsKey(HEADER_INFORMATION_INDEX)){
            Double[] mappingHeader =  mappingsForThisRound.get(HEADER_INFORMATION_INDEX);
            Double timeFromHeader = mappingHeader[0];
            if (timeFromHeader!=null && !timeFromHeader.isNaN()){
                simulationTime = timeFromHeader.longValue();
                Agent.simulationTime = simulationTime;
            }
        }

        for (Agent currentAgent : AgentHolder.allAgents){
            Double[] agentProperties = mappingsForThisRound.get(currentAgent.sceneID);
            if (agentProperties == null){
                currentAgent.setCoordinates(Double.NaN,Double.NaN);
            } else {
                currentAgent.setCoordinates(agentProperties[0], agentProperties[1]);
            }
        }
    }

    private void updateAgentsLocationsFromRandom(int roundNumber){
        if (roundNumber == 0){
            return;
        }
        for (Agent currentAgent : AgentHolder.allAgents){
            double newXCoord = currentAgent.xCoordinate + ( 6 * (Math.random() - 0.5));
            newXCoord = (newXCoord + xBounds) % xBounds;
            double newYCoord = currentAgent.yCoordinate + ( 6 * (Math.random() - 0.5));
            newYCoord = (newYCoord + yBounds) % yBounds;
            currentAgent.setCoordinates(newXCoord, newYCoord);
        }
    }

    public int getTotalRounds(){
        return totalRounds;
    }
}
