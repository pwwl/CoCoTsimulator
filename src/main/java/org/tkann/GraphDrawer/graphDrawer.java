package GraphDrawer;

import org.apache.commons.math3.util.Pair;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.*;

import java.util.*;

public class graphDrawer {

	//global params - defaults set here. 
	static double alpha = 1;
	static double wDistance = 0;
	static double wDisagreement = 3;
	static double wBaseline = 1;
	static double thresholdStressToCutoffMin = -1;
	static double thresholdStressToCutoffMax = -1;
	static boolean relativeDisagreement = true;
	static boolean fractionInsteadOfExponent = false;

	public static void setParams(double alpha, double wDistance, double wDisagreement,
								  double wBaseline, boolean relativeDelta,
								 boolean fractionInsteadOfExponent,
								 double thresholdStressMin, double thresholdStressMax){
		graphDrawer.alpha = alpha;
		graphDrawer.wDistance = wDistance;
		graphDrawer.wDisagreement = wDisagreement;
		graphDrawer.wBaseline = wBaseline;
		graphDrawer.thresholdStressToCutoffMin = thresholdStressMin;
		graphDrawer.thresholdStressToCutoffMax = thresholdStressMax;
		graphDrawer.relativeDisagreement = relativeDelta;
		graphDrawer.fractionInsteadOfExponent = fractionInsteadOfExponent;
	}

	public static HashMap<Integer, HashMap<Integer, Double>> safeCopyEntryList(HashMap<Integer, HashMap<Integer, Double>> entryList) {
		HashMap<Integer, HashMap<Integer, Double>> CopyOfEntry = (HashMap<Integer, HashMap<Integer, Double>>) entryList.clone();
		for (Integer key : entryList.keySet()){
			HashMap<Integer, Double> copiedEntry = (HashMap<Integer, Double>) entryList.get(key).clone();
			CopyOfEntry.put(key, copiedEntry);
		}

		return CopyOfEntry;
	}
	
	public static RealMatrix pairwiseDistances(RealMatrix M) {
		return null;
	}

	private static HashMap<Integer, HashMap<Integer, Double>> sanitizeEntryList(HashMap<Integer, HashMap<Integer, Double>> entryList) {
		ArrayList<Integer> neighborhood = new ArrayList<>(entryList.keySet());
		for (HashMap<Integer, Double> entry : entryList.values()){
			ArrayList<Integer> toRemove = new ArrayList<>();
			for (Integer neighborID : entry.keySet()){
				if (!neighborhood.contains(neighborID)){
					toRemove.add(neighborID);
				}
			}
			for (Integer neighborID : toRemove){
				entry.remove(neighborID);
			}
		}
		return entryList;
	}

	public static double[][] listsToMatrix(int CallerID, HashMap<Integer, HashMap<Integer, Double>> EntryList){
		TreeSet<Integer> allIDs = new TreeSet<Integer>(EntryList.keySet());
		for (int UID : EntryList.keySet()){
			allIDs.addAll(EntryList.get(UID).keySet());
			if (!allIDs.contains(UID))
				allIDs.add(UID);
		}
		int totalNodes = allIDs.size();

		double[][] distanceMatrix = new double[totalNodes][totalNodes];

		TreeSet<Integer> IDsOfNeighbors = new TreeSet<>();
		IDsOfNeighbors.addAll(EntryList.get(CallerID).keySet());
		IDsOfNeighbors.add(CallerID);

		//fill array with known distances and -1 if the distance is unknown
		//for (int UID : EntryList.get(CallerID).keySet()){
		for (int UID : IDsOfNeighbors){
			HashMap<Integer, Double> NeighborDistances = EntryList.get(UID);

			int UIDEntry = allIDs.headSet(UID).size();
			for (int neighborUID : allIDs){
				int neighborUIDentry = allIDs.headSet(neighborUID).size();

				if (UID == neighborUID){
					distanceMatrix[UIDEntry][neighborUIDentry] = 0.0; // put 0 distance to self, this is always known
				} else if (NeighborDistances.containsKey(neighborUID)) {
					distanceMatrix[UIDEntry][neighborUIDentry] = NeighborDistances.get(neighborUID);
				} else {
					distanceMatrix[UIDEntry][neighborUIDentry] = -1;
				}

			}
		}

		return distanceMatrix;
	}

