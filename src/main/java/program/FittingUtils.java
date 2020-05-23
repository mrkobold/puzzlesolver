package program;

import javax.swing.JFrame;
import java.awt.Color;
import java.awt.Graphics;
import java.util.Arrays;
import java.util.IntSummaryStatistics;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static program.Main.PADDING;

class FittingUtils {

    static void tryFit(Piece p0, Piece p1, int c11, int c12, int c21, int c22) throws InterruptedException {
        int[][] img_walk_0 = clone_array(p0.getImg_walk());
        int[][] img_walk_1 = clone_array(p1.getImg_walk());

        // get coordinates of to-be-matched corners
        int c11_id_img_walk = p0.getCorner_ids_on_img_walk().get(c11);
        double y11 = (double) img_walk_0[c11_id_img_walk][0];
        double x11 = (double) img_walk_0[c11_id_img_walk][1];

        int c12_id_img_walk = p0.getCorner_ids_on_img_walk().get(c12);
        double y12 = (double) img_walk_0[c12_id_img_walk][0];
        double x12 = (double) img_walk_0[c12_id_img_walk][1];

        int c21_id_img_walk = p1.getCorner_ids_on_img_walk().get(c21);
        double y21 = (double) img_walk_1[c21_id_img_walk][0];
        double x21 = (double) img_walk_1[c21_id_img_walk][1];

        int d_id_img_walk = p1.getCorner_ids_on_img_walk().get(c22);
        double y22 = (double) img_walk_1[d_id_img_walk][0];
        double x22 = (double) img_walk_1[d_id_img_walk][1];

        // compute rotation angle of p1
        double rotation_alfa = computeRotationAlfa(y11, x11, y12, x12, y21, x21, y22, x22);

        // rotate each pixel's value in img_walk_1 by rotation_alfa around (yc, xc)
        int[][] img_walk_1_new = new int[img_walk_1.length][img_walk_1[0].length];
        for (int i = 0; i < img_walk_1.length; i++) {
            double y = (double) img_walk_1[i][0];
            double x = (double) img_walk_1[i][1];

            double l = sqrt(y * y + x * x);
            double original_alfa = Math.atan(y / x);
            double new_alfa = original_alfa + rotation_alfa;

            double new_y = sin(new_alfa) * l + y21;
            double new_x = cos(new_alfa) * l + x21;

            img_walk_1_new[i][0] = (int) new_y;
            img_walk_1_new[i][1] = (int) new_x;
        }


        /// show piece and rotated image together
        int rotated_corner_y = img_walk_1_new[c21_id_img_walk][0];
        int rotated_corner_x = img_walk_1_new[c21_id_img_walk][1];

        int[][] piece0_walk = p0.getImg_walk();
        int piece_corner_y = img_walk_0[c12_id_img_walk][0];
        int piece_corner_x = img_walk_0[c12_id_img_walk][1];

        IntSummaryStatistics y_stat = Arrays.stream(img_walk_1_new).mapToInt(a -> a[0]).summaryStatistics();
        IntSummaryStatistics x_stat = Arrays.stream(img_walk_1_new).mapToInt(a -> a[1]).summaryStatistics();
        int height_rotated = y_stat.getMax() - y_stat.getMin() - 100;
        int width_rotated = x_stat.getMax() - x_stat.getMin();

        JFrame frame = new JFrame();
        frame.setSize(width_rotated + p0.getImg()[0].length, height_rotated + p0.getImg().length);

        frame.setUndecorated(true);
        frame.setVisible(true);
        Graphics g = frame.getGraphics();
        Thread.sleep(100);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 10_000, 10_000);
        Thread.sleep(100);

        g.setColor(Color.CYAN);
        for (int i = 0; i < piece0_walk.length; i++) {
            g.fillOval(piece0_walk[i][1], piece0_walk[i][0] + height_rotated, 1, 1);
        }

        int y_offset = piece_corner_y + height_rotated - rotated_corner_y - 5;
        int x_offset = rotated_corner_x - piece_corner_x + 2 * PADDING;

        g.setColor(Color.BLUE);
        for (int i = 0; i < img_walk_1_new.length; i++) {
            g.fillOval(img_walk_1_new[i][1] + x_offset, img_walk_1_new[i][0] + y_offset, 1, 1);
        }
    }

    private static double computeRotationAlfa(double y11, double x11, double y12, double x12, double y21, double x21, double y22, double x22) {
        double m0 = (y12 - y11) / (x12 - x11);
        double alfa0 = Math.atan(m0);
        double m1 = (y22 - y21) / (x22 - x21);
        double alfa1 = Math.atan(m1);
        return alfa0 - alfa1;
    }

    private static int[][] clone_array(int[][] original) {
        int[][] clone = new int[original.length][original[0].length];
        for (int i = 0; i < original.length; i++) {
            System.arraycopy(original[i], 0, clone[i], 0, original[0].length);
        }
        return clone;
    }
}
