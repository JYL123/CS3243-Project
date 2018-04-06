import java.util.Random;

/* class for weights */
public class Genome {

    private int id;
    /*weights for each feature, it is initialized randomly*/
    private double holesWeight = Math.random();
    private double highestColWeight = Math.random(); //height of highest column
    private double lowestColWeight = Math.random();
    private double islandWeight = Math.random();
    private double parityWeight = Math.random();
    private double edgeHeightWeight = Math.random();
    private double blockadeWeight = Math.random();
    private double heightDifferenceWeight = Math.abs(highestColWeight - lowestColWeight);

    public Genome(int id) {
        this.id = id;
    }

    public Genome (int id, double heightDifferenceWeight, double holesWeight, double islandWeight,
                        double parityWeight, double edgeHeightWeight, double blockadeWeight) {
        this.id = id;
        this.heightDifferenceWeight = heightDifferenceWeight;
        this.holesWeight = holesWeight;
        this.islandWeight = islandWeight;
        this.parityWeight = parityWeight;
        this.edgeHeightWeight = edgeHeightWeight;
        this.blockadeWeight = blockadeWeight;
    }

    public void setId(int id) {this.id = id;}
    public void setHeightDifferenceWeight(double heightDifferenceWeight) {this.heightDifferenceWeight = heightDifferenceWeight;}
    public void setHolesWeight(double holesWeight) {this.holesWeight = holesWeight;}
    public void setIslandWeight(double islandWeight) {this.islandWeight = islandWeight;}
    public void setParityWeight(double parityWeight) {this.parityWeight = parityWeight;}
    public void setEdgeHeightWeight(double edgeHeightWeight) {this.edgeHeightWeight = edgeHeightWeight;}
    public void setBlockadeWeight(double blockadeWeight) {this.blockadeWeight = blockadeWeight;}

    public int getId() {return id;}
    public double getHeightDifferenceWeight() {return heightDifferenceWeight;}
    public double getHolesWeight() {return holesWeight;}
    public double getIslandWeight() {return islandWeight;}
    public double getParityWeight() {return parityWeight;}
    public double getEdgeHeightWeight() {return edgeHeightWeight;}
    public double getBlockadeWeight() {return blockadeWeight;}
}
