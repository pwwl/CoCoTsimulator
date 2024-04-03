package simulation;

public enum PartOfRound implements Comparable<PartOfRound>{
    initialGuess,
    OneRoundStressMajorization,
    cliqueMDS,
    averageOut,
    weightedORMDS,
    weightedORMDS_drop,
    weightedORMDS_dropLink;

    //justAveraging,
    //neighborHeuristic;

}