	public static HashMap<Integer, Integer> listsToUIDtoIndex(HashMap<Integer, HashMap<Integer, Double>> EntryList){
		TreeSet<Integer> allIDs = new TreeSet<Integer>(EntryList.keySet());
		for (int UID : EntryList.keySet()){
			allIDs.addAll(EntryList.get(UID).keySet());
		}
		HashMap<Integer,Integer> UIDtoIndex = new HashMap<Integer,Integer>();
		for (int neighborUID : allIDs){
			UIDtoIndex.put(neighborUID,allIDs.headSet(neighborUID).size());
		}
		return UIDtoIndex;
	}

	public static double[][] estimateMissingDistances(double[][] distanceGuess){
		double[][] updatedDistanceGuess = new double[distanceGuess.length][];

		for (int i = 0; i< updatedDistanceGuess.length;i++){
			updatedDistanceGuess[i] = distanceGuess[i].clone();
		}

		for (int i = 0; i < distanceGuess.length; i++){
			for (int j = 0; j < distanceGuess[0].length; j++){
				if (distanceGuess[i][j]==-1){
					updatedDistanceGuess[i][j] = estimateMissingDistanceHelper(distanceGuess, i, j);
				}
			}
		}
		return updatedDistanceGuess;
	}

	public static double estimateMissingDistanceHelper(double[][] distanceGuess, int i, int j){
		//find the upper bound for the missing length based on the triangle inequality and a lower bound based on the min distance,
		//then average the two guesses
		double Bij = distanceGuess[i][0]+distanceGuess[0][j];
		double bijOther = Bij;
		for (int h = 0; h<distanceGuess.length; h++){
			//triangle inequality part.
			if( (distanceGuess[i][h] != -1) && (distanceGuess[h][j] != -1)) {
				double BijCandidate = distanceGuess[i][h] + distanceGuess[h][j];
				if (BijCandidate < Bij || Bij == -1) {
					Bij = BijCandidate;
				}
			}

			//lower bound based on min distances
			if (distanceGuess[i][h] > 0 &&
					(distanceGuess[i][h]< bijOther || bijOther <= 0)){
				bijOther = distanceGuess[i][h];
			}
			if (distanceGuess[h][j] > 0 &&
					(distanceGuess[h][j] < bijOther || bijOther <= 0)){
				bijOther = distanceGuess[h][j];
			}
		}

		//return the average of the lower bound and upper bound guess for a start
		double BijBias = 0; //a number in [0,.5] that says how biased the simulation is towards on part of the estimate
		return (Bij * (.5 + BijBias)) + (bijOther * (.5 - BijBias));
		//return (Bij + bijOther)/2.0;
	}

