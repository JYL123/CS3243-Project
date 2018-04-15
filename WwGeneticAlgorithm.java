import java.security.cert.CollectionCertStoreParameters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by weiwen on 11/4/18.
 */
public class WwGeneticAlgorithm {
    private final static double[] MIN_WEIGHTS = {0, 0, 0, 0, 0, 0};
    private final static double[] MAX_WEIGHTS = {-50, -50, 50, -100, 100, 50};

    List<double[]> population = new ArrayList<>();
    private final int childrenCount;
    private final int parentCount;
    private final int simulationsPerChild;
    private final double generations;
    private final double survivalRate;
    private final double matingRate;
    private final double mutationProbability;

    WwGeneticAlgorithm(int childrenCount, int parentCount, int simulationsPerChild, int generations, double survivalRate, double matingRate, double mutationProbability) {
        this.childrenCount = childrenCount;
        this.parentCount = parentCount;
        this.simulationsPerChild = simulationsPerChild;
        this.generations = generations;
        this.survivalRate = survivalRate;
        this.matingRate = matingRate;
        this.mutationProbability = mutationProbability;

        // Initialise children
        for (int i = 0; i < childrenCount; i++) {
            double[] weights = new double[MIN_WEIGHTS.length];
            for (int j = 0; j < weights.length; j++) {
                weights[j] = MIN_WEIGHTS[j] + Math.random() * (MAX_WEIGHTS[j] - MIN_WEIGHTS[j]);
            }

            population.add(weights);
        }
    }

    double evaluateWeights(double[] weights) {
        return IntStream.range(0, simulationsPerChild)
                .parallel()
                .map(i -> simulate(weights))
                .sum();
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

            // Selection
            List<Double> sortedFitness = new ArrayList<>(fitness);
            sortedFitness.sort((a, b) -> Double.compare(b, a));

            double parentCutoff = sortedFitness.get((int) (childrenCount * matingRate));
            List<double[]> parents = new ArrayList<>();
            IntStream.range(0, childrenCount)
                    .filter(i -> fitness.get(i) >= parentCutoff)
                    .forEach(i -> parents.add(population.get(i)));

            double survivorCutoff = sortedFitness.get((int) (childrenCount * survivalRate));
            List<double[]> survivors = new ArrayList<>();
            IntStream.range(0, childrenCount)
                    .filter(i -> fitness.get(i) >= survivorCutoff)
                    .forEach(i -> survivors.add(population.get(i)));

            System.out.print("Fitness: ");
            System.out.println(sortedFitness);

            // Crossover
            List<double[]> children = new ArrayList<>(survivors);
            IntStream.range(0, childrenCount - survivors.size()).forEach(c -> {
                double[] weights = new double[MIN_WEIGHTS.length];
                List<double[]> myParents = new ArrayList<>();
                // Find our parents
                IntStream.range(0, parentCount).forEach(i -> {
                    int parent = (int) (Math.random() * parents.size());
                    myParents.add(parents.get(parent));
                });
                // Create our genome
                IntStream.range(0, MIN_WEIGHTS.length).forEach(i -> {
                    int parent = (int) (Math.random() * parentCount);
                    weights[i] = parents.get(parent)[i];
                });
                children.add(weights);
            });


            // Mutation
            children.forEach(c -> {
                IntStream.range(0, MIN_WEIGHTS.length).forEach(i -> {
                    if (Math.random() < mutationProbability) {
                        c[i] = MIN_WEIGHTS[i] + Math.random() * (MAX_WEIGHTS[i] - MIN_WEIGHTS[i]);
                    }
                });
            });

            population = children;
        }

        // Print weights
        List<Double> fitness = population
                .parallelStream()
                .map(this::evaluateWeights)
                .collect(Collectors.toList());

        int maxIndex = 0;
        double maxFitness = fitness.get(0);
        for (int i = 1; i < fitness.size(); i++) {
            if (fitness.get(i) > maxFitness) {
                maxIndex = i;
                maxFitness = fitness.get(i);
            }
        }

        System.out.printf("Fitness: %f\n", maxFitness);
        System.out.print("Weights: {");
        for (double w : population.get(maxIndex)) {
            System.out.printf("%f, ", w);
        }
        System.out.println("}");
    }

    public static void main(String[] args) {
        State s = new State();

        WwGeneticAlgorithm ga = new WwGeneticAlgorithm(50, 2, 5, 10, 0.25, 0.5, 0.01);
        ga.run();
    }
}
