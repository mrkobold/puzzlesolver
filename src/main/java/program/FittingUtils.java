package program;

import javax.swing.JFrame;
import java.awt.Color;
import java.awt.Graphics;
import java.util.Arrays;
import java.util.IntSummaryStatistics;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

class FittingUtils {

    static void tryHard(Piece p0, Piece p1, int c00_name, int c01_name, int c10_name, int c11_name) throws InterruptedException {
        int[][] img_walk_0 = clone_array(p0.getImg_walk());
        int[][] img_walk_1 = clone_array(p1.getImg_walk());

        // get coordinates of to-be-matched corners
        int c11_id_img_walk = p0.getCorner_ids_on_img_walk().get(c00_name);
        double y00 = (double) img_walk_0[c11_id_img_walk][0];
        double x00 = (double) img_walk_0[c11_id_img_walk][1];

        int c12_id_img_walk = p0.getCorner_ids_on_img_walk().get(c01_name);
        double y01 = (double) img_walk_0[c12_id_img_walk][0];
        double x01 = (double) img_walk_0[c12_id_img_walk][1];

        int c21_id_img_walk = p1.getCorner_ids_on_img_walk().get(c10_name);
        double y10 = (double) img_walk_1[c21_id_img_walk][0];
        double x10 = (double) img_walk_1[c21_id_img_walk][1];

        int d_id_img_walk = p1.getCorner_ids_on_img_walk().get(c11_name);
        double y11 = (double) img_walk_1[d_id_img_walk][0];
        double x11 = (double) img_walk_1[d_id_img_walk][1];

        // check if need to re-order
        double yc0 = p0.getCenter_y();
        double xc0 = p0.getCenter_x();

        // compute rotation angle of p1
        double p1_rotation_alfa = computeRotationAlfa(y00, x00, y01, x01, y10, x10, y11, x11);

        // rotate each pixel's value in img_walk_1 by p1_rotation_alfa around (y10, x10)
        int[][] img_walk_1_new = getP1RotatedWalk(img_walk_1, y10, x10, p1_rotation_alfa);

        // get rotated corner (y10, x10) of p1
        int rotated_corner_y = img_walk_1_new[c21_id_img_walk][0];
        int rotated_corner_x = img_walk_1_new[c21_id_img_walk][1];

        // get corner (y01, x01) of p0
        int[][] piece0_walk = p0.getImg_walk();
        int piece_corner_y = img_walk_0[c12_id_img_walk][0];
        int piece_corner_x = img_walk_0[c12_id_img_walk][1];

        // TODO EXPERIMENTAL build frame for visualization
        IntSummaryStatistics y_stat = Arrays.stream(img_walk_1_new).mapToInt(a -> a[0]).summaryStatistics();
        IntSummaryStatistics x_stat = Arrays.stream(img_walk_1_new).mapToInt(a -> a[1]).summaryStatistics();
        int height_p1 = y_stat.getMax() - y_stat.getMin() - 100;
        int width_p1 = x_stat.getMax() - x_stat.getMin();
        JFrame frame = new JFrame();
        frame.setSize(width_p1 + p0.getImg()[0].length, height_p1 + p0.getImg().length);
        frame.setUndecorated(true);
        frame.setVisible(true);
        Graphics g = frame.getGraphics();
        Thread.sleep(100);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 10_000, 10_000);
        Thread.sleep(100);

        g.setColor(Color.CYAN);
        for (int i = 0; i < piece0_walk.length; i++) {
            g.fillOval(piece0_walk[i][1], piece0_walk[i][0] + height_p1, 1, 1);
        }

        int y_offset = height_p1 - rotated_corner_y + piece_corner_y;
        int x_offset = - rotated_corner_x + piece_corner_x;

        g.setColor(Color.BLUE);
        for (int i = 0; i < img_walk_1_new.length; i++) {
            g.fillOval(img_walk_1_new[i][1] + x_offset, img_walk_1_new[i][0] + y_offset, 1, 1);
        }
    }

    private static int[][] getP1RotatedWalk(int[][] img_walk_1, double y21, double x21, double p1_rotation_alfa) {
        int[][] img_walk_1_new = new int[img_walk_1.length][img_walk_1[0].length];
        for (int i = 0; i < img_walk_1.length; i++) {
            double y = (double) img_walk_1[i][0];
            double x = (double) img_walk_1[i][1];

            double l = sqrt(y * y + x * x);
            double original_alfa = Math.atan(y / x);
            double new_alfa = original_alfa + p1_rotation_alfa;

            double new_y = sin(new_alfa) * l + y21;
            double new_x = cos(new_alfa) * l + x21;

            img_walk_1_new[i][0] = (int) new_y;
            img_walk_1_new[i][1] = (int) new_x;
        }
        return img_walk_1_new;
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
