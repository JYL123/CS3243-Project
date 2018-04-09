import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;


public class GeneticAlgorithm {
    private static ArrayList<Genome> population;
    private int populationSize = 3;
    private static int numberOfFeatures = 6; //highestCol and lowestCol are replaced by heightDifference
    /*features of a state, we randomly define a state in the begining*/
    private static int blockadeWeightIndex = 0;
    private static int edgeHeightWeightIndex = 1;
    private static int heightDifferenceWeightIndex = 2;
    private static int holesWeightIndex = 3;
    private static int islandWeightIndex = 4;
    private static int parityWeightIndex = 5;


    public ArrayList<Genome> initializePopulation () {
        population = new ArrayList<>();
        //initial random weights for each feature
        for (int i = 0; i < populationSize; i++) {
            int id = i;
            Genome genome = new Genome(id);
            population.add(genome);
        }
        return population;
    }

    /*
     * evaluate sets of weights
     * first we need to evalute weights for every individual of the population
     */
    private static double evaluateIndividual (Genome individual, State s) {
        //put weights into the array for evaluation 
        double[] weights = new double[6];
        weights[blockadeWeightIndex] = individual.getBlockadeWeight();
        weights[edgeHeightWeightIndex] = individual.getEdgeHeightWeight();
        weights[heightDifferenceWeightIndex] = individual.getHeightDifferenceWeight();
        weights[holesWeightIndex] = individual.getHolesWeight();
        weights[islandWeightIndex] = individual.getIslandWeight();
        weights[parityWeightIndex] = individual.getParityWeight();

        double score = 0;
        new TFrame(s);
        PlayerSkeleton p = new PlayerSkeleton();
        while(!s.hasLost()) {
            s.makeMove(p.pickMove(s,s.legalMoves(), weights));
            s.draw();
            s.drawNext(0,0);
            //comment the thread to speed up the program
//            try {
//                Thread.sleep(300);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }
        score = s.getRowsCleared();

        return score;
    }

    /*
     * give score to each set of weights
     * return the arraylist of pair(id, score): id: id for that set of weights; score: evaluation score
     */
    public static HashMap<Integer, Double> evaluatePopulation (ArrayList<Genome> population, State s) {
        HashMap<Integer, Double> evaluationScore = new HashMap<Integer, Double>();

        //evaluate each individual in the population.
        //fixed index from a set of weights to its evaluation score
        for (int i = 0; i < population.size(); i++) {
            /* Map<id, score> */
            State newState = new State();
            evaluationScore.put(i, evaluateIndividual(population.get(i), newState));
        }

        return evaluationScore;
    }

    /*
     * update population, each time choose the first and second sets of weight 
     * their child will replace the worst in the list
     * return an updated population
     */
    public static ArrayList<Genome> evolve (ArrayList<Genome> population, HashMap<Integer, Double> evaluationScore) {
        /* selection of parents */
        int[] parents = selectParent(evaluationScore);
        int mom = parents[0];
        int dad = parents[1];

        Genome momGenome = population.get(mom);
        Genome dadGenome = population.get(dad);

        /* cross over */
        Random random = new Random();
        int id = selectReplacement(evaluationScore);
        double holesWeight = random.nextBoolean() ? momGenome.getHolesWeight() : dadGenome.getHolesWeight();
        double heightDifferenceWeight = random.nextBoolean() ? momGenome.getHeightDifferenceWeight() : dadGenome.getHeightDifferenceWeight();
        double islandWeight = random.nextBoolean() ? momGenome.getIslandWeight() : dadGenome.getIslandWeight();
        double parityWeight = random.nextBoolean() ? momGenome.getParityWeight() : dadGenome.getParityWeight();
        double edgeHeightWeight = random.nextBoolean() ? momGenome.getEdgeHeightWeight() : dadGenome.getEdgeHeightWeight();
        double blockadeWeight = random.nextBoolean() ? momGenome.getBlockadeWeight() : dadGenome.getBlockadeWeight();
        Genome child = new Genome(id, blockadeWeight, edgeHeightWeight, heightDifferenceWeight, holesWeight, islandWeight, parityWeight);

        /* mutation */
        int randomIndex = (int) Math.floor(Math.random() * (numberOfFeatures - 1));
        if (randomIndex == 0) child.setBlockadeWeight(-100 * Math.random());
        else if (randomIndex == 1) child.setEdgeHeightWeight( -100 * Math.random());
        else if (randomIndex == 2) child.setHeightDifferenceWeight( 100 * Math.random());
        else if (randomIndex == 3) child.setHolesWeight( -100 * Math.random());
        else if (randomIndex == 4) child.setIslandWeight( 100 * Math.random());
        else child.setParityWeight( 100 * Math.random());

        /* new born will replace the worst scored weights in the list, so population is evolved */
        population.set(selectReplacement(evaluationScore), child);

        return population;
    }

    /**
     * return the ids of parents
     */
    public static int[] selectParent(HashMap<Integer, Double> evaluationScore) {
        int[] parents = new int[2];
        int bestId = 0;
        double currBestScore = 0;

        //first parent
        for (Integer id : evaluationScore.keySet()) {
            if(evaluationScore.get(id) > currBestScore){
                bestId = id;
                currBestScore = evaluationScore.get(id);
            }
        }
        parents[0] = bestId;

        //second parent
        double bestScore = evaluationScore.get(bestId);
        currBestScore = 0;
        int saveId = bestId;
        evaluationScore.put(saveId, Double.NEGATIVE_INFINITY);
        for (Integer id : evaluationScore.keySet()) {
            if(evaluationScore.get(id) > currBestScore){
                bestId = id;
                currBestScore = evaluationScore.get(id);
            }
        }
        parents[1] = bestId;
        evaluationScore.put(saveId, bestScore);

        return parents;
    }

    /**
     * return the ids of parents
     */
    public static int selectReplacement(HashMap<Integer, Double> evaluationScore) {
        int replaceId = 0;
        double currScore = Double.POSITIVE_INFINITY;

        for (Integer id : evaluationScore.keySet()) {
            if(evaluationScore.get(id) < currScore){
                replaceId = id;
                currScore = evaluationScore.get(id);
            }
        }

        return replaceId;
    }

    /* test */
    public static void main(String[] args) {
        GeneticAlgorithm GA =  new GeneticAlgorithm();
        ArrayList<Genome> population = GA.initializePopulation();
        State s = new State();

        HashMap<Integer, Double> scores = evaluatePopulation(population, s);
        for (Integer id : scores.keySet()) {
            System.out.println(id + ": "+ scores.get(id));
        }
        population = evolve(population, scores);
        scores = evaluatePopulation(population, s);
        for (Integer id : scores.keySet()) {
            System.out.println(id + ": "+ scores.get(id));
        }
    }
}
