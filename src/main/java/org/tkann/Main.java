import simulation.*;
import GraphDrawer.graphDrawer;
import simulation.Record;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class Main {
    static String settingFile;
    static double bounds;
    static int numberOfRounds;
    static double weightBias;
    static double weightSeparation;
    static double weightDiscrepancy;
    static boolean fractionInsteadOfExponent;
    static double thresholdStressToCutoffMin;
    static double thresholdStressToCutoffMax;
    static int numAgents;
    static int numberMaliciuos;
    static int maliciousIDCount;
    static Malicious_Agent.strategy maliciousStrategy;
    static double alpha;
    static boolean relativeConfusion;
    static boolean writeOutputHeader;
    static String dataLocation;
    static String precompiledDistances;
    static String outputDestination;
    static String fullOutputDestination;
    public static boolean averageOut, stressMajorization, cliqueMDS, weightedSprings, stressMajDropNeighbor, stressMajDropLink;
    public static long rngSeed;

    public static boolean visualsON;

    static String trackingNumberHumanOutput;

    static DistanceMeasure distanceMeasure;
    static boolean symmetricDefault = true;

    public static void main(String[] Args) throws Exception {
        parameterizedRunner(Args);
    }

    private static void writeResultsHeader(HashMap<String, String> parameters, String destination) throws IOException {
        FileWriter fw = new FileWriter(destination, true);
        BufferedWriter bw = new BufferedWriter(fw);

       //write headers for the parameters of the run (#note: only writes keyset of params)
       for(String param : parameters.keySet()){
           bw.write(String.format("%s,",param));
       }

        //write headers for the statistics of the run too
        bw.write("part,nDCF3,nDCF6,nDCF10,uErr,STD,uAbsErr,AbsSTD,Q0,Q1,Q2,Q3,Q4,stress\n");

        bw.close();
    }

    private static void writeResults(HashMap<String, String> parameters, ArrayList<Record> Records, String destination) throws IOException {


        FileWriter fw = new FileWriter(destination, true);
        BufferedWriter bw = new BufferedWriter(fw);

        //write result values
        for (PartOfRound part : PartOfRound.values()){
            ArrayList<Record> recordsOfPart = Metrics.filterBySubround(Records, part);
            if (recordsOfPart.size()>0) {
                String paramValues = "";
                //write param values
                for(String param : parameters.keySet()){
                    //Iterate over the keyset because idk if the ordering is the same otherwise.
                    String paramValue = parameters.get(param);
                    paramValues += String.format("%s,",paramValue);
                }

                //gather statistics
                double nDCF3 = Metrics.getNDCFOfCollection(recordsOfPart, 3);
                double nDCF6 = Metrics.getNDCFOfCollection(recordsOfPart, 6);
                double nDCF10 = Metrics.getNDCFOfCollection(recordsOfPart, 10);
                double uErr = Metrics.getBiasOfCollectionError(recordsOfPart);
                double std = Metrics.getSTDofCollectionError(recordsOfPart);
                double uAbsErr = Metrics.getAbsoluteBiasOfCollectionError(recordsOfPart);
                double Absstd = Metrics.getSTDofCollectionAbsError(recordsOfPart);
                double Q0Err = Metrics.getQError(recordsOfPart,0.0/4.0);
                double Q1Err = Metrics.getQError(recordsOfPart,1.0/4.0);
                double Q2Err = Metrics.getQError(recordsOfPart,2.0/4.0);
                double Q3Err = Metrics.getQError(recordsOfPart,3.0/4.0);
                double Q4Err = Metrics.getQError(recordsOfPart,4.0/4.0);
                double stress = Metrics.getAverageStressOfCollection(recordsOfPart);
                //print statistics
                bw.write(paramValues +
                            String.format("%s,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f\n",
                            part.toString(), nDCF3, nDCF6, nDCF10, uErr, std, uAbsErr, Absstd, Q0Err, Q1Err, Q2Err, Q3Err, Q4Err, stress)
                        );
            }
        }

        bw.close();
    }

    public static void parameterizedRunner(String[] Args) throws Exception {
        HashMap<String, String> parameters = parseParams(Args);
        setParams(parameters);
        Simulation sim = new Simulation(Main.numAgents, Main.bounds, Main.numberOfRounds, Main.settingFile);
        sim.runSim();
        if(writeOutputHeader){
            writeResultsHeader(parameters, outputDestination);
        }
        writeResults(parameters, sim.getRecords(),outputDestination);
        cleanOutput(fullOutputDestination);
        if (!Main.fullOutputDestination.equalsIgnoreCase("")){
            sim.Metrics.appendCollectionToCSV(sim.getRecords(),new File(Main.fullOutputDestination));
        }
        if(visualsON){
            sim.keepRedrawing();
        }
    }



    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void cleanOutput(String target){
        File output = new File("output.csv");
        if(output.exists()){
            if (target==""){
                output.delete();
            } else {
                output.renameTo(new File(target));
            }
        }
    }

    public static HashMap<String, String> parseParams(String[] Args) throws Exception {
        HashMap<String, String> argsMap = new HashMap<>();
        for (String arg : Args) {
            String[] parts = arg.split("=");
            argsMap.put(parts[0], parts[1]);
        }
        //found on stackOverflow @ https://stackoverflow.com/a/51421535

        HashMap<String, String> defaults = getDefaultArgs();
        //check to see all args are parsed
        for (String arg : argsMap.keySet()) {
            if (!defaults.containsKey(arg)){
                throw new Exception("parameter not recognized: "+arg);
            }
        }

        //if parameter is not supplied, fill it in with default arg.
        for (String arg : defaults.keySet()) {
            if (!argsMap.containsKey(arg)) {
                argsMap.put(arg, defaults.get(arg));
            }
        }

        return argsMap;
    }


    public static void setParams(String settingFile, double bounds,
                                 int numAgents, int numberOfRounds,
                                 double alpha,
                                 double weightBias, double weightDiscrepancy,
                                 double weightSeparation, int numMalicious,
                                 Malicious_Agent.strategy strategy,
                                 int sybilMultiplier, boolean fractionInsteadOfExponent,
                                 double thresholdStressToCutoffMin, double thresholdStressToCutoffMax,
                                 boolean relativeConfusion, String dataLocation,
                                 DistanceMeasure distanceMeasure,
                                 String PrecompiledDistances)
    {
        Main.settingFile = settingFile;
        Main.bounds = bounds;
        Main.numberOfRounds = numberOfRounds;
        Main.numAgents = numAgents;
        Main.alpha = alpha;
        Main.weightBias = weightBias;
        Main.weightDiscrepancy = weightDiscrepancy;
        Main.weightSeparation = weightSeparation;
        Main.numberMaliciuos = numMalicious;
        Main.maliciousStrategy = strategy;
        Main.maliciousIDCount = sybilMultiplier;
        Main.fractionInsteadOfExponent = fractionInsteadOfExponent;
        Main.thresholdStressToCutoffMin = thresholdStressToCutoffMin;
        Main.thresholdStressToCutoffMax = thresholdStressToCutoffMax;
        Main.relativeConfusion = relativeConfusion;
        Main.distanceMeasure = distanceMeasure;
        Agent.setInitialGuessMethod(distanceMeasure);
        AgentHolder.distanceMeasure = distanceMeasure;
        Main.precompiledDistances = PrecompiledDistances;

        graphDrawer.setParams(Main.alpha, Main.weightSeparation, Main.weightDiscrepancy,
                Main.weightBias, Main.relativeConfusion, Main.fractionInsteadOfExponent,
                Main.thresholdStressToCutoffMin, Main.thresholdStressToCutoffMax);

        SceneBuilder.setParams(numMalicious, maliciousIDCount, strategy,
                                averageOut, stressMajorization, cliqueMDS,
                                weightedSprings, stressMajDropNeighbor, stressMajDropLink );

        RSSI_collector.MLLocations = dataLocation;
        RSSI_collector.fileLocation = dataLocation;
        RSSI_collector.seed = Main.rngSeed;
        RSSI_collector.precompiledDistancesLocation = PrecompiledDistances;
        RSSI_collector.defaultSymmetry = Main.symmetricDefault;

        AgentHolder.seed    = Main.rngSeed;
        AgentHolder.precompiledDistances = PrecompiledDistances;

        SceneBuilder.symmetricDefault = Main.symmetricDefault;
    }

    public static void setParams(HashMap<String, String> parameters){

        Main.settingFile = parameters.get("setting");
        Main.bounds =  Double.parseDouble(parameters.get("bounds"));
        Main.numberOfRounds = Integer.parseInt(parameters.get("numberOfRounds"));
        Main.numAgents = Integer.parseInt(parameters.get("population"));
        Main.alpha = Double.parseDouble(parameters.get("alpha"));
        Main.weightBias = Double.parseDouble(parameters.get("weightBias"));
        Main.weightDiscrepancy = Double.parseDouble(parameters.get("weightDiscrepancy"));
        Main.weightSeparation = Double.parseDouble(parameters.get("weightSeparation"));
        Main.numberMaliciuos = Integer.parseInt(parameters.get("numMalicious"));
        Main.maliciousStrategy = Malicious_Agent.parseStrategy(parameters.get("maliciousStrategy"));
        Main.maliciousIDCount = Integer.parseInt(parameters.get("sybilMultiplier"));
        Main.fractionInsteadOfExponent = Objects.equals(parameters.get("weights"), "fractional");
        Main.thresholdStressToCutoffMin = Double.parseDouble(parameters.get("threshStressMin"));
        Main.thresholdStressToCutoffMax = Double.parseDouble(parameters.get("threshStressMax"));
        Main.relativeConfusion = Objects.equals(parameters.get("delta"), "relative");
        Main.averageOut = Boolean.parseBoolean(parameters.get("averageOut"));
        Main.stressMajorization = Boolean.parseBoolean(parameters.get("stressMajorization"));
        Main.cliqueMDS = Boolean.parseBoolean(parameters.get("cliqueMDS"));
        Main.weightedSprings = Boolean.parseBoolean(parameters.get("weightedSprings"));
        Main.stressMajDropNeighbor = Boolean.parseBoolean(parameters.get("stressMajDropNeighbor"));
        Main.stressMajDropLink = Boolean.parseBoolean(parameters.get("stressMajDropLink"));
        Main.writeOutputHeader = Boolean.parseBoolean(parameters.get("writeOutputHeader"));
        Main.outputDestination = parameters.get("output");
        Main.fullOutputDestination = parameters.get("fullOutput");
        Main.dataLocation = parameters.get("dataLocation");
        Main.visualsON = Boolean.parseBoolean(parameters.get("visualsON"));
        Main.rngSeed = Long.parseLong(parameters.get("rngSeed"));
        Main.distanceMeasure = DistanceMeasure.getDistanceMeasure(parameters.get("distanceMeasure"));
        Main.precompiledDistances = parameters.get("PrecompiledDistances");
        Main.symmetricDefault = Boolean.parseBoolean(parameters.get("symmetric"));


        setParams(Main.settingFile, Main.bounds,
                Main.numAgents, Main.numberOfRounds,
                Main.alpha,
                Main.weightBias, Main.weightDiscrepancy,
                Main.weightSeparation, Main.numberMaliciuos,
                Main.maliciousStrategy, Main.maliciousIDCount,
                Main.fractionInsteadOfExponent,
                Main.thresholdStressToCutoffMin,
                Main.thresholdStressToCutoffMax,
                Main.relativeConfusion, Main.dataLocation,
                Main.distanceMeasure,
                Main.precompiledDistances);

        Simulation.visualsON = Main.visualsON;
    }

    public static HashMap<String, String> getDefaultArgs(){
        HashMap<String, String> defaults = new HashMap<>();
        defaults.put("setting","");
        defaults.put("bounds","500");
        defaults.put("numberOfRounds","20");
        defaults.put("population","1000");
        defaults.put("alpha","2.66");
        defaults.put("weightBias","8.95");
        defaults.put("weightDiscrepancy","0.32");
        defaults.put("weightSeparation","0.26");
        defaults.put("weights","absolute");
        defaults.put("threshStressMin","-1.0");
        defaults.put("threshStressMax","-1.0");
        defaults.put("delta","absolute");
        defaults.put("numMalicious","0");
        defaults.put("maliciousStrategy","close");
        defaults.put("sybilMultiplier","1");
        defaults.put("averageOut","false");
        defaults.put("stressMajorization","false");
        defaults.put("cliqueMDS","false");
        defaults.put("weightedSprings","true");
        defaults.put("stressMajDropNeighbor","false");
        defaults.put("stressMajDropLink","false");
        defaults.put("writeOutputHeader","false");
        defaults.put("output","outputResults.csv");
        defaults.put("fullOutput","");
        defaults.put("dataLocation","CDFs/");
        defaults.put("visualsON","false");
        defaults.put("rngSeed","1337");
        defaults.put("distanceMeasure","ML");
        defaults.put("PrecompiledDistances",null);
        defaults.put("symmetric", "true");
        return defaults;
    }
}
