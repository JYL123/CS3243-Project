import java.util.concurrent.locks.ReentrantLock;
import java.util.ArrayList;
import java.util.HashMap;

public class GAThread{
   private int largeNumber = 50;
   private int numFeatures = 6;
   double[] weights = new double[numFeatures];
   GeneticAlgorithm GA = new GeneticAlgorithm();
   ArrayList<Genome> population = GA.initializePopulation();
   private int blockadeWeightIndex = 0;
   private int edgeHeightWeightIndex = 1;
   private int heightDifferenceWeightIndex = 2;
   private int holesWeightIndex = 3;
   private int islandWeightIndex = 4;
   private int parityWeightIndex = 5;
   private ReentrantLock lock = new ReentrantLock();

   /* return best set of weights */
   private void runGA(State state, ArrayList<Genome> population) {
    //get the evaluation score for each individual in the populaion
    HashMap<Integer, Double> evaluationScore = GA.evaluatePopulation(population, state);
    //scores before evolve
    System.out.println("First generation score:");
    for (Integer id : evaluationScore.keySet()) {
        System.out.println("First generation score:");
        System.out.println(id + ": "+ evaluationScore.get(id));
        System.out.println("");
    }
    //run
    for(int i = 0; i < largeNumber; i++) {
        population = GA.evolve(population, evaluationScore);
        evaluationScore = GA.evaluatePopulation(population, state);
        //print score after each evolution
        System.out.println("Score after evolving:");
        for (Integer id : evaluationScore.keySet()) {
            System.out.println(id + ": "+ evaluationScore.get(id));
        }
    }
    //scores after evolve
    for (Integer id : evaluationScore.keySet()) {
        System.out.println(id + ": "+ evaluationScore.get(id));
    }
    
   }

   public void firstThread(State state) throws InterruptedException {
       lock.lock();

       try {
        runGA(state, population);
       } finally {
        lock.unlock();
       }
   }

   public void secondThread(State state) throws InterruptedException {
       lock.lock();
       
       try {
        runGA(state, population);
       } finally {
        lock.unlock();
       }
   }

   public double[] finished(State state) {
       HashMap<Integer, Double> evaluationScore = GA.evaluatePopulation(population, state);
       double currBestScore = 0;
       int bestId = 0;
       for(Integer id: evaluationScore.keySet()) {
           if(evaluationScore.get(id) > currBestScore){
               bestId = id;
               currBestScore = evaluationScore.get(id);
           }
       }

       Genome individual = population.get(bestId);
       weights[blockadeWeightIndex] = individual.getBlockadeWeight();
       weights[edgeHeightWeightIndex] = individual.getEdgeHeightWeight();
       weights[heightDifferenceWeightIndex] = individual.getHeightDifferenceWeight();
       weights[holesWeightIndex] = individual.getHolesWeight();
       weights[islandWeightIndex] = individual.getIslandWeight();
       weights[parityWeightIndex] = individual.getParityWeight();

       return weights;
   }

   public static void main(String[] args) throws Exception {
       final GAThread gat = new GAThread();
       // should be replaced by a state
       State state = new State();

       Thread t1 = new Thread(new Runnable() {
           public void run() {
               try {
                   gat.firstThread(state);
               } catch (InterruptedException e) {
                   e.printStackTrace();
               }
           }
       });

       Thread t2 = new Thread(new Runnable() {
            public void run() {
                try {
                    gat.secondThread(state);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        gat.finished(state);
   }

}