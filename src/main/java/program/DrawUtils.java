package program;

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.Color;
import java.awt.Graphics;

public class DrawUtils {
    static void showImage(int[][] pixels) throws InterruptedException {
        int height = pixels.length;
        int width = pixels[0].length;

        JFrame frame = new JFrame("preview");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(pixels[0].length + 100, pixels.length + 100);
        frame.setVisible(true);
        Graphics g = frame.getGraphics();
        Thread.sleep(100);
        g.fillRect(0, 0, width, height);

        g.setColor(Color.WHITE);
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (pixels[row][col] == 255) {
                    g.drawLine(col + 20, row + 60, col + 20, row + 60);
                }
            }
        }
    }
}
