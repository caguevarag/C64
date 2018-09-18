
public class C64Iter {
    static final int N = 8;
    static int[][] jumps = { { -2, -1 }, { -2, 1 }, { -1, -2 }, { -1, 2 }, { 1, -2 }, { 1, 2 }, { 2, -1 }, { 2, 1 } };
    static Square[][] board;
    static Square[] sequence;
    static int pos, numSols;;
    static long t;

    public static void main(String[] a) {
        init();

        t = System.currentTimeMillis();
        play(0, 0);
        while (pos > 0) {
            if (sequence[pos].jump < 7) {
                sequence[pos].jump++;
                int[] jump = jumps[sequence[pos].jump];
                int i = sequence[pos].i + jump[0];
                int j = sequence[pos].j + jump[1];
                if (i >= 0 && i < N && j >= 0 && j < N && board[i][j].val == 0)
                    play(i, j);
            } else
                unplay();

            if (pos == 64) {
                showBoard();
                unplay();
            }
        }
    }

    private static void init() {
        board = new Square[N][N];
        sequence = new Square[N * N + 1];
        pos = numSols = 0;
        for (int i = 0; i < 64; i++)
            board[i / 8][i % 8] = new Square(i / 8, i % 8);

        new Thread() {
            public void run() {
                while (true) {
                    showStats();
                    try {
                        Thread.sleep(10000);
                    } catch (Exception e) {
                    }
                }
            }
        }.start();
    }

    private static void play(int i, int j) {
        pos++;
        sequence[pos] = board[i][j];
        board[i][j].val = pos;
        board[i][j].jump = -1;
    }

    private static void unplay() {
        sequence[pos].val = 0;
        pos--;
    }

    private synchronized static void showBoard() {
        System.out.println(" " + (++numSols) + "  " + (System.currentTimeMillis() - t) + " ms");
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++)
                System.out.print(String.format("%3d", board[i][j].val));
            System.out.println();
        }
    }

    private synchronized static void showStats() {
        double[] progress = new double[N * N];
        for (int i = pos - 1; i > 0; i--)
            progress[i] = (sequence[i].jump + progress[i + 1]) / N;

        System.out.println("****** " + msToTime(System.currentTimeMillis() - t) + " ******");

        int i = 1;
        while (progress[i] < .001)
            i++;
        while (i < 40)
            System.out.println(String.format("%2d: %.5f", i++, progress[i]));
        System.out.println("**********************");
    }

    private static String msToTime(long ms) {
        int h = (int) ms / 3600000;
        ms -= h * 3600000;
        int m = (int) ms / 60000;
        ms -= h * 60000;
        int s = (int) ms / 1000;
        return (h < 10 ? "0" : "") + h + (m < 10 ? ":0" : ":") + m + (s < 10 ? ":0" : ":") + s;
    }

    static class Square {
        final int i, j;
        int val, jump;

        public Square(int i, int j) {
            this.i = i;
            this.j = j;

            jump = -1;
            val = 0;
        }
    }
}