	private static double[][] estimateMissingWeights(double[][] distanceGuess){
		double[][] missingWeights = new double[distanceGuess.length][distanceGuess[0].length];

		for (int i = 0; i < distanceGuess.length; i++){
 			for (int j = 0; j < distanceGuess[0].length; j++){
				if (distanceGuess[i][j]==-1){
					estimateMissingWeightsHelper(distanceGuess, missingWeights, i, j);
				}
			}
		}

		return missingWeights;
	}
	private static void estimateMissingWeightsHelper(double[][] distanceGuess, double[][] pathLookup, int i, int j){
		double Bij = distanceGuess[i][0]+distanceGuess[0][j];
		double bijOther = Bij;
		double BijWeightContribution = 0;
		double bijOtherWeightContribution = 0;
		for (int h = 0; h<distanceGuess.length; h++){
			/*
			 * triangle inequality part.
		     * find the shortest length 2 path between i and j
			 */
			if( (distanceGuess[i][h] != -1) && (distanceGuess[h][j] != -1)) {
				/*
				h will never equal i or j and have neither be not -1 since this
				function wouldn't be called in that case, they'd be neighbors
				*/
				double BijCandidate = distanceGuess[i][h] + distanceGuess[h][j];
				if (BijCandidate < Bij || Bij == -1) {
					Bij = BijCandidate;
					BijWeightContribution = getDisagreement(distanceGuess,i,h) + getDisagreement(distanceGuess,j,h);
											//Math.abs(distanceGuess[i][h] - distanceGuess[h][i]) + Math.abs(distanceGuess[j][h] - distanceGuess[j][i]);
				}
			}

			/*
			 * lower bound based on min distances
			 * assume i and j have a max distance they can see that is	roughly equal to the
			 * furthest node they can see
			 */
			if (distanceGuess[i][h] > 0 &&
					(distanceGuess[i][h]< bijOther || bijOther <= 0)){
				bijOther = distanceGuess[i][h];
				bijOtherWeightContribution = getDisagreement(distanceGuess,i,h);
											//Math.abs(distanceGuess[i][h] - distanceGuess[h][j]);
			}
			if (distanceGuess[h][j] > 0 &&
					(distanceGuess[h][j] < bijOther || bijOther <= 0)){
				bijOther = distanceGuess[h][j];
				bijOtherWeightContribution = getDisagreement(distanceGuess,i,h);
											//Math.abs(distanceGuess[i][h] - distanceGuess[h][j]);
			}
		}

		//write the sum of the weight contributions in the pathLookup area
		pathLookup[i][j] = BijWeightContribution+bijOtherWeightContribution;
	}

	public static RealMatrix turnHermitian(RealMatrix M){
		//if the matrix isn't hermetian, we can average out the diagonal parts to make it hermetian
		//this shouldn't violate any assumptions here since across the diagonals should be identical with perfect information.
		//This is essentially doing an average out method first before MDS
		for(int i = 0; i<M.getRowDimension();i++){
			for(int j=i; j<M.getColumnDimension();j++){
				double upperPart = M.getEntry(i,j);
				double lowerPart = M.getEntry(j,i);

				double average = (upperPart+lowerPart)/2;

				M.setEntry(i,j,average);
				M.setEntry(j,i,average);
			}
		}
		return M;
	}

	public static RealMatrix MDS(RealMatrix M){
		int n = M.getColumnDimension();


		//create the P^2 squared proximity matrix from M
		RealMatrix P = M.copy();

		for (int i = 0; i < n; i++){
			//vector has entry by entry multiply but not matricies for some reason
			//So we go vector by vector replacing it with the entry wise square of itself
			P.setColumnVector(i, P.getColumnVector(i).ebeMultiply(P.getColumnVector(i)));
		}

		//Create the Jn Matrix to produce the double centering
		double[] toBeOnes = new double[n];
		for (int i = 0; i < n; i++){
			toBeOnes[i]=1;
		}
		RealVector oneN = MatrixUtils.createRealVector(toBeOnes);
		RealMatrix Jn = MatrixUtils.createRealIdentityMatrix(n).subtract(oneN.outerProduct(oneN).scalarMultiply((double) 1/n));

		//create the double centring matrix B
		RealMatrix B = Jn.scalarMultiply(-1.0/2.0).multiply(P).multiply(Jn);

		//find the 2 largest eigenvalues of B now
		if (B.getColumnDimension()==0){
			System.out.println("B is 0!");
		}
		//System.out.println(B);
		EigenDecomposition BEigen = new EigenDecomposition(B);
		//throw all the values and indicies into a hashmap to sort
		double[] eigenValues = BEigen.getRealEigenvalues(); //there should be no imaginary ones


		HashMap<Double,Integer> eigenvaluesToIndex = new HashMap<Double,Integer>();
		for (int i = 0; i < eigenValues.length; i++){
			eigenvaluesToIndex.put(eigenValues[i],i);
		}

		TreeSet<Double> sortedEigenValues = new TreeSet<Double>(eigenvaluesToIndex.keySet());

		Iterator<Double> iterator = sortedEigenValues.descendingIterator();
		int LargestEigenIndex = eigenvaluesToIndex.get(iterator.next());
		int secondLargestEigenIndex = eigenvaluesToIndex.get(iterator.next());

		RealMatrix Q = MatrixUtils.createRealMatrix(n,2);
		RealVector Q1 = BEigen.getEigenvector(LargestEigenIndex).mapDivide(Math.sqrt(eigenValues[LargestEigenIndex]));
		RealVector Q2 = BEigen.getEigenvector(secondLargestEigenIndex).mapDivide(Math.sqrt(eigenValues[secondLargestEigenIndex]));
		Q.setColumnVector(0,Q1);
		Q.setColumnVector(1,Q2);
		return Q;
	}

