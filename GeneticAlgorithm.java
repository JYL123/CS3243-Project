import java.util.ArrayList;
import java.util.Random;

public class GeneticAlgorithm {
    private ArrayList<Genome> genomes;
    private int gridHeight = 10;
    private int populationSize = 50;
    private int numberOfFeatures = 6; //highestCol and lowestCol are replaced by heightDifference
    /*features of a state, we randomly define a state in the begining*/
    private int holes = 4;
    private int highestCol = 8; //height of highest column
    private int lowestCol = 3;
    private int island = 4;
    private int parity = 3;
    private int edgeHeightWeight = 4;
    private int blockadeWeight = 3;
    private int heightDifference = highestCol - lowestCol;

    private ArrayList<Genome> initializaPopulation () {
        genomes = new ArrayList<>();

        for (int i = 0; i < populationSize; i++) {
            int id = i;
            Genome genome = new Genome(id);
            genomes.add(genome);
        }
        return genomes;
    }

    private Genome evolve (ArrayList<Genome> genomes) {
        /* random selection of parents */
        int mom = (int)Math.ceil(Math.random() * (populationSize - 1));
        int dad = (int)Math.ceil(Math.random() * (populationSize - 1));

        Genome momGenome = genomes.get(mom);
        Genome dadGenome = genomes.get(dad);

        /* cross over */
        Random random = new Random();
        int id = random.nextBoolean() ? momGenome.getId() : dadGenome.getId();
        double holesWeight = random.nextBoolean() ? momGenome.getHolesWeight() : dadGenome.getHolesWeight();
        double heightDifferenceWeight = random.nextBoolean() ? momGenome.getHeightDifferenceWeight() : dadGenome.getHeightDifferenceWeight();
        double islandWeight = random.nextBoolean() ? momGenome.getIslandWeight() : dadGenome.getIslandWeight();
        double parityWeight = random.nextBoolean() ? momGenome.getParityWeight() : dadGenome.getParityWeight();
        double edgeHeightWeight = random.nextBoolean() ? momGenome.getEdgeHeightWeight() : dadGenome.getEdgeHeightWeight();
        double blockadeWeight = random.nextBoolean() ? momGenome.getBlockadeWeight() : dadGenome.getBlockadeWeight();
        Genome child = new Genome(id, heightDifferenceWeight, holesWeight, islandWeight, parityWeight, edgeHeightWeight, blockadeWeight);

        /* mutation */
        int randomIndex = (int) Math.floor(Math.random() * (numberOfFeatures - 1));
        if (randomIndex == 0) child.setHeightDifferenceWeight(Math.random());
        else if (randomIndex == 1) child.setHolesWeight(Math.random());
        else if (randomIndex == 2) child.setIslandWeight(Math.random());
        else if (randomIndex == 3) child.setParityWeight(Math.random());
        else if (randomIndex == 4) child.setEdgeHeightWeight(Math.random());
        else child.setBlockadeWeight(Math.random());

        /* new born will replace one of the parents, so population is evolved */
        genomes.set(id, child);

        return child;
    }

    /*
     * evaluate genome
     */
    private double individualEvaluation (Genome genome) {
        //randomly choose s set of weights
        int individual = (int)Math.ceil(Math.random() * (populationSize - 1));

        for (int i = 0; i < ORIENT; i++) {
            for (int j = 0; j< SLOT; j++) {
                double score = evaluation(transition(state, genome), LegalMove[i][j]);
            }
        }

        return score;
    }

    /* main will be serving the purpose of testing here */
    public static void main(String args[]) {
        GeneticAlgorithm GA = new GeneticAlgorithm();
        ArrayList<Genome> population = GA.initializaPopulation();
        /* Display the utility value for each child */
        for (int i = 0; i < 100000000; i++) {
            System.out.println(GA.evolve(population));
        }

        ArrayList<Genome> bestWeights = somemethod;

    }

}
