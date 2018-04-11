import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Map.Entry;
import java.util.LinkedHashMap;
import static java.util.Collections.reverseOrder;

public class GeneticAlgorithm {
    private static ArrayList<Genome> population;
    private static int populationSize = 6;
    private static int numberOfFeatures = 6; //highestCol and lowestCol are replaced by heightDifference
    /*features of a state, we randomly define a state in the begining*/
    private static int blockadeWeightIndex = 0;
    private static int edgeHeightWeightIndex = 1;
    private static int heightDifferenceWeightIndex = 2;
    private static int holesWeightIndex = 3;
    private static int islandWeightIndex = 4;
    private static int parityWeightIndex = 5;
    private static double threadhold = 0.1;
    private static double parentsSelectionRatio = 0.5;


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
     * update population, each time choose a certain proportion of the population and produce children
     * children will make up another 50% of the population
     * return an updated population
     */
    public static ArrayList<Genome> evolve (ArrayList<Genome> population, HashMap<Integer, Double> evaluationScore) {
        return updatePopulation(selectParent(evaluationScore),population);
    }

    /**
     * top parentsSelectionRatio (a percentage) of the population is chosen for evolution
     * return the ids of parents
     */
    public static int[] selectParent(HashMap<Integer, Double> evaluationScore) {
        // certain percentage of population
        int numParents = (int) Math.floor(populationSize * parentsSelectionRatio);
        int[] parents = new int[numParents];
        // sort based on score
        HashMap<Integer, Double> sortedScore = sortByValue(evaluationScore);
        for (Integer id : sortedScore.keySet()) {
            System.out.println("sorted score:" + sortedScore.get(id));
        }
        numParents--;
        for (Integer id : sortedScore.keySet()) {
            if(numParents >= 0) {
                parents[numParents] = id;
            } else {
                break;
            }
            numParents--;
        }

        return parents;
    }

    public static ArrayList<Genome> updatePopulation(int[] parents, ArrayList<Genome> population) {
        int childrenNum = population.size() - parents.length;
        ArrayList<Genome> newPopulation = new ArrayList<Genome>();

        //add parents in newPopulation
        for (int i = 0; i< parents.length; i++) {
            newPopulation.add(population.get(parents[i]));
        }

        for (int i = 0; i < childrenNum; i++) {
            int randomIndexMom = (int) Math.floor(Math.random() * parents.length);
            int randomIndexDad = (int) Math.floor(Math.random() * parents.length);

            Genome momGenome = population.get(randomIndexMom);
            Genome dadGenome = population.get(randomIndexDad);

            /* crossover */
            Genome child = crossover(momGenome, dadGenome, newPopulation);

            /* mutation */
            Random mutation = new Random();
            boolean isMutation = false;
            double randomMutation = mutation.nextGaussian();
            if(randomMutation > threadhold) {
                isMutation = true;
            }
            if(isMutation) {
                mutation(child);
            }

            newPopulation.add(child);
        }

        return newPopulation;
    }

    /* Swap Mutation */
    public static Genome mutation (Genome child) {
        double[] childweights = new double[numberOfFeatures];
        childweights[0] = child.getBlockadeWeight();
        childweights[1] = child.getEdgeHeightWeight();
        childweights[2] = child.getHeightDifferenceWeight();
        childweights[3] = child.getHolesWeight();
        childweights[4] = child.getIslandWeight();
        childweights[5] = child.getParityWeight();

        /* swap */
        int randomIndex1 = (int) Math.floor(Math.random() * (numberOfFeatures));
        int randomIndex2 = (int) Math.floor(Math.random() * (numberOfFeatures));
        double temp = childweights[randomIndex1];
        childweights[randomIndex1] = childweights[randomIndex2];
        childweights[randomIndex2] = temp;

        child.setBlockadeWeight(childweights[0]);
        child.setEdgeHeightWeight(childweights[1]);
        child.setHeightDifferenceWeight(childweights[2]);
        child.setHolesWeight(childweights[3]);
        child.setIslandWeight(childweights[4]);
        child.setParityWeight(childweights[5]);

        return child;
    }

    /* cross over */
    public static Genome crossover (Genome momGenome, Genome dadGenome, ArrayList<Genome> newPopulation) {
        Random random = new Random();
        double holesWeight = random.nextBoolean() ? momGenome.getHolesWeight() : dadGenome.getHolesWeight();
        double heightDifferenceWeight = random.nextBoolean() ? momGenome.getHeightDifferenceWeight() : dadGenome.getHeightDifferenceWeight();
        double islandWeight = random.nextBoolean() ? momGenome.getIslandWeight() : dadGenome.getIslandWeight();
        double parityWeight = random.nextBoolean() ? momGenome.getParityWeight() : dadGenome.getParityWeight();
        double edgeHeightWeight = random.nextBoolean() ? momGenome.getEdgeHeightWeight() : dadGenome.getEdgeHeightWeight();
        double blockadeWeight = random.nextBoolean() ? momGenome.getBlockadeWeight() : dadGenome.getBlockadeWeight();
        Genome child = new Genome(newPopulation.size()+1, blockadeWeight, edgeHeightWeight, heightDifferenceWeight, holesWeight, islandWeight, parityWeight);

        return child;
    }

    public static <K, V extends Comparable<? super V>> HashMap<K, V> sortByValue(HashMap<K, V> map) {
        ArrayList<Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(reverseOrder(Entry.comparingByValue()));

        HashMap<K, V> result = new LinkedHashMap<>();
        for (Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
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