	public static RealMatrix stressMajorization(RealMatrix QfromMDS,  HashMap<Integer, HashMap<Integer, Double>> EntryList){
		HashMap<Integer,Integer> UIDtoIndex = listsToUIDtoIndex(EntryList);

		int repeatTimes = 100;
		for (int iterationCount = 0; iterationCount<repeatTimes; iterationCount++){
			for (int UIDforNodeI : EntryList.keySet()){
				HashMap<Integer,Double> distanceGuessesForNodeI = EntryList.get(UIDforNodeI);
				int indexForI = UIDtoIndex.get(UIDforNodeI);
				RealVector Qi = QfromMDS.getRowVector(indexForI);
				RealVector totalSum = MatrixUtils.createRealVector(new double[Qi.getDimension()]);
				for (int UIDForNodeJ : distanceGuessesForNodeI.keySet()){
					int indexForJ = UIDtoIndex.get(UIDForNodeJ);
					RealVector Qj = QfromMDS.getRowVector(indexForJ);
					RealVector difference = Qi.subtract(Qj);
					RealVector normalizedDifference = difference.mapMultiply(modifiedInverse(difference.getNorm()));
					totalSum = totalSum.add( Qj.add(normalizedDifference.mapMultiply(distanceGuessesForNodeI.get(UIDForNodeJ))));
				}
				totalSum = totalSum.mapDivide(distanceGuessesForNodeI.size());//assuming I is not in distance guess for I
				QfromMDS.setRowVector(indexForI,totalSum);
			}
//			if (iterationCount % 10 == 0){
//				String thispoints = "points"+iterationCount;
//				System.out.println(thispoints+" = "+QfromMDS);
//				System.out.println(thispoints+"(:,1) = "+thispoints+"(:,1)-mean("+thispoints+"(:,1))");
//				System.out.println(thispoints+"(:,2) = "+thispoints+"(:,2)-mean("+thispoints+"(:,2))");
//				System.out.println("figure\nhold on");
//				System.out.println("scatter(points(:,1),points(:,2),'b');");
//				System.out.println("scatter("+thispoints+"(:,1),"+thispoints+"(:,2),'b');");
//				System.out.println("\n");
//			}
		}

		return QfromMDS;
	}

	public static double modifiedInverse(double input){
		return input == 0 ? 0 : 1/input;
	}

