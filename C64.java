import java.io.PrintStream;

import javax.swing.JFrame;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class C64 {

	Square[][] board = new Square[8][8];
	long numSols = 0;
	long crono;
	int min, minMin;
	int iIni, jIni;
	int statsSecs;
	long lastLoopSolution = 0;
	long numLoops = 0;
	long th1;
	int[] neighborSequence = null;
	boolean resuming = false;
	PrintStream solutionsFile, logFile;
	long acumCPUTime = 0;
	ThreadMXBean mx;

	public static void main(String[] a) {
		int i = Integer.parseInt(a[0]);
		int j = Integer.parseInt(a[1]);
		int s = Integer.parseInt(a[2]);

		new C64(i, j, s, System.out, System.out).run(0);
	}

	public C64(int i, int j, int s, PrintStream solutionsFile, PrintStream logFile) {
		iIni = i;
		jIni = j;
		statsSecs = s;
		this.solutionsFile = solutionsFile;
		this.logFile = logFile;

		for (int x = 0; x < 64; x++)
			board[x / 8][x % 8] = new Square(x / 8, x % 8);
		for (int x = 0; x < 64; x++)
			board[x / 8][x % 8].addNeighbors(board);

		min = minMin = 64;
	}

	public void setNeighborSequence(int[] s) {
		neighborSequence = s;
	}

	public void run(long elapsedTime) {
		th1 = Thread.currentThread().getId();
		mx = ManagementFactory.getThreadMXBean();

		crono = System.currentTimeMillis()-elapsedTime;
		if (statsSecs > 0)
			new Thread() {
				public void run() {
					long lastStats = System.currentTimeMillis();
					while (true) {
						while (System.currentTimeMillis() - lastStats < statsSecs * 1000 - 500)
							try {
								sleep(1000);
							} catch (InterruptedException e) {
							}
						showStats();
						lastStats=System.currentTimeMillis();
					}
				}
			}.start();

		board[iIni][jIni].setVal(1);
		nextCell(board[iIni][jIni]);
	}

	private void nextCell(Square sq) {
		sq.setFreeNeighbors();
		int nextVal = sq.getVal() + 1;

		if (neighborSequence != null)
			sq.setNextNeighbor(neighborSequence[sq.getVal()]);

		Square nb;
		while ((nb = sq.nextNeighbor()) != null) {
			nb.setVal(nextVal);
			if (nb.getVal() < 64)
				nextCell(nb);
			else {
				showBoard(nb);
			}
			neighborSequence = null;
			min = Math.min(min, sq.getVal());
			nb.setVal(0);
		}
	}

	public synchronized double[] getProgress() {
		Square[] sequence = new Square[65];
		double[] progress = new double[128];

		int maxPos = 0;
		for (Square[] row : board)
			for (Square sq : row) {
				sequence[sq.getVal()] = sq;
				maxPos = Math.max(maxPos, sq.getVal());
			}

		for (int i = Math.min(40, maxPos) - 1; i > 0; i--) {

			progress[2 * i] = sequence[i].getPartialProgress()
					+ progress[2 * (i + 1)] / sequence[i].getNumFreeNeighbors();
			progress[2 * i + 1] = sequence[i].getNumFreeNeighbors();
		}
		return progress;
	}

	private synchronized void showStats() {
		double[] p = getProgress();

		logFile.println(" * stats " + millisToTime(System.currentTimeMillis() - crono) + " *");
		logFile.println("cpu time: " + millisToTime(getCPUTime() / 1000000));
		logFile.println("last loop: "
				+ (lastLoopSolution == 0 ? "not yet found" : lastLoopSolution + " | " + numLoops + " loops found"));

		int i = 1;
		while (i < 35 && p[i] < .000001)
			i++;
		while (i <= 30) {
			logFile.println(String.format("* %2d: " + (p[2 * i] > 0.000005 ? "%.5f" : "%4.1e"), i, p[2 * i]));
			i++;
		}
		logFile.println(" * * * * * * * * * * * *");
	}

	public long getCPUTime() {
		return acumCPUTime + mx.getThreadCpuTime(th1);
	}

	private synchronized void showBoard(Square nb) {
		boolean loop = nb.checkLoop();
		solutionsFile.println("solution " + ++numSols + (loop ? " loop" : " no loop") + " min:" + min + "/" + minMin);
		solutionsFile.println("cpu/elapsed time: " + millisToTime(getCPUTime() / 1000000) + "/"
				+ millisToTime(System.currentTimeMillis() - crono));

		if (loop) {
			lastLoopSolution = numSols;
			numLoops++;
		}

		minMin = Math.min(min, minMin);
		min = 64;

		for (Square[] row : board) {
			for (Square n : row)
				solutionsFile.print((n.getVal() < 10 ? "  " : " ") + n.getVal());

			solutionsFile.println();
		}
		// solutionsFile.println("encoding: " + encodeBoard());
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
	public synchronized int[] getNeighborSequence() {
		int[] neighborSequence = new int[65];
		// double x = 0;

		for (Square[] row : board)
			for (Square sq : row) {
				neighborSequence[sq.getVal()] = sq.getActualJump();
				// if (sq.getVal() != 64)
				// x += Math.log(sq.getNumFreeNeighbors());
			}
		// solutionsFile.println(x / Math.log(2));
		/*
		 * String encoding = ""; for (int i = 1; i < 64; i++) encoding +=
		 * neighborSequence[i];
		 */
		return neighborSequence;
	}
}
