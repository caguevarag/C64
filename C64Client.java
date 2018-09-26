
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class C64Client extends JFrame {
    private static final long serialVersionUID = 1L;
    private static BufferedReader in;
    private static PrintWriter out;
    private Socket socket;

    private Image im;
    private long frameTimes[] = new long[25];
    private long crono;
    private int timePos = 0;

    private static String host = null;
    private static int port;

    public void showGraph() {
        setBounds(150, 100, 680, 570);
        im = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        setVisible(true);
        crono = System.currentTimeMillis();

        new Thread() {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(50);
                        repaint();
                    } catch (Exception e) {
                        System.err.println("error in repaint thread");
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

        Point o = new Point(20, 550);
        int w = 20;
        int h = 500;

        double[] progress = getProgress();
        for (int i = 0; i < 32; i++) {
            // int d = (int) (h * progress[2 * (i + 1)]);
            double dd = h * progress[2 * (i + 1)];
            // int alpha = (int) (256 * (h * progress[2 * (i + 1)] - d));

            g2.setColor(Color.gray);
            g2.drawLine(o.x + i * w + w / 2 - 1, o.y, o.x + i * w + w / 2 - 1, o.y - h);
            for (int j = 0; j <= progress[2 * (i + 1) + 1]; j++) {
                int h2 = (int) (o.y - j * h / progress[2 * (i + 1) + 1]);
                g2.drawLine(o.x + i * w + w / 2 - 7, h2, o.x + i * w + w / 2 + 5, h2);
            }
            g2.setColor(new Color(255, 0, 0, 180));
            g2.fill(new Rectangle2D.Double(o.x + i * w, o.y - dd, w - 1, 5));

            g2.setColor(Color.BLACK);
            g2.drawString((i + 1) + "", o.x + i * w + 5, o.y + 15);
        }
        frameTimes[timePos] = System.currentTimeMillis() - crono;
        double fps = frameTimes.length * 1000. / (frameTimes[timePos] - frameTimes[(timePos + 1) % frameTimes.length]);
        g2.drawString(String.format("fps: %.1f", (Object) fps), 40, 40);
        timePos = (timePos + 1) % frameTimes.length;

        g.drawImage(im, 0, 0, this);
    }

    double[] getProgress() {
        double[] p = new double[128];
        String response = null;

        try {
            while (response == null) {
                out.println("getProgress");
                response = in.readLine();
                if (response == null)
                    connectToServer();
            }

            String[] s = response.split("[|,]");

            for (int i = 1; i < s.length / 3; i++) {
                p[2 * i] = Double.parseDouble(s[3 * i - 2]);
                p[2 * i + 1] = (double) Integer.parseInt(s[3 * i - 1].trim());
            }
        } catch (Exception ex) {
            System.err.println("Exception in getProgress(): ");
            ex.printStackTrace();
        }
        return p;
    }

    public void connectToServer() throws IOException {

        // Get the server address from a dialog box.
        if (host == null) {
            String serverAddress = JOptionPane.showInputDialog(this, "Enter IPAddress:port of the Server:",
                    "Welcome to the C64 Client", JOptionPane.QUESTION_MESSAGE);

            String[] s = serverAddress.split(":");
            host = s[0];
            port = Integer.parseInt(s[1]);
        }
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Consume the initial welcoming message from the server
        System.out.println(in.readLine());
    }

    public static void main(String[] args) throws Exception {
        C64Client client = new C64Client();

        if (args.length == 2) {
            host = args[0];
            port = Integer.parseInt(args[1]);
        }
        client.connectToServer();
        client.showGraph();

        Scanner userInput = new Scanner(System.in);

        boolean running = true;
        while (running) {
            String input = userInput.next();
            if (input.equals("exit") || input.equals("x"))
                running = false;
            else {
                out.println(input);

                System.out.println(in.readLine());
                while (in.ready())
                    System.out.println(in.readLine());
            }
        }
        userInput.close();
    }
}