	public static HashMap<Integer, Double> DistancesFromGraph(int callerID, RealMatrix graphMatrix, HashMap<Integer, HashMap<Integer, Double>> EntryList){
		//set up our map to return
		HashMap<Integer,Double> betterDistancesFromGraphDrawing = new HashMap<Integer,Double>();
		//get some data about our caller
		HashMap<Integer,Double> CallersNeighbors = EntryList.get(callerID);
		HashMap<Integer,Integer> UIDtoIndex = listsToUIDtoIndex(EntryList);
		int callerIndex = UIDtoIndex.get(callerID);

		RealVector callerPosition = graphMatrix.getRowVector(callerIndex);
		for (int neighborUID : CallersNeighbors.keySet()){
			int neighborIndex = UIDtoIndex.get(neighborUID);
			RealVector neighborsPosition = graphMatrix.getRowVector(neighborIndex);
			double distanceToNeighbor = callerPosition.getDistance(neighborsPosition);
			betterDistancesFromGraphDrawing.put(neighborUID,distanceToNeighbor);
		}

		return betterDistancesFromGraphDrawing;
	}

	public static HashMap<Integer,Double> getBetterDistancesFromGraphDrawing_ORMDS(int callerID, HashMap<Integer, HashMap<Integer, Double>> EntryList){
		//run all the helper functions
		EntryList = safeCopyEntryList(EntryList);
		EntryList = sanitizeEntryList(EntryList);
		double[][] matrixFromLists = listsToMatrix(callerID,EntryList);
		double[][] distanceMatrix = estimateMissingDistances(matrixFromLists);
		//the matrix SHOULD be hermetian, but just in case we will force it to be
		RealMatrix HermetianDistance = turnHermitian(MatrixUtils.createRealMatrix(distanceMatrix));
		RealMatrix MDSEstimate = MDS(HermetianDistance);
		RealMatrix stressMajorizedMDS = stressMajorization(MDSEstimate,EntryList);

		//set up our map to return
		HashMap<Integer,Double> betterDistancesFromGraphDrawing = DistancesFromGraph(callerID,stressMajorizedMDS,EntryList);

		return betterDistancesFromGraphDrawing;
	}


	public static RealMatrix getWeightMatrix(RealMatrix distances){
		RealMatrix weights = distances.copy();
		for (int i = 0; i < weights.getColumnDimension(); i++){
			for (int j = 0; j < weights.getRowDimension(); j++){
				double weight = 1;
				if (j!=i){
					double directDistance = (distances.getEntry(i,j)+distances.getEntry(j,i))/2;//average out the two guesses
					double disagreement = Math.abs(distances.getEntry(i,j)-distances.getEntry(j,i));
					double preWeight = wBaseline + wDistance*directDistance + wDisagreement*disagreement;
					weight = Math.pow(preWeight,-1*alpha);
				}
				//along the diagonals just set 0;
				weights.setEntry(i,j,weight);
			}
		}
		return weights;
	}

	public static RealMatrix getWeightMatrix2(RealMatrix distances, double[][] missingWeights){
		RealMatrix weights = distances.copy();
		for (int i = 0; i < weights.getColumnDimension(); i++){
			for (int j = 0; j < weights.getRowDimension(); j++){
				double weight = 1;
				double disagreement;
				if (j!=i){
					if (missingWeights[i][j]!=0){
						disagreement = missingWeights[i][j];
					} else {
						//get disagreement is a state based function that can return either
						//relative or absolute weights
						disagreement = getDisagreement(distances,i,j);
					}
					double directDistance = (distances.getEntry(i, j) + distances.getEntry(j, i)) / 2;//average out the two guesses
					double preWeight = wBaseline + wDistance * directDistance + wDisagreement * disagreement;

					if (fractionInsteadOfExponent) {
						weight = Math.pow(preWeight, -1 * alpha);
					} else {
						weight = Math.exp(-1 * preWeight);
					}
				}
				//along the diagonals just set 0;
				weights.setEntry(i,j,weight);
			}
		}


		return weights;
	}
	
