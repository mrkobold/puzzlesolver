package program;

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.Color;
import java.awt.Graphics;
import java.util.Arrays;

import static program.Const.ONE;
import static program.Const.SLOPE_COMPUTE_LENGTH;
import static program.Const.dirs;

class Piece {
    int[][] corners = new int[4][2];
    int height, width;
    double[] slopes;

    int[][] img;
    int[][] img_walk;

    Piece(int[][] img) {
        this.img = img;
        height = img.length;
        width = img[0].length;
    }

    void draw_curves() throws Exception {
        Graphics g = createFrame("Curves");
    }

    void draw_slopes() throws InterruptedException {
        Graphics g = createFrame("Slopes");

        for (int i = 0; i < img_walk.length; i++) {
            double slope = slopes[i];

            int blue;
            int green;
            int red = 0;

            int add = (int) (Math.atan(slope) / (Math.PI * 2) * 125) + 125;
            green = add;
            blue = 255 - add;

            if (green > 240) {
                System.out.println();
            }

            g.setColor(new Color(red, green, blue));
            g.drawOval(img_walk[i][1], img_walk[i][0], 1, 1);

            int l = width / 10;
            int steps = width / 8;
            if (i % steps == 0) {
                int y_c = img_walk[i][0];
                int x_c = img_walk[i][1];

                int d_x = (int) (Math.sqrt((l * l) / (1 + slope * slope)));
                int d_y = (int) (d_x * slope);
                g.drawLine(x_c - d_x, y_c - d_y, x_c + d_x, y_c + d_y);
            }
        }
    }

    void compute_slopes() {
        int n = img_walk.length;
        for (int i = 0; i < n; i++) {
            int[] before = img_walk[(i - SLOPE_COMPUTE_LENGTH / 2 + n) % n];
            int[] after = img_walk[(i + SLOPE_COMPUTE_LENGTH / 2) % n];
            slopes[i] = ((double) (after[0] - before[0])) / ((double) (after[1] - before[1]));
        }
    }

    void walk() {
        // will hold the starting point of the walk around perimeter
        int s_y = -1, s_x = -1;
        boolean found_s = false;
        for (int y = 0; y < height && !found_s; y++) {
            for (int x = 0; x < width && !found_s; x++) {
                if (img[y][x] == ONE) {
                    s_y = y;
                    s_x = x;
                    found_s = true;
                }
            }
        }

        // walk around the shape starting at (s_y, s_x)
        int p_y = s_y, p_x = s_x; // previous pixel

        int dir = 0;
        int n_y = s_y, n_x = s_x; // next pixel
        while (img[n_y][n_x] != ONE || (n_y == p_y && n_x == p_x)) {
            n_y = s_y + dirs[dir][0];
            n_x = s_x + dirs[dir][1];
            dir = (dir + 1) % 8;
        }
        dir = (dir + 6) % 8;

        int c_y = n_y, c_x = n_x; // current pixel

        int[][] img_cleansed = new int[height][width];
        int[][] img_walkk = new int[10 * (height + width)][2];
        int curr_pix = 0;

        while (c_y != s_y || c_x != s_x) {
            c_y = n_y;
            c_x = n_x;

            // collect data
            img_cleansed[c_y][c_x] = ONE;
            img_walkk[curr_pix][0] = c_y;
            img_walkk[curr_pix][1] = c_x;
            curr_pix++;
            // collected data

            while (img[n_y][n_x] != ONE || (n_y == p_y && n_x == p_x) || (n_y == c_y && n_x == c_x)) {
                n_y = c_y + dirs[dir][0];
                n_x = c_x + dirs[dir][1];
                dir = (dir + 1) % 8;
            }
            dir = (dir + 6) % 8;
            p_y = c_y;
            p_x = c_x;
        }

        // save collected data
        img = Arrays.copyOfRange(img_cleansed, 0, curr_pix);
        this.img_walk = Arrays.copyOfRange(img_walkk, 0, curr_pix);
        slopes = new double[curr_pix];
    }

    private Graphics createFrame(String text) throws InterruptedException {
        JFrame frame = new JFrame(text);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setUndecorated(true);
        frame.setSize(width, height);
        frame.setVisible(true);
        Thread.sleep(100);
        Graphics g = frame.getGraphics();
        Thread.sleep(100);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.GREEN);
        g.drawString(text, 10, 10);
        return g;
    }
}
