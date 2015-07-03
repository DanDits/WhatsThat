package dan.dit.whatsthat.util.mosaic.reconstruction;

/**
 * An enumeration containing constants representing a neighbor
 * of a {@link MosaicFragment}. This includes the 4 neighbors on the
 * side and the 4 neighbors in the corners.
 * @author Daniel
 *
 */
public enum FragmentNeighbor {
	
	/**
	 * The left neighbor.
	 */
	LEFT(0, -1),
	
	/**
	 * The upper neighbor.
	 */
	UP(-1, 0),
	
	/**
	 * The right neighbor.
	 */
	RIGHT(0, 1),
	
	/**
	 * The lower neighbor.
	 */
	DOWN(1, 0),
	
	/**
	 * The top left neighbor.
	 */
	TOP_LEFT(-1, -1), 
	
	/**
	 * The top right neighbor.
	 */
	TOP_RIGHT(-1, 1),
	
	/**
	 * The bottom left neighbor.
	 */
	BOTTOM_LEFT(1, -1),
	
	/**
	 * The bottom right neigbor.
	 */
	BOTTOM_RIGHT(1, 1);
	
	/**
	 * Returns the amount of neighbors, which is the amount of constants of this enum, 9.
	 */
	public static final int COUNT = FragmentNeighbor.values().length;

	/**
	 * An array containing only the corners in the order:
	 * TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT.
	 */
	public static final FragmentNeighbor[] CORNERS 
		= new FragmentNeighbor[] {TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT};
	
	/**
	 * An array containing only the sides in the order:
	 * UP, RIGHT, DOWN, LEFT.
	 */
	public static final FragmentNeighbor[] SIDES 
		= new FragmentNeighbor[] {UP, RIGHT, DOWN, LEFT};

	private int rowDelta;
	private int columnDelta;
	
	private FragmentNeighbor(int rowDelta, int columnDelta) {	
		this.rowDelta = rowDelta;
		this.columnDelta = columnDelta;
	}
	
	/**
	 * Returns the row delta for this FragmentNeighbor. For example
	 * the TOP_LEFT neighbor has -1.
	 * @return The row delta for this FragmentNeighbor.
	 */
	public int getRowDelta() {
		return rowDelta;
	}

	/**
	 * Returns the column delta for this FragmentNeighbor. For example
	 * the TOP neighbor has 0 and the RIGHT neighbor has 1.
	 * @return The column delta for this FragmentNeighbor.
	 */
	public int getColumnDelta() {
		return columnDelta;
	}

	/**
	 * Returns the partner of this FragmentNeighbor, which is
	 * the opposite neighbor.<br>
	 * Partners:<ul><li>BOTTOM_LEFT and TOP_RIGHT</li><li>BOTTOM_RIGHT and
	 * TOP_LEFT</li><li>DOWN and UP</li><li>RIGHT and LEFT</li></ul>
	 * @return The opposite Neighbor.
	 */
	public FragmentNeighbor getPartner() {
		switch (this) {
		case BOTTOM_LEFT:
			return TOP_RIGHT;
		case BOTTOM_RIGHT:
			return TOP_LEFT;
		case DOWN:
			return UP;
		case LEFT:
			return RIGHT;
		case RIGHT:
			return LEFT;
		case TOP_LEFT:
			return BOTTOM_RIGHT;
		case TOP_RIGHT:
			return BOTTOM_LEFT;
		case UP:
			return DOWN;
		default:
			return null;
		}
	}
}
