package program;

public class Const {

    static final int ZERO = 0x00000000;
    static final int ONE = 0xffffffff;
    static final int VISITED = 0x00000004;
    static final int TO_BE_VISITED = 0x00000005;

    static final int PADDING = 20;

    static final int SLOPE_COMPUTE_LENGTH = 20;

    static final int[][] dirs = new int[][]{{-1, 0}, {-1, 1}, {0, 1}, {1, 1}, {1, 0}, {1, -1}, {0, -1}, {-1, -1}};
}
