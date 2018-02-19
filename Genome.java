public class Genome {

    private int id;
    private int rowsCleared;
    private int heightDifference;
    private int holes;

    public Genome (int id, int rowsCleared, int heightDifference, int holes) {
        this.id = id;
        this.rowsCleared = rowsCleared;
        this.heightDifference = heightDifference;
        this.holes = holes;
    }

    public void setId(int id) {this.id = id;}
    public void setRowsCleared(int rowsCleared) {this.rowsCleared = rowsCleared;}
    public void setHeightDifference(int heightDifference) {this.heightDifference = heightDifference;}
    public void setHoles(int holes) {this.holes = holes;}

    public int getId() {return id;}
    public int getRowsCleared() {return rowsCleared;}
    public int getHeightDifference() {return heightDifference;}
    public int getHoles() {return holes;}
}
