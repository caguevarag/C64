import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.Point;

import javax.swing.JFrame;

public class C64Gui extends JFrame {
    private static final long serialVersionUID = 1L;

    static C64 model;
    Image im;

    public static void main(String[] a) {
        model = new C64(0, 0, 0);

        new C64Gui();
        model.run();
    }

    public C64Gui() {
        setBounds(150, 100, 1000, 800);
        im = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        setVisible(true);

        new Thread() {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(50);
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

        g2.setBackground(Color.white);
        g2.clearRect(0, 0, im.getWidth(this), im.getHeight(this));

        Point o = new Point(100, 750);
        int w = 25;
        int h = 700;

        double[] progress = model.getProgress();
        for (int i = 0; i < 32; i++) {
            g2.setColor(Color.RED);
            g2.fillRect(o.x + i * w, o.y - (int) (h * progress[i + 1]), w - 1, 4);
            g2.setColor(Color.BLACK);
            g2.drawString((i + 1) + "", o.x + i * w+5, o.y+15);
        }
        g.drawImage(im, 0, 0, this);
    }
}