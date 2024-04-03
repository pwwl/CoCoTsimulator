package GraphDrawer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

public class GraphTester {
    /*
	static Utility util;
	static MetricAnalyzer metrics;

	public static void main(String[] args) {
		//double[][] testMatrix = {{1.1897974801,-0.2377003968,-2.1856812493,-0.0219873468,4.7494049826,-2.0278105993,0.4094786532,-1.8755015237},{1.1897974801,-0.2377003968,-2.1856812493,-0.0219873468,4.7494049826,-2.0278105993,0.4094786532,-1.8755015237},{-5.636601353,7.5950749851,8.6285319376,-6.5173253799,-6.2943314505,5.8049647826,-9.5375873799,5.9572738582},{1.1897974801,-0.2377003968,-2.1856812493,-0.0219873468,4.7494049826,-2.0278105993,0.4094786532,-1.8755015237},{1.1897974801,-0.2377003968,-2.1856812493,-0.0219873468,4.7494049826,-2.0278105993,0.4094786532,-1.8755015237},{-3.2621752299,-7.9288643068,2.7255638557,4.8892577582,-9.3821565274,4.6019071107,5.3207237582,3.0357435813},{1.1897974801,-0.2377003968,-2.1856812493,-0.0219873468,4.7494049826,-2.0278105993,0.4094786532,-1.8755015237},{2.9497891826,1.5222913057,-0.4256895468,1.7380043557,-8.0705369349,-0.2678188968,2.1694703557,0.3844901788}};
		//EigenDecomposition BEigen = new EigenDecomposition(MatrixUtils.createRealMatrix(testMatrix));
		//System.out.println(MatrixUtils.createRealVector(BEigen.getRealEigenvalues()));

		try {
			util = new Utility();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		metrics = new MetricAnalyzer(util);

		int NumNodes = 7;
		double range = 11;
		node[] allNodes = {new node(0, Position.INHAND,0,0,range,util,metrics),
							new node(1, Position.INHAND,5,5,range,util,metrics),
							new node(2, Position.INHAND,5,-5,range,util,metrics),
							new node(3, Position.INHAND,-5,5,range,util,metrics),
							new node(4, Position.INHAND,-5,-5,range,util,metrics),
							new node(5, Position.INHAND,0,10,range,util,metrics),
							new node(6, Position.INHAND,0,-10,range,util,metrics)
							};

		HashMap<Integer,HashMap<Integer,Double>> EntryList = new HashMap<Integer,HashMap<Integer,Double>>();
		for (node thisNode : allNodes){
			for (node potentialNeighbor : allNodes){
				if (!(thisNode.equals(potentialNeighbor)) && (thisNode.distTo(potentialNeighbor)< thisNode.range)){
					thisNode.addNeighbor(potentialNeighbor);
				}
			}

			HashMap<Integer,Double> thisNodesGuesses = new HashMap<Integer,Double>();
			double errorPercent = .1;
			for (node neighbor : thisNode.neighbors){
				double distanceGuess = thisNode.distTo(neighbor)* main.randfloat(1-errorPercent,1+errorPercent);
				thisNodesGuesses.put(neighbor.UID, distanceGuess);
			}
			EntryList.put(thisNode.UID, thisNodesGuesses);
		}

//		double[][] matrixFromLists = GraphDrawer.graphDrawer.listsToMatrix(0,EntryList);
//		double[][] distanceMatrix = GraphDrawer.graphDrawer.estimateMissingDistances(matrixFromLists);
//		RealMatrix MDSEstimate = GraphDrawer.graphDrawer.MDS(MatrixUtils.createRealMatrix(distanceMatrix));
//		RealMatrix stressMajorizedMDS = GraphDrawer.graphDrawer.stressMajorization(MDSEstimate,EntryList);
//
//		System.out.println(stressMajorizedMDS);

		HashMap<Integer,Double> graphDistances = graphDrawer.getBetterDistancesFromGraphDrawing(0,EntryList);
		System.out.println("\n\n\n");
		for (int UID : graphDistances.keySet()){
			double realDist = allNodes[0].distTo(allNodes[UID]);
			double guessedDist = graphDistances.get(UID);
			System.out.println("dist to "+UID+" is estimated as "+guessedDist+", actual distance is "+realDist+", error is "+ ((realDist-guessedDist)/realDist));
		}


	}
*/
}
