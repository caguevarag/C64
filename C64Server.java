
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class C64Server {
    static C64 model;
    static boolean serverRunning;

    /**
     * Application method to run the server runs in an infinite loop listening on
     * port 9898. When a connection is requested, it spawns a new thread to do the
     * servicing and immediately returns to listening. The server keeps a unique
     * client number for each client that connects just to show interesting logging
     * messages. It is certainly not necessary to do this.
     */
    public static void main(String[] a) throws Exception {
        if (a.length == 0) {
            System.out.println("Please specify the port number");
            return;
        }
        int port = Integer.parseInt(a[0]);
        int i = Integer.parseInt(a[1]);
        int j = Integer.parseInt(a[2]);

        model = new C64(i, j, 0);
        new Thread() {
            public void run() {
                model.run();
            }
        }.start();

        serverRunning = true;
        System.out.println("The server is running.");
        int clientNumber = 0;
        ServerSocket listener = new ServerSocket(port);
        try {
            while (true) {
                new Capitalizer(listener.accept(), clientNumber++).start();
            }
        } finally {
            listener.close();
        }
    }

    /**
     * A private thread to handle capitalization requests on a particular socket.
     * The client terminates the dialogue by sending a single line containing only a
     * period.
     */
    private static class Capitalizer extends Thread {
        private Socket socket;
        private int clientNumber;

        public Capitalizer(Socket socket, int clientNumber) {
            this.socket = socket;
            this.clientNumber = clientNumber;
            log("New connection with client# " + clientNumber + " at " + socket);
        }

        /**
         * Services this thread's client by first sending the client a welcome message
         * then repeatedly reading strings and sending back the capitalized version of
         * the string.
         */
        public void run() {
            try {

                // Decorate the streams so we can send characters
                // and not just bytes. Ensure output is flushed
                // after every newline.
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                // Send a welcome message to the client.
                out.println("Hello, you are client #" + clientNumber + ".");

                while (serverRunning) {
                    String input = in.readLine();

                    switch (input) {
                    case "end":
                        serverRunning = false;
                        break;
                    case "getProgress":
                        double[] p = model.getProgress();
                        String s = "progress: ";
                        for (int i = 0; i < 64; i++)
                            s += p[i] + ",";
                        out.println(s);
                        break;
                    case "getLastBoard":
                        out.println("< not yet implemented >");
                        break;
                    case "getLastLoop":
                        out.println("lastLoop: " + model.lastLoopSolution);
                        break;
                    case "getNumSolutions":
                        out.println("number of solutions found: " + model.numSols);
                        break;
                    case "getNumLoops":
                        out.println("number of loops found: " + model.numLoops);
                        break;
                    case "help":
                        out.println(
                                "available commands:\n end\n getLastBoard\n getNumLoops\n getLastLoop\n getNumSolutions\n getProgress");
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
                log("Connection with client# " + clientNumber + " closed");
                
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