	private static double getDisagreement(RealMatrix distances, int i, int j){
		if(relativeDisagreement){
			return Math.abs(distances.getEntry(i, j) - distances.getEntry(j, i))
					/Math.abs(distances.getEntry(i, j) + distances.getEntry(j, i))
					* 15; //scale it up so instead of [0,1] it lies within roughly the same range as others, at [0,15]
		} else {
			return Math.abs(distances.getEntry(i, j) - distances.getEntry(j, i));
		}
	}
	private static double getDisagreement(double[][] distances, int i, int j){
		return getDisagreement(MatrixUtils.createRealMatrix(distances), i, j);
	}

	public static double calculateStress(RealMatrix weights, RealMatrix estimate, RealMatrix trueDistances){
		int rows = weights.getRowDimension();
		int columns = weights.getColumnDimension();
		double stressSum = 0;
		for(int j = 0; j<rows-1;j++) {
			for (int i = j+1; i < rows; i++) {
				RealVector iLoc = estimate.getRowVector(i);
				RealVector jLoc = estimate.getRowVector(j);
				double GraphedDistance = iLoc.subtract(jLoc).getNorm();
				double Error = GraphedDistance - trueDistances.getEntry(i,j);
				double stressEntry = weights.getEntry(i,j)*(Math.pow(Error, 2));
				stressSum+=stressEntry;
			}
		}

		return stressSum;
	}

	public static void fillDiagonals(RealMatrix input, int style, boolean negate){
		/***
		 * style 1 = diagonals = sum of all else
		 * style 2 = diagonals = sum of row
		 * style 3 = diagonals = sum of cols
		 */
		int rows = input.getRowDimension();
		int negated = negate ? -1 : 0;
		double sum = 0;
		switch (style){
			case 1:
				sum = 0;
				for(int i = 0; i<rows;i++){
					//do the diagonals now
					for(int j=0; j<rows; j++){
						if(j!=i) {
							sum += input.getEntry(i, j);
						}
					}
				}

				for (int i=0; i<rows; i++){
					input.setEntry(i,i,negated*sum);
				}
				break;
			case 2:
				for(int i=0; i<rows;i++){
					sum = 0;
					for(int k=0; k<rows; k++){
						if(k!=i) {
							sum += input.getEntry(i, k);
						}
					}
					input.setEntry(i,i,negated * sum);
				}
				break;
			case 3:
				for(int i=0; i<rows;i++){
					sum = 0;
					for(int k=0; k<rows; k++){
						if(k!=i) {
							sum += input.getEntry(k, i);
						}
					}
					input.setEntry(i,i,negated * sum);
				}
				break;
		}


	}
	public static RealMatrix makeLaplacianWeight(RealMatrix weights){
		int rows = weights.getRowDimension();
		int columns = weights.getColumnDimension();
		RealMatrix L = MatrixUtils.createRealMatrix(rows,columns);
		for(int i = 0; i<rows;i++){
			for(int j=0; j<columns;j++){
				double LEntry = 0;
				if (i!=j) {
					LEntry = -1 * weights.getEntry(i, j);
					L.setEntry(i, j, LEntry);
				}
				L.setEntry(i,j,LEntry);
			}
		}

		fillDiagonals(L,1,true);

		return L;
	}
	public static RealMatrix makeLaplacianPositions(RealMatrix positions, RealMatrix distances){
		int rows = positions.getRowDimension();
		RealMatrix L = MatrixUtils.createRealMatrix(rows,rows);
		for(int i = 0; i<rows;i++){
			for(int j=0; j<rows;j++){
				double LEntry;
				if (i!=j) {
					RealVector iLoc = positions.getRowVector(i);
					RealVector jLoc = positions.getRowVector(j);
					double delta = distances.getEntry(i,j);
					LEntry = -1 * delta * modifiedInverse(iLoc.subtract(jLoc).getNorm());
					L.setEntry(i, j, LEntry);
				} else {
					LEntry = 0;//need to finish the rest of the matrix first, the go back and do the diagonal
				}
				L.setEntry(i,j,LEntry);
			}
		}

		fillDiagonals(L,2,true);

		return L;
	}

