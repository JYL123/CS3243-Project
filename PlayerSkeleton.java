import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class PlayerSkeleton {

	//implement this function to have a working system
	public int pickMove(State s, int[][] legalMoves) {

		// This should eventually be learned by our genetic algorithm
		double[] weights = {100, -1, 1};

		// Pick the move with highest valuation
		// We should implement minimax for better moves
		List<Double> valuations = Stream.of(legalMoves)
				.parallel()
				.map(move -> transition(s, move))
				.map(state -> evaluate(state, weights))
				.collect(Collectors.toList());

		int move = 0;
		for (int i = move + 1; i < valuations.size(); i++) {
			if (valuations.get(i) > valuations.get(move)) {
				move = i;
			}
		}

		return move;
	}

	private static double evaluate(SerializedState s, double[] weights) {
		if (s.lost) {
			return Double.NEGATIVE_INFINITY;
		}

		double valuation = 0;

		// This should eventually be the full list of evaluation metrics
		valuation += s.cleared * weights[0];
		valuation += getHighestCol(s.field) * weights[1];
		valuation += getLowestCol(s.field) * weights[2];

		return valuation;
	}

	private static int getHolesCount(int[][] field, int[] tops) {

		int holeCount = 0;
		boolean isHole = false;
		for (int i = 0; i < State.COLS; i++) {
			for (int j = tops[i]; j >= 0; j--) {
			    if (field[j][i] != 0) {
			        isHole = true;
                } else if(isHole && field[j][i] == 0) {
			        holeCount++;
			        isHole = false;
                }
            }
            isHole = false;
		}

		return holeCount;
	}

	private static int getBlockadeCount (int[][] field, int[] tops) {

	    int totalBlockade = 0;
	    int tempBlockade = 0;
	    boolean isHole = false;
	    for (int i = 0; i < State.COLS; i++) {
	        for (int j = tops[i]; j >= 0; j--) {
	            if (field[j][i] != 0) {
	                isHole = true;
	                tempBlockade++;
                } else if (isHole) {
	                totalBlockade += tempBlockade;
	                tempBlockade = 0;
	                isHole = false;
                }
            }
            //reset tempBlockade when changing cols
            tempBlockade = 0;
        }
        return totalBlockade;
    }

    private static int getParityCount (int[][] field) {

	    int filledCount = 0;
        for (int i = 0; i < State.COLS; i++) {
            for (int j = State.ROWS - 1; j >= 0; j--) {
                if (field [j][i] != 0) {
                    filledCount++;
                }
            }
        }

        int parity = (State.COLS * State.ROWS) - filledCount;
        if (parity < 0) {
            return parity * -1;
        } else {
            return parity;
        }
    }
	private static int getHighestCol(int[][] field) {
		return getTops(field).max().getAsInt();
	}

	private static int getLowestCol(int[][] field) {
		return getTops(field).min().getAsInt();
	}

	/*
	 * Returns a stream of the height of the top piece in each column
	 */
	private static IntStream getTops(int[][] field) {
		return IntStream.range(0, State.COLS)
				.map(j -> {
					for (int i = State.ROWS-1; i >= 0; i--) {
						if (field[i][j] != 0) {
							return i;
						}
					}
					return 0;
				});
	}

	private class SerializedState {
		public final int[][] field;
		public final boolean lost;
		public final int cleared;
		public final int[] tops;

		public SerializedState(int[][] field, boolean lost, int cleared, int[] tops) {
			this.field = field;
			this.lost = lost;
			this.cleared = cleared;
			this.tops = tops;
		}
	}

	private SerializedState transition(State s, int[] move) {
		int nextPiece = s.getNextPiece();
		int orient = move[0];
		int slot = move[1];

		int ROWS = State.ROWS;
		int COLS = State.COLS;
		boolean lost = s.lost;
		int turn = s.getTurnNumber();
		int cleared = s.getRowsCleared();

		// Deep copy
		int[][] field = new int[ROWS][];
		for (int i = 0; i < ROWS; i++) {
			field[i] = s.getField()[i].clone();
		}
		int[] top = s.getTop().clone();

		int[][][] pTop = State.getpTop();
		int[][][] pBottom = State.getpBottom();
		int[][] pWidth = State.getpWidth();
		int[][] pHeight = State.getpHeight();

		/*
		 * This section of code is taken and modified from State.java
		 */

		//height if the first column makes contact
		int height = top[slot]-pBottom[nextPiece][orient][0];
		//for each column beyond the first in the piece
		for(int c = 1; c < pWidth[nextPiece][orient];c++) {
			height = Math.max(height,top[slot+c]-pBottom[nextPiece][orient][c]);
		}

		//check if game ended
		if(height+pHeight[nextPiece][orient] >= ROWS) {
			lost = true;
			return new SerializedState(field, lost, cleared, top);
		}


		//for each column in the piece - fill in the appropriate blocks
		for(int i = 0; i < pWidth[nextPiece][orient]; i++) {

			//from bottom to top of brick
			for(int h = height+pBottom[nextPiece][orient][i]; h < height+pTop[nextPiece][orient][i]; h++) {
				field[h][i+slot] = turn;
			}
		}

		//adjust top
		for(int c = 0; c < pWidth[nextPiece][orient]; c++) {
			top[slot+c]=height+pTop[nextPiece][orient][c];
		}

		int rowsCleared = 0;

		//check for full rows - starting at the top
		for(int r = height+pHeight[nextPiece][orient]-1; r >= height; r--) {
			//check all columns in the row
			boolean full = true;
			for(int c = 0; c < COLS; c++) {
				if(field[r][c] == 0) {
					full = false;
					break;
				}
			}
			//if the row was full - remove it and slide above stuff down
			if(full) {
				rowsCleared++;
				cleared++;
				//for each column
				for(int c = 0; c < COLS; c++) {

					//slide down all bricks
					for(int i = r; i < top[c]; i++) {
						field[i][c] = field[i+1][c];
					}
					//lower the top
					top[c]--;
					while(top[c]>=1 && field[top[c]-1][c]==0)	top[c]--;
				}
			}
		}

		return new SerializedState(field, lost, cleared, top);
	}


	public static void main(String[] args) {
		State s = new State();
		new TFrame(s);
		PlayerSkeleton p = new PlayerSkeleton();
		while(!s.hasLost()) {
			s.makeMove(p.pickMove(s,s.legalMoves()));
			s.draw();
			s.drawNext(0,0);
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}

}

