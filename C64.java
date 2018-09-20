import javax.swing.JFrame;

import com.sun.management.ThreadMXBean;

public class C64 {

	Square[][] board = new Square[8][8];
	long numSols = 0;
	long crono;
	int min, minMin;
	int iIni, jIni;
	int statsSecs;
	long lastLoopSolution = 0;
	int numLoops = 0;
	long th1, th2;

	public static void main(String[] a) {
		int i = Integer.parseInt(a[0]);
		int j = Integer.parseInt(a[1]);
		int s = Integer.parseInt(a[2]);

		new C64(i, j, s).run();
	}

	public C64(int i, int j, int s) {
		iIni = i;
		jIni = j;
		statsSecs = s;

		for (int x = 0; x < 64; x++)
			board[x / 8][x % 8] = new Square(x / 8, x % 8);
		for (int x = 0; x < 64; x++)
			board[x / 8][x % 8].addNeighbors(board);

		min = minMin = 64;
	}

	public void run() {
		th1 = Thread.currentThread().getId();
		System.out.println("thread: " + th1);

		crono = System.currentTimeMillis();
		if (statsSecs > 0)
			new Thread() {
				public void run() {
					while (true) {
						long t = System.currentTimeMillis() - crono;
						try {
							sleep(statsSecs * 1000 - t % 1000);
						} catch (InterruptedException e) {
						}
						showStats();
					}
				}
			}.start();

		board[iIni][jIni].setVal(1);
		nextCell(board[iIni][jIni]);
	}

	private void nextCell(Square sq) {
		sq.setFreeNeighbors();
		int nextVal = sq.getVal() + 1;

		Square nb;
		while ((nb = sq.nextNeighbor()) != null) {
			nb.setVal(nextVal);
			if (nb.getVal() < 64)
				nextCell(nb);
			else {
				showBoard();
				if (nb.checkLoop()) {
					// showBoard();
					lastLoopSolution = numSols;
					numLoops++;
				}
			}
			min = Math.min(min, sq.getVal());
			nb.setVal(0);
		}
	}

	public synchronized double[] getProgress() {
		Square[] sequence = new Square[65];
		double[] progress = new double[64];

		int maxPos = 0;
		for (Square[] row : board)
			for (Square sq : row) {
				sequence[sq.getVal()] = sq;
				maxPos = Math.max(maxPos, sq.getVal());
			}
		//System.out.println("maxPos: " + maxPos);

		for (int i = Math.min(35, maxPos) - 1; i > 0; i--)
			progress[i] = sequence[i].getPartialProgress() + progress[i + 1] / sequence[i].getNumFreeNeighbors();
		return progress;
	}

	private synchronized void showStats() {
		double[] progress = getProgress();

		System.out.println(" * stats " + millisToTime(System.currentTimeMillis() - crono) + " *");
		java.lang.management.ThreadMXBean mx = java.lang.management.ManagementFactory.getThreadMXBean();
		System.out.println(mx.getThreadCpuTime(th1) / 1000000. + "," + mx.getThreadUserTime(th1) / 1000000.);
		System.out.println(mx.getThreadCpuTime(th2) / 1000000. + "," + mx.getThreadUserTime(th2) / 1000000.);
		System.out.println("loop: solution "
				+ (lastLoopSolution == 0 ? "not found" : lastLoopSolution + " | " + numLoops + " encontrados"));

		int i = 1;
		while (i < 35 && progress[i] < .000001)
			i++;
		while (i <= 30) {
			System.out.println(String.format(" * %2d: %.6f", i, progress[i]));
			i++;
		}
		System.out.println(" * * * * * * * * * * * *");
	}

	private synchronized void showBoard() {
		System.out.println(
				++numSols + " min:" + min + "/" + minMin + " " + millisToTime(System.currentTimeMillis() - crono));
		minMin = Math.min(min, minMin);
		min = 64;

		for (Square[] row : board) {
			for (Square n : row)
				System.out.print((n.getVal() < 10 ? "  " : " ") + n.getVal());

			System.out.println();
		}
		/*
		int[] encoding = encodeBoard();
		for (int i = 1; i < encoding.length; i++)
			System.out.print(encoding[i]);
		System.out.println();
		*/
	}

	private String millisToTime(long t) {
		long h = t / 3600000;
		t -= h * 3600000;
		long m = t / 60000;
		t -= m * 60000;

		return String.format("%3d", h) + ":" + (m < 10 ? "0" : "") + m + ":" + (t < 10000 ? "0" : "")
				+ String.format("%.3f", t / 1000.);
	}

	// the board must be complete
	public int[] encodeBoard() {
		int[] jumpSequence = new int[65];
		double x = 0;

		for (Square[] row : board)
			for (Square sq : row) {
				jumpSequence[sq.getVal()] = sq.getActualJump();
				if (sq.getVal() != 64)
					x += Math.log(sq.getNumFreeNeighbors());
			}
		System.out.println(x/Math.log(2));

		return jumpSequence;
	}
}
