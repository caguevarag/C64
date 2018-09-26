
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.Scanner;

public class C64Server {
    static C64 model;
    static boolean serverRunning;
    static int saveStateTime = 1000;
    static String stateFileName;
    static PrintStream logFile, solutionsFile;
    static boolean resuming;
    static long elapsedTime;

    /**
     * Application method to run the server runs in an infinite loop listening When
     * a connection is requested, it spawns a new thread to do the servicing and
     * immediately returns to listening. The server keeps a unique client number for
     * each client that connects just to show interesting logging messages. It is
     * certainly not necessary to do this.
     */
    public static void main(String[] a) throws Exception {
        if (a.length == 0) {
            // TODO check main arguments
            System.out.println("Please specify the port number");
            return;
        }
        int port = Integer.parseInt(a[0]);
        int i = Integer.parseInt(a[1]);
        int j = Integer.parseInt(a[2]);
        int tStats = Integer.parseInt(a[3]);

        PrintStream logFile = new PrintStream(new FileOutputStream(i + "" + j + ".log", true));
        PrintStream solutionsFile = new PrintStream(new FileOutputStream(i + "" + j + ".txt", true));
        stateFileName = i + "" + j + ".state";

        model = new C64(i, j, tStats, solutionsFile, logFile);
        resuming = new File(stateFileName).exists();

        new Thread() {
            public void run() {
                if (resuming)
                    setResumeData();

                model.run(elapsedTime);
            }
        }.start();

        new Thread() {
            public void run() {
                try {
                    while (true) {
                        Thread.sleep(saveStateTime);
                        saveState();
                    }
                } catch (Exception e) {
                    System.err.println("Error in saveState Thread");
                }
            }
        }.start();

        serverRunning = true;
        System.out.println("The server is running.");
        int clientNumber = 0;
        ServerSocket listener = new ServerSocket(port);
        try {
            while (true) {
                new C64ClientHandler(listener.accept(), clientNumber++).start();
            }
        } finally {
            listener.close();
        }
    }

    private static void setResumeData() {
        try {
            Scanner scan = new Scanner(new File(stateFileName));

            model.numSols = scan.nextLong();
            model.numLoops = scan.nextLong();
            model.lastLoopSolution = scan.nextLong();
            elapsedTime = scan.nextLong();
            model.acumCPUTime = scan.nextLong();
            model.minMin = scan.nextInt();

            int[] sequence = new int[65];
            for (int i = 1; i < 64; i++) {
                int j = scan.nextInt();
                if (j != -1)
                    sequence[i] = j;
                else
                    break;
            }

            model.setNeighborSequence(sequence);

            scan.close();
        } catch (Exception e) {
        }
    }

    private synchronized static boolean saveState() {
        try {
            FileWriter writer = new FileWriter(stateFileName, false);

            writer.write(model.numSols + "\n");
            writer.write(model.numLoops + "\n");
            writer.write(model.lastLoopSolution + "\n");
            writer.write((System.currentTimeMillis() - model.crono) + "\n");
            writer.write(model.getCPUTime() + "\n");
            writer.write(model.minMin + "\n");

            int[] n = model.getNeighborSequence();
            for (int i = 1; i < 64; i++)
                writer.write(n[i] + " ");

            writer.append("\n");
            writer.close();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static class C64ClientHandler extends Thread {
        private Socket socket;
        private int clientNumber;

        public C64ClientHandler(Socket socket, int clientNumber) {
            this.socket = socket;
            this.clientNumber = clientNumber;
            log("New connection with client# " + clientNumber + " at " + socket);
        }

        public void run() {
            try {
                // Decorate the streams so we can send characters
                // and not just bytes. Ensure output is flushed
                // after every newline.
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                // Send a welcome message to the client.
                out.println("Hello, you are client #" + clientNumber + ".");

                while (true) {
                    String input = in.readLine();

                    switch (input) {
                    case "endServer":
                    case ".":
                        // serverRunning = false;
                        out.println("< not yet implemented >");
                        break;
                    case "getProgress":
                    case "gp":
                        double[] p = model.getProgress();
                        String s = "";
                        for (int i = 1; i < 40; i++)
                            s += String.format("%2d," + (p[2 * i] > 0.000005 ? "%.5f" : "%4.1e") + ",%2d|", i, p[2 * i],
                                    (int) p[2 * i + 1]);
                        out.println(s.substring(0, s.length() - 1));
                        break;
                    case "getLastBoard":
                    case "glb":
                        out.println("< not yet implemented >");
                        break;
                    case "getLastLoop":
                    case "gll":
                        out.println("lastLoop: " + model.lastLoopSolution);
                        break;
                    case "getNumSolutions":
                    case "gns":
                        out.println("number of solutions found: " + model.numSols);
                        break;
                    case "getNumLoops":
                    case "gnl":
                        out.println("number of loops found: " + model.numLoops);
                        break;
                    case "help":
                    case "h":
                        out.println("available commands  | shortcut\n" + "  endServer         |   .     \n"
                                + "  exit              |   x     \n" + "  getLastBoard      |   glb   \n"
                                + "  getNumLoops       |   gnl   \n" + "  getLastLoop       |   gll   \n"
                                + "  getNumSolutions   |   gns   \n" + "  getProgress       |   gp    \n"
                                + "  help              |   h       ");
                        break;
                    case "":
                        break;
                    default:
                        out.println(input + ": command not found");
                    }
                }
            } catch (IOException e) {
                log("Error handling client# " + clientNumber + ": " + e);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    log("Couldn't close a socket, what's going on?");
                }
                log("Connection with client #" + clientNumber + " closed");
            }
        }

        /**
         * Logs a simple message. In this case we just write the message to the server
         * applications standard output.
         */
        private void log(String message) {
            System.out.println(message);
        }
    }
}