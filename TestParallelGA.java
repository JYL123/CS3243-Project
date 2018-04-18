import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * The optimal result is obtained from this file.
 * "Optimal": 1. num of rows cleared; 2. learning rate (increment, fluctuation)
 */
public class TestParallelGA {

    private static double[] HEURISTICS = {-1, -1, -1, -1, 5, -1};
    private static int populationSize = 25;
    private static double parentsSelectionRatio = 0.5;
    private static int numberOfFeatures = 6;
    private static double threadhold = 0.1;
    private double bestFitness = 0;
    private double[] bestWeights = new double[numberOfFeatures];

    ArrayList<double[]> population = new ArrayList<>();
    private final int childrenCount;
    private final int parentCount;
    private final int simulationsPerChild;
    private final double generations;
    private final double survivalRate;
    private final double matingRate;
    private final double mutationProbability;

    TestParallelGA(int childrenCount, int parentCount, int simulationsPerChild, int generations, double survivalRate, double matingRate, double mutationProbability) {
        this.childrenCount = childrenCount;
        this.parentCount = parentCount;
        this.simulationsPerChild = simulationsPerChild;
        this.generations = generations;
        this.survivalRate = survivalRate;
        this.matingRate = matingRate;
        this.mutationProbability = mutationProbability;

        // Initialise children
        for (int i = 0; i < populationSize; i++) {
            double[] weights = new double[numberOfFeatures];
            for (int j = 0; j < weights.length; j++) {
                weights[j] = Math.random() * HEURISTICS[j];
                //weights[j] = MIN_WEIGHTS[j] + Math.random() * (MAX_WEIGHTS[j] - MIN_WEIGHTS[j]);
            }

            population.add(weights);
        }
    }

    // get the average over 5 simulation per child
    double evaluateWeights(double[] weights) {
        return IntStream.range(0, simulationsPerChild)
                .parallel()
                .map(i -> simulate(weights))
                .sum()/simulationsPerChild;
    }

    int simulate(double[] weights) {
        int[][][] legalMoves = State.legalMoves;
        SerializedState state = new SerializedState();

        while (!state.lost) {
            final SerializedState s = state;
            int nextPiece = SerializedState.randomPiece();

            List<Double> valuations = Stream.of(legalMoves[s.nextPiece])
                    .parallel()
                    .map(move -> PlayerSkeleton.transition(s, move, nextPiece))
                    .map(nextState -> PlayerSkeleton.evaluate(nextState, weights))
                    .collect(Collectors.toList());

            int maxMove = 0;
            double maxValuation = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < valuations.size(); i++) {
                if (valuations.get(i) > maxValuation) {
                    maxMove = i;
                    maxValuation = valuations.get(i);
                }
            }

            state = PlayerSkeleton.transition(state, legalMoves[s.nextPiece][maxMove]);

        }

