package simulation;

public enum DistanceMeasure {
    RSSI,
    Rand,
    ML,
    MLRSSIHybrid,
    DATASET;

    public static DistanceMeasure getDistanceMeasure(String measure) {
        switch (measure) {
            case "RSSI":
                return RSSI;
            case "Rand":
                return Rand;
            case "ML":
                return ML;
            case "MLRSSIHybrid":
                return MLRSSIHybrid;
            case "DATASET":
                return DATASET;
            default:
                return RSSI;
        }
    }
}