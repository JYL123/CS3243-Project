import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GeneticAlgorithm {
    private static ArrayList<double[]> population;
    private static int populationSize = 6;                         ;
    private static int numberOfFeatures = 6; //highestCol and lowestCol are replaced by heightDifference
    /*0: rowTransitions 1: columnTransitions 2: heightDifferenceWeight 3: holesWeight 4: rowsCleared 5: wells*/
    //DEFAULT_WEIGHTS = {-1.3343042352708279, -0.7720367230689456, -0.0153739588059979, -0.8663918638956187, 1.9153960163441597, -0.48611250378933557};
    private static double[] heuristic = {-1, -1, -1, -1, 5, -1};
    private static double threadhold = 0.1;
    private static double parentsSelectionRatio = 0.5;
    private static double[] bestWeights = new double[numberOfFeatures];
    private static double bestFitness = Integer.MIN_VALUE;
    private static int generations = 6;

    public static ArrayList<double[]> initializePopulation () {
        population = new ArrayList<>(populationSize);
        //initial random weights for each feature
        IntStream.range(0, populationSize+1)
                .parallel()
                .forEach(i -> {
                    //individual
                    double[] weights =  new double[numberOfFeatures];
                    for (int j = 0; j< weights.length; j++) {weights[j] = Math.random() * heuristic[j];}
                    population.add(weights);
                });
        return population;
    }

    /*
     * evaluate sets of weights
     * first we need to evaluate weights for every individual of the population
     */
    private static double evaluateIndividual (double[] individual, State s) {
        double score = 0;
        PlayerSkeleton p = new PlayerSkeleton();
        while(!s.hasLost()) {
            s.makeMove(p.pickMove(s,s.legalMoves(), individual));
<<<<<<< HEAD
            if(s.getRowsCleared() % 1000 == 0)System.out.println(s.getRowsCleared() + " rows cleared.");
=======
            if(s.getRowsCleared() % 1000 ==0 && s.getRowsCleared() != 0) System.out.println(s.getRowsCleared() + " rows cleared.");
>>>>>>> 73dc983a9cbb90838fbea69a3bfc6d058809daed
        }
        score = s.getRowsCleared();

        return score;
    }

    /*
     * give score to each set of weights
     */
    public static List<Double> evaluatePopulation (ArrayList<double[]> population) {
        List<Double> fitness = population
                .parallelStream()
                .map(i -> evaluateIndividual(i, new State())
                ).collect(Collectors.toList());

        return fitness;
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

        //select parents
        IntStream.range(0, populationSize)
                .parallel()
                .forEach(i -> {
                   if(evaluationScore.get(i) >= benchmark) {
                       parents.add(population.get(i));
                       System.out.println("score seleceted: " + evaluationScore.get(i));
                   }
                });

        return parents;
    }

    private static ArrayList<double[]> updatePopulation(List<double[]> parents, List<Double> fitness) {
        int childrenNum = population.size() - parents.size();
        ArrayList<double[]> newPopulation = new ArrayList<double[]>();

        //add parents in newPopulation
        IntStream.range(0, parents.size())
                .parallel()
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
                .parallel()
                .forEach(i -> {
                    child[i] = random.nextBoolean() ? momGenome[i] : dadGenome[i];
                });
        return child;
    }

    /*
    * Tournament selection, with top 10% parents to select parents for crossover
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
        System.out.print("Initialize population: ");
        ArrayList<double[]> population = initializePopulation();
        //double[] population = {-1.3343042352708279, -0.7720367230689456, -0.0153739588059979, -0.8663918638956187, 1.9153960163441597, -0.48611250378933557};

        //initial population
        List<Double> scores = evaluatePopulation(population);
        System.out.print("Initial evaluation: ");
        System.out.println(Arrays.toString(scores.toArray()));

        for(int j = 0; j < generations; j++) {
            System.out.println("Evolution " + j + ": ");
            //evolution
            population = evolve(population, scores);

            //after evolving
            scores = evaluatePopulation(population);
            for(int i = 0; i < scores.size(); i++) {
                if(scores.get(i) > bestFitness) {
                    bestFitness = scores.get(i);
                    bestWeights = population.get(i);
                }
            }

            System.out.println("Best fitness: " + bestFitness);
            System.out.print("Best weight: ");
            System.out.println(Arrays.toString(bestWeights));

        }

    }

    /* test */
    public static void main(String[] args) {
        GeneticAlgorithm GA =  new GeneticAlgorithm();
        GA.run();
    }
}
