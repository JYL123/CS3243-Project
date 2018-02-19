import java.util.ArrayList;
import java.util.Random;

public class GeneticAlgorithm {
    private ArrayList<Genome> genomes;
    private int gridHeight = 10;;
    private double rowsClearWeight = 0.5;
    private double heightDifferenceWeight = 0.03;
    private double holesWeight = 0.02;
    private int populationSize = 50;

    private ArrayList<Genome> initializaPopulation () {
        genomes = new ArrayList<>();

        for (int i = 0; i < populationSize; i++) {
            int id = i;
            int rowsCleared = (int)Math.ceil(Math.random() * gridHeight);
            int heightDifference = (int)Math.ceil(Math.random() * gridHeight);
            int holes = (int)Math.ceil(Math.random() * gridHeight);

            Genome genome = new Genome(id, rowsCleared, heightDifference, holes);
            genomes.add(genome);
        }
        return genomes;
    }

    /*
     * return the utility value for an individual
     */
    private double individualEvaluation (Genome genome) {
        /* utility function */
        double utilityValue = genome.getRowsCleared() * rowsClearWeight + genome.getHeightDifference() * heightDifferenceWeight
                    + genome.getHoles() * holesWeight;
        return utilityValue;
    }

    private Genome evolve (ArrayList<Genome> genomes) {
        /* random selection of parents */
        int mom = (int)Math.ceil(Math.random() * (populationSize - 1));
        int dad = (int)Math.ceil(Math.random() * (populationSize - 1));

        Genome momGenome = genomes.get(mom);
        Genome dadGenome = genomes.get(dad);

        /* cross over */
        Random random = new Random();
        int rowsCleared = random.nextBoolean() ? momGenome.getRowsCleared() : dadGenome.getRowsCleared();
        int heightDifference = random.nextBoolean() ? momGenome.getHeightDifference() : dadGenome.getHeightDifference();
        int holes = random.nextBoolean() ? momGenome.getHoles() : dadGenome.getHoles();
        int id = random.nextBoolean() ? momGenome.getId() : dadGenome.getId();
        Genome child = new Genome(id, rowsCleared, heightDifference, holes);
        /* new born will replace one of the parents, so population is evolved */
        genomes.set(id, child);

        /* mutation */
        int numberOfFeatures = 3;
        int randomIndex = (int) Math.floor(Math.random() * (numberOfFeatures - 1));
        if (randomIndex == 0) child.setRowsCleared((int) Math.ceil(Math.random()) * gridHeight);
        else if (randomIndex == 1) child.setHeightDifference((int) Math.ceil(Math.random()) * gridHeight);
        else child.setHoles((int) Math.ceil(Math.random()) * gridHeight);

        return child;
    }

    public static void main(String args[]) {
        GeneticAlgorithm GA = new GeneticAlgorithm();
        /* Display the utility value for each child */
        for (int i = 0; i < 1000; i++) {
            ArrayList<Genome> population = GA.initializaPopulation();
            System.out.println(GA.individualEvaluation(GA.evolve(population)));
        }

    }

}
