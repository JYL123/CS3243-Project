import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class PlayerSkeleton {

	public final static double[] DEFAULT_WEIGHTS = {
			-12.017158881253732,
			-57.15851874259157,
			41.70493697627634,
			-37.20733213557791,
			56.73326127463958,
			35.316656436742065
	};

	//implement this function to have a working system
	public int pickMove(State s, int[][] legalMoves, double[] weights) {

		// Serialize our state
		SerializedState state = new SerializedState(s);

		// Pick the move with highest valuation
		return expectimax(state, legalMoves, weights, 1);
	}

	private static int expectimax(SerializedState s, int[][] legalMoves, double[] weights, int depth) {

		List<Double> valuations = Stream.of(legalMoves)
				.parallel()
				.map(move -> transition(s, move))
				.map(state -> expectimax(state, weights, depth, false))
				.collect(Collectors.toList());

		int maxMove = 0;
		double maxValuation = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < valuations.size(); i++) {
			if (valuations.get(i) > maxValuation) {
				maxMove = i;
				maxValuation = valuations.get(i);
			}
		}

		return maxMove;
	}

	private static double expectimax(SerializedState s, double[] weights, int depth, boolean isMax) {
		// Terminal node
		if (depth == 0) {
			return evaluate(s, weights);
		}

		// Max-node
		if (isMax) {
			int[][] legalMoves = State.legalMoves[s.nextPiece];
			return Stream.of(legalMoves)
					.parallel()
					.map(move -> transition(s, move))
					.map(state -> expectimax(state, weights, depth - 1, false))
					.max(Double::compare)
					.get();
		}

		// Exp-node
		else {
			return IntStream.range(0, State.N_PIECES)
					.parallel()
					.boxed()
					.map(piece -> new SerializedState(s.field, s.top, s.turn, piece, s.lost, s.cleared))
					.map(state -> expectimax(state, weights, depth, true))
					.reduce(0.0, Double::sum) / State.N_PIECES;
		}
	}

	public static double evaluate(SerializedState s, double[] weights) {
		if (s.lost) {
			return Double.NEGATIVE_INFINITY;
		}

		double valuation = 0;

		// This should eventually be the full list of evaluation metrics
		valuation += s.cleared;
		valuation += (getHighestCol(s.field) - getLowestCol(s.field)) * weights[2];
		valuation += getHolesCount(s.field, s.top) * weights[3];
		valuation += getBlockadeCount(s.field, s.top) * weights[0];
		valuation += getParityCount(s.field) * weights[5] ;
		valuation += getTotalHeightDiff(s.top) * weights[1];
		valuation += getIslandCount(s.field) * weights[4];

		return valuation;
	}
  
  private static int getIslandCount(int[][] field) {
		return 0;
	}

	private static int getHolesCount(int[][] field, int[] top) {
		int holeCount = 0;
		boolean isHole = false;
		for (int i = 0; i < State.COLS; i++) {
			for (int j = top[i]; j >= 0; j--) {
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

	private static int getBlockadeCount (int[][] field, int[] top) {

		int totalBlockade = 0;
		int tempBlockade = 0;
		boolean isHole = false;
		for (int i = 0; i < State.COLS; i++) {
			for (int j = top[i]; j >= 0; j--) {
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

	private static int getTotalHeightDiff (int[] top) {

		int totalDiff = 0;
		for (int i = 1; i < top.length; i++) {
			int diff = top[i] - top[i-1];

			if (diff < 0) {
				diff = diff * -1;
			}
			totalDiff += diff;
		}

		return totalDiff;
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

	public static SerializedState transition(SerializedState s, int[] move) {
		return transition(s, move, SerializedState.randomPiece());
	}

	public static SerializedState transition(SerializedState s, int[] move, int followingPiece) {
		int nextPiece = s.nextPiece;
		int orient = move[0];
		int slot = move[1];

		int ROWS = State.ROWS;
		int COLS = State.COLS;
		boolean lost = s.lost;
		int turn = s.turn;
		int cleared = s.cleared;

		// Deep copy
		int[][] field = new int[ROWS][];
		for (int i = 0; i < ROWS; i++) {
			field[i] = s.field[i].clone();
		}
		int[] top = s.top.clone();

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
			return new SerializedState(field, top, followingPiece, turn + 1, true, cleared);
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

		return new SerializedState(field, top, turn + 1, followingPiece, lost, cleared);
	}

	public static void main(String[] args) {
		State s = new State();
		new TFrame(s);
		PlayerSkeleton p = new PlayerSkeleton();
		while(!s.hasLost()) {
			s.makeMove(p.pickMove(s,s.legalMoves(),DEFAULT_WEIGHTS));
			s.draw();
			s.drawNext(0,0);
//			try {
//				Thread.sleep(300);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
		}
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}

}

class SerializedState {
	final int[][] field;
	final int[] top;
	final int turn;
	final int nextPiece;
	final boolean lost;
	final int cleared;

	SerializedState() {
		this(new int[State.ROWS][State.COLS],
				new int[State.COLS],
				0,
				randomPiece(),
				false,
				0);
	}

	SerializedState(int[][] field, int[] top, int turn, int nextPiece, boolean lost, int cleared) {
		this.field = field;
		this.top = top;
		this.turn = turn;
		this.nextPiece = nextPiece;
		this.lost = lost;
		this.cleared = cleared;
	}

	SerializedState(State s) {
		this(s.getField(),
				s.getTop(),
				s.getTurnNumber(),
				s.getNextPiece(),
				s.lost,
				s.getRowsCleared());
	}

	public static int randomPiece() {
		return (int) (Math.random() * State.N_PIECES);
	}


}