        return state.cleared;
    }

    /*
     * update population, each time choose a certain proportion of the population and produce children
     * children will make up another 50% of the population
     * return an updated population
     */
    public static ArrayList<double[]> evolve (ArrayList<double[]> population, List<Double> evaluationScore) {
        return updatePopulation(selectParent(evaluationScore, population), evaluationScore);
    }

    /**
     * top parentsSelectionRatio (a percentage) of the population is chosen for evolution
     * return the ids of parents
     */
    private static List<double[]> selectParent(List<Double> evaluationScore, ArrayList<double[]> population) {
        // certain percentage of population
        List<double[]> parents = new ArrayList<double[]>();

        List<Double> sortedScore = new ArrayList<Double>(evaluationScore);
        sortedScore.sort((a, b) -> Double.compare(b, a));
        double benchmark = sortedScore.get((int) Math.floor(populationSize * parentsSelectionRatio));

        System.out.println("population size: " + population.size());
        for(int i = 0; i <population.size(); i++) {
            if(parents.size() < population.size()) {
                if (evaluationScore.get(i) >= benchmark) {
                    parents.add(population.get(i));
                    //System.out.println("score selected: " + evaluationScore.get(i));
                }
            } else break;
        }

        return parents;
    }

    private static ArrayList<double[]> updatePopulation(List<double[]> parents, List<Double> fitness) {
        int childrenNum = populationSize - parents.size();
        ArrayList<double[]> newPopulation = new ArrayList<double[]>();

        //add parents in newPopulation
        IntStream.range(0, parents.size())
                .forEach(i -> {
                    newPopulation.add(parents.get(i));
                });


        for (int i = 0; i < childrenNum; i++) {

            List<double[]> selParents = tournamentSelection(fitness, parents);

            //get parents
            double[] momGenome = selParents.get(0);
            double[] dadGenome = selParents.get(1);

            /* crossover */
            double[] child = crossover(momGenome, dadGenome);

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

    //TODO: avoid mutating on rowsCleared? it will not be selected into the next round of generation if the performance is poor.
    /* Swap Mutation */
    private static double[] mutation (double[] child) {
        /* swap */
        int randomIndex1 = (int) Math.floor(Math.random() * (numberOfFeatures));
        int randomIndex2 = (int) Math.floor(Math.random() * (numberOfFeatures));
        double temp = child[randomIndex1];
        child[randomIndex1] = child[randomIndex2];
        child[randomIndex2] = temp;

        return child;
    }

    /* cross over */
    private static double[] crossover (double[] momGenome, double[] dadGenome) {
        Random random = new Random();
        double[] child = new double[momGenome.length];
        IntStream.range(0, momGenome.length)
                .forEach(i -> {
                    child[i] = random.nextBoolean() ? momGenome[i] : dadGenome[i];
                });
        return child;
    }

    /*
    * Tournament selection, with top 50% parents to select parents for crossover
    * */
    private static List<double[]> tournamentSelection(List<Double> fitness, List<double[]> parents) {
        List<double[]> selParents = new ArrayList<>();
        double betterFitness = Double.NEGATIVE_INFINITY;
        int better = 0;
        //select 2 parents
        for(int j = 0; j< 2; j++) {
            //binary tournament selection
            for (int i = 0; i < 2; i++) {
                int individual = (int) Math.floor(Math.random() * parents.size());
                double individualFitness = fitness.get(individual);
                if (betterFitness == Double.NEGATIVE_INFINITY || individualFitness > betterFitness) {
                    betterFitness = individualFitness;
                    better = individual;
                }
            }
            selParents.add(parents.get(better));
            //reset best for next round
            betterFitness = Double.NEGATIVE_INFINITY;
        }

        return selParents;
    }

    public void run() {
        System.out.println("starting Genetic Algorithm");

        for (int generation = 0; generation < generations; generation++) {
            // Fitness
            // We run this in parallel because this is the most time consuming part
            System.out.printf("Simulating generation %d...\n", generation);
            List<Double> fitness = population
                    .parallelStream()
                    .map(this::evaluateWeights)
                    .collect(Collectors.toList());

            //evolution
            population = evolve(population, fitness);


            //score after evolving
            fitness = population
                    .parallelStream()
                    .map(this::evaluateWeights)
                    .collect(Collectors.toList());

            //print
            int maxIndex = 0;
            double maxFitness = fitness.get(0);
            for (int i = 1; i < fitness.size(); i++) {
                if (fitness.get(i) > maxFitness) {
                    maxIndex = i;
                    maxFitness = fitness.get(i);
                }
            }

            System.out.println("Average fitness: "+ fitness.stream().mapToDouble(i -> i).average());
            System.out.printf("Best Fitness: %f\n", maxFitness);
            System.out.print("Best Weights: {");
            for (double w : population.get(maxIndex)) {
                System.out.printf("%f, ", w);
            }
            System.out.println("}");
        }
    }

    public static void main(String[] args) {
        State s = new State();

        TestParallelGA ga = new TestParallelGA(50, 2, 5, 100, 0.25, 0.5, 0.1);
        ga.run();
    }
}