	public static RealMatrix weightedStressMajorization(RealMatrix initialGuess, RealMatrix trueDistances, RealMatrix weights){
		int MAXITERS = 100;
		int iterCount = 0;
		double TOLERANCE = Math.pow(10,-4);
		double epsChange = 100;
		double prevStress = calculateStress(weights, initialGuess, trueDistances);

		RealMatrix guess = initialGuess;
		RealMatrix updatedGuess= guess.copy();
		//the row i of guess is the guess of the location of vertex i in the graph

		RealMatrix laplacianWeights = makeLaplacianWeight(weights);
		//drop first row and column
		int cols = laplacianWeights.getColumnDimension();
		laplacianWeights = laplacianWeights.getSubMatrix(1,cols-1,1,cols-1);
		DecompositionSolver solver = new LUDecomposition(laplacianWeights).getSolver();
		//luckily Lw is a constant so we only need to create it once

		while (iterCount < MAXITERS && epsChange>TOLERANCE){
			iterCount++;
			//check to see that X0 is centered at 0, if it's not center the drawing at 0,0
			if (guess.getRowVector(0).getNorm()!=0){//norm = 0 is an easy way to tell if it's 0
				for (int i = 0; i < guess.getRowDimension(); i++){
					//subtract X0 from all entries
					guess.setRowVector(i, updatedGuess.getRowVector(i).subtract(updatedGuess.getRowVector(0)));
				}
			}

			updatedGuess= guess.copy();

			RealMatrix LaplacianGuess = makeLaplacianPositions(guess, trueDistances);


			RealMatrix target = LaplacianGuess.multiply(guess);
			cols = target.getColumnDimension();
			int rows = target.getRowDimension();
			target = target.getSubMatrix(1,rows-1,0,cols-1);
			updatedGuess = solver.solve(target);
			/*
			for (int axis = 0; axis < guess.getColumnDimension(); axis++){
			}
			 */
			//debug: set weights to be identity matrix, see if it converges to something else.
			updatedGuess = target.copy();
			//update the guess
			guess.setSubMatrix(updatedGuess.getData(),1,0);
			//calculate stressEpsilon
			double newStress = calculateStress(weights,guess,trueDistances);
			epsChange = Math.abs(prevStress-newStress)/prevStress;
			//save previous stress for computation
			prevStress = newStress;
		}

		return guess;
	}

	public static Pair<RealMatrix,Double> weightedStressMajorizationTry2(RealMatrix initialGuess, RealMatrix trueDistances, RealMatrix weights){
		int MAXITERS = 100;
		int iterCount = 0;
		double TOLERANCE = Math.pow(10,-5);
		double epsChange = 100;
		double prevStress = calculateStress(weights, initialGuess, trueDistances);

		RealMatrix guess = initialGuess;
		RealMatrix updatedGuess= guess.copy();
		//the row i of guess is the guess of the location of vertex i in the graph


		while (iterCount < MAXITERS && epsChange>TOLERANCE){
			iterCount++;
			for (int dimension=0; dimension<2; dimension++){

				for (int i=0; i<guess.getRowDimension(); i++){
					double numeratorSum = 0;
					double denominatorSum = 0;
					double Xia = guess.getEntry(i,dimension);
					for (int j=0; j<guess.getRowDimension(); j++){
						if (i!=j){
							double weight = weights.getEntry(i,j);
							denominatorSum += weight;

							double Xja = guess.getEntry(j,dimension);
							double dij = trueDistances.getEntry(i,j);
							RealVector delta = guess.getRowVector(i).subtract(guess.getRowVector(j));
							double invDelta = modifiedInverse(delta.getNorm());
							double component = Xja + dij*(Xia-Xja)*invDelta;

							numeratorSum += weight*component;
						}
					}
					updatedGuess.setEntry(i,dimension,numeratorSum/denominatorSum);
					//debug: deleteme
					//todo: debug
					guess.setEntry(i,dimension,numeratorSum/denominatorSum);
				}

			}

			guess = updatedGuess.copy();
			double newStress = calculateStress(weights,guess,trueDistances);
			epsChange = Math.abs(prevStress-newStress)/prevStress;
			//save previous stress for computation
			prevStress = newStress;
		}

		return new Pair<RealMatrix, Double>(guess,prevStress);
	}


