
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.sql.rowset.serial.SerialArray;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * A simple Swing-based client for the capitalization server. It has a main
 * frame window with a text field for entering strings and a textarea to see the
 * results of capitalizing them.
 */
public class C64Client extends JFrame {

    private static final long serialVersionUID = 1L;
    private BufferedReader in;
    private PrintWriter out;
    private JFrame frame = new JFrame("Capitalize Client");
    private JTextField dataField = new JTextField(40);
    private JTextArea messageArea = new JTextArea(8, 60);
    private Image im;
    private long frameTimes[] = new long[100];
    private long crono;
    private int timePos = 0;

    public void showGraph() {
        setBounds(150, 100, 1000, 800);
        im = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        setVisible(true);
        crono = System.currentTimeMillis();

        new Thread() {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(30);
                        repaint();
                    } catch (InterruptedException e) {
                        System.err.println("error");
                    }
                }
            }
        }.start();
    }

    public void paint(Graphics g) {
        // System.out.println("painting");
        Graphics2D g2 = (Graphics2D) im.getGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);

        g2.setBackground(Color.white);
        g2.clearRect(0, 0, im.getWidth(this), im.getHeight(this));

        Point o = new Point(100, 750);
        int w = 25;
        int h = 700;

        double[] progress = getProgress();
        for (int i = 0; i < 32; i++) {
            int d = (int) (h * progress[i + 1]);
            int alpha = (int) (256 * (h * progress[i + 1] - d));

            g2.setColor(new Color(255, 0, 0, alpha));
            g2.drawLine(o.x + i * w, o.y - d - 1, o.x + i * w + w - 2, o.y - d - 1);
            g2.setColor(new Color(255, 0, 0));
            g2.fillRect(o.x + i * w, o.y - d, w - 1, 3);
            g2.setColor(new Color(255, 0, 0, 255 - alpha));
            g2.drawLine(o.x + i * w, o.y - d + 3, o.x + i * w + w - 2, o.y - d + 3);

            g2.setColor(Color.BLACK);
            g2.drawString((i + 1) + "", o.x + i * w + 5, o.y + 15);
        }
        frameTimes[timePos] = System.currentTimeMillis() - crono;
        double fps = frameTimes.length * 1000. / (frameTimes[timePos] - frameTimes[(timePos + 1) % frameTimes.length]);
        g2.drawString(String.format("fps: %.1f", fps), 40, 40);
        timePos = (timePos + 1) % frameTimes.length;

        g.drawImage(im, 0, 0, this);
    }

    double[] getProgress() {
        double[] p = new double[64];

        out.println("getProgress");
        String response;
        try {
            response = in.readLine();
            String[] s = response.split("[ ,]");

            for (int i = 0; i < 64; i++)
                p[i] = Double.parseDouble(s[i + 1]);
        } catch (IOException ex) {
        }
        return p;
    }

    /**
     * Constructs the client by laying out the GUI and registering a listener with
     * the textfield so that pressing Enter in the listener sends the textfield
     * contents to the server.
     */
    public C64Client() {
        // Layout GUI
        messageArea.setEditable(false);
        frame.getContentPane().add(dataField, "North");
        frame.getContentPane().add(new JScrollPane(messageArea), "Center");

        // Add Listeners
        dataField.addActionListener(new ActionListener() {
            /**
             * Responds to pressing the enter key in the textfield by sending the contents
             * of the text field to the server and displaying the response from the server
             * in the text area. If the response is "." we exit the whole application, which
             * closes all sockets, streams and windows.
             */
            public void actionPerformed(ActionEvent e) {
                out.println(dataField.getText());
                String response;
                try {
                    response = in.readLine();
                    if (response == null || response.equals("")) {
                        System.exit(0);
                    }
                } catch (IOException ex) {
                    response = "Error: " + ex;
                }
                messageArea.append(response + "\n");
                dataField.selectAll();
            }
        });
    }

    /**
     * Implements the connection logic by prompting the end user for the server's IP
     * address, connecting, setting up streams, and consuming the welcome messages
     * from the server. The Capitalizer protocol says that the server sends three
     * lines of text to the client immediately after establishing a connection.
     */
    public void connectToServer() throws IOException {

        // Get the server address from a dialog box.
        String serverAddress = JOptionPane.showInputDialog(frame, "Enter IPAddress:port of the Server:",
                "Welcome to the C64 Client", JOptionPane.QUESTION_MESSAGE);

        // Make connection and initialize streams
        String[] s = serverAddress.split(":");
        Socket socket = new Socket(s[0], Integer.parseInt(s[1]));
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Consume the initial welcoming message from the server
        messageArea.append(in.readLine() + "\n");
    }

    /**
     * Runs the client application.
     */
    public static void main(String[] args) throws Exception {
        C64Client client = new C64Client();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.pack();
        client.frame.setVisible(true);
        client.connectToServer();

        client.showGraph();
    }
}