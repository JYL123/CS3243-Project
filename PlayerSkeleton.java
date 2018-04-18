import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class PlayerSkeleton {

	public final static double[] DEFAULT_WEIGHTS = {-0.5409023035972038, -0.5779452238352727, -0.08382226928735714, -0.7237267127345023, 2.299698319650534, -0.11110083421382877};
	private int[] pieceHistory = new int[State.N_PIECES];

	//implement this function to have a working system
	public int pickMove(State s, int[][] legalMoves, double[] weights) {

		pieceHistory[s.nextPiece]++;

		// Serialize our state
		SerializedState state = new SerializedState(s, pieceHistory);

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
					.map(piece -> new SerializedState(s.field, s.top, s.turn, piece, s.pieceHistory,s.lost, s.cleared))
					.map(state -> expectimax(state, weights, depth, true))
					.reduce(0.0, Double::sum) / State.N_PIECES;
		}
	}

	public static double evaluate(SerializedState s, double[] weights) {
		if (s.lost) {
			return Double.NEGATIVE_INFINITY;
		}

		double valuation = 0;

		valuation += (getHighestCol(s) -getLowestCol(s)) * weights[2];
		valuation += getHolesCount(s.field, s.top) * weights[3];
		valuation += rowTransitions(s.field) * weights[0];
		valuation += getWellSums(s.field) * weights[5] ;
		valuation += columnTransitions(s.field) * weights[1];
		valuation += s.cleared * weights[4];

		return valuation;
	}

	private static int getHolesCount(int[][] field, int[] top) {
		int holeCount = 0;
		for (int i = 0; i < State.COLS; i++) {
			for (int j = top[i]; j >= 0; j--) {
				if(field[j][i] == 0) {
					holeCount++;
				}
			}
		}

		return holeCount;
	}

	private static int getHighestCol(SerializedState s) {
		int max = Arrays.stream(s.top).max().isPresent() ? Arrays.stream(s.top).max().getAsInt() : 0;
		return max;
	}

	private static int getLowestCol(SerializedState s) {
		return Arrays.stream(s.top).min().isPresent() ? Arrays.stream(s.top).min().getAsInt() : 0;
	}

	private static int getWellSums(int[][] field) {
		int wellSum = 0;
		int test = 0;

		// Case inner columns
		for (int i = 1; i < State.COLS - 1; i++) {
			int inc = 1;
			int sum = 0;
			test++;
			for (int j = field.length - 1; j >= 0; j--) {
				if (field[j][i] == 0 && field[j][i - 1] != 0 && field[j][i + 1] != 0) {
					sum += inc;
					inc++;
					test++;
					for (int k = j - 1; k >= 0; k--) {
						if (field[k][i] == 0) {
							sum += inc;
							inc++;
							test++;
						} else {
							wellSum += sum;
							break;
						}
					}
				}
			}
		}

		// Case if well is on the left side of the wall
		for (int j = field.length - 1; j >= 0; j--) {
			int inc = 1;
			int sum = 0;
			if (field[j][0] == 0 && field[j][1] != 0) {
				sum += inc;
				inc++;
				test++;
				for (int k = j - 1; k >= 0; k--) {
					if (field[k][0] == 0) {
						sum += inc;
						inc++;
						test++;
					} else {
						wellSum += sum;
						break;
					}
				}
			}
		}

		// Case if well is on the right side of the wall
		for (int j = field.length - 1; j >= 0; j--) {
			int inc = 1;
			int sum = 0;
			if (field[j][State.COLS - 1] == 0 && field[j][State.COLS - 2] != 0) {
				sum += inc;
				inc++;
				test++;
				for (int k = j - 1; k >= 0; k--) {
					if (field[k][State.COLS - 1] == 0) {
						sum += inc;
						inc++;
						test++;
					} else {
						wellSum += sum;
						break;
					}
				}
			}
		}
		return test;
	}


	public static int rowTransitions(int[][] field) {
		int tr = 0;

		for (int i = 0; i < field.length; i++) {
			boolean isEmptyCell = field[i][0] == 0;
			for (int j = 1; j < field[i].length; j++) {
				if (isEmptyCell && field[i][j] != 0) {
					tr++;
				}

				else if (!isEmptyCell && field[i][j] == 0) {
					tr++;
				}

				isEmptyCell = field[i][j] == 0;
			}
		}
		return tr;
	}

	public static int columnTransitions(int[][] field) {
		int tr = 0;
		boolean isEmptyCell;

		for (int i = 0; i < State.COLS; i++) {
			isEmptyCell = field[0][i] == 0;
			for (int j = 1; j < State.ROWS; j++) {
				if (isEmptyCell && field[j][i] != 0) {
					tr++;
				}

				if (!isEmptyCell && field[j][i] == 0) {
					tr++;
				}

				isEmptyCell = field[j][i] == 0;
			}
		}

		return tr;

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
			lost = true;
			return new SerializedState(field, top, followingPiece, turn + 1, s.pieceHistory, true, cleared);
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

		return new SerializedState(field, top, turn + 1, followingPiece, s.pieceHistory, lost, cleared);
	}

	public static void main(String[] args) {
		State s = new State();
		new TFrame(s);
		PlayerSkeleton p = new PlayerSkeleton();
		while(!s.hasLost()) {
			s.makeMove(p.pickMove(s,s.legalMoves(),DEFAULT_WEIGHTS));
			s.draw();
			s.drawNext(0,0);
			System.out.println("You have completed "+s.getRowsCleared()+" rows.");
//			try {
//				Thread.sleep(300);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
		}
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}

}

//class SerializedState {
//	final int[][] field;
//	final int[] top;
//	final int turn;
//	final int nextPiece;
//	final boolean lost;
//	final int cleared;
//	final int[] pieceHistory;
//
//	SerializedState() {
//		this(new int[State.ROWS][State.COLS],
//				new int[State.COLS],
//				0,
//				randomPiece(),
//				new int[State.N_PIECES],
//				false,
//				0
//				);
//	}
//
//	SerializedState(int[][] field, int[] top, int turn, int nextPiece, int[] pieceHistory, boolean lost, int cleared) {
//		this.field = field;
//		this.top = top;
//		this.turn = turn;
//		this.nextPiece = nextPiece;
//		this.lost = lost;
//		this.cleared = cleared;
//		this.pieceHistory = pieceHistory;
//	}
//
//	SerializedState(State s,  int[] pieceHistory) {
//		this(s.getField(), s.getTop(), s.getTurnNumber(), s.getNextPiece(), pieceHistory, s.lost, s.getRowsCleared());
//	}
//
//	public static int randomPiece() {
//		return (int) (Math.random() * State.N_PIECES);
//	}
//}