	public static Pair<HashMap<Integer,Double>,Double> getBetterDistancesFromGraphDrawing_weightedORMDS(int callerID, HashMap<Integer, HashMap<Integer, Double>> EntryList){
		//run all the helper functions
		EntryList = safeCopyEntryList(EntryList);
		EntryList = sanitizeEntryList(EntryList);
		double[][] matrixFromLists = listsToMatrix(callerID,EntryList);
		double[][] distanceMatrix = estimateMissingDistances(matrixFromLists);
		double[][] missingWeights = estimateMissingWeights(matrixFromLists);
		RealMatrix MatrixOfDistances = MatrixUtils.createRealMatrix(distanceMatrix);
		RealMatrix weightMatrix = getWeightMatrix2(MatrixOfDistances, missingWeights);
		//the matrix SHOULD be hermitian, but just in case we will force it to be
		RealMatrix HermitianDistance = turnHermitian(MatrixOfDistances);
		RealMatrix MDSEstimate = MDS(HermitianDistance);
		RealMatrix stressMajorizedMDS = stressMajorization(MDSEstimate,EntryList);
		Pair<RealMatrix,Double> results = weightedStressMajorizationTry2(stressMajorizedMDS,HermitianDistance,weightMatrix);
		RealMatrix weightedMajorization = results.getKey();
		double totalStress = results.getValue();

		double averageStress = totalStress/EntryList.get(callerID).size();

		//set up our map to return
		HashMap<Integer,Double> betterDistancesFromGraphDrawing;
		if ((thresholdStressToCutoffMin < averageStress|| thresholdStressToCutoffMin < 0) && //stress is above min or min unset AND
				(averageStress < thresholdStressToCutoffMax || thresholdStressToCutoffMax < 0)){ //stress is below max or max unset
			//normal case
			betterDistancesFromGraphDrawing = DistancesFromGraph(callerID,weightedMajorization,EntryList);
		} else {
			//case where stress is exceedingly high
			betterDistancesFromGraphDrawing = EntryList.get(callerID);
		}

		return new Pair<>(betterDistancesFromGraphDrawing,averageStress);
	}

	public static RealMatrix WORMDSGrid(int callerID, HashMap<Integer, HashMap<Integer, Double>> EntryList){
		//run all the helper functions
		EntryList = safeCopyEntryList(EntryList);
		EntryList = sanitizeEntryList(EntryList);
		double[][] matrixFromLists = listsToMatrix(callerID,EntryList);
		double[][] distanceMatrix = estimateMissingDistances(matrixFromLists);
		double[][] missingWeights = estimateMissingWeights(matrixFromLists);
		RealMatrix MatrixOfDistances = MatrixUtils.createRealMatrix(distanceMatrix);
		RealMatrix weightMatrix = getWeightMatrix2(MatrixOfDistances, missingWeights);
		//the matrix SHOULD be hermitian, but just in case we will force it to be
		RealMatrix HermitianDistance = turnHermitian(MatrixOfDistances);
		RealMatrix MDSEstimate = MDS(HermitianDistance);
		RealMatrix stressMajorizedMDS = stressMajorization(MDSEstimate,EntryList);
		Pair<RealMatrix,Double> results = weightedStressMajorizationTry2(stressMajorizedMDS,HermitianDistance,weightMatrix);
		RealMatrix weightedMajorization = results.getKey();
		return weightedMajorization;
	}




}
