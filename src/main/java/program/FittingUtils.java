package program;

import javax.swing.JFrame;
import java.awt.Color;
import java.awt.Graphics;
import java.util.Arrays;
import java.util.IntSummaryStatistics;

import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

class FittingUtils {

    private static final double VERY_OFF_DELTA = 50;
    private static final double VERY_OFF_COUNT = 150;
    private static final double MINI_ADJUST = 2;

    static double tryHard(Piece p0, Piece p1, int c00_name, int c01_name, int c10_name, int c11_name) throws InterruptedException {
        int[][] img_walk_0 = clone_array(p0.getImg_walk());
        int[][] img_walk_1 = clone_array(p1.getImg_walk());

        // get coordinates of to-be-matched corners
        int c00_id_img_walk = p0.getCorner_ids_on_img_walk().get(c00_name);
        double y00 = (double) img_walk_0[c00_id_img_walk][0];
        double x00 = (double) img_walk_0[c00_id_img_walk][1];

        int c01_id_img_walk = p0.getCorner_ids_on_img_walk().get(c01_name);
        double y01 = (double) img_walk_0[c01_id_img_walk][0];
        double x01 = (double) img_walk_0[c01_id_img_walk][1];

        int c10_id_img_walk = p1.getCorner_ids_on_img_walk().get(c10_name);
        double y10 = (double) img_walk_1[c10_id_img_walk][0];
        double x10 = (double) img_walk_1[c10_id_img_walk][1];

        int c11_id_img_walk = p1.getCorner_ids_on_img_walk().get(c11_name);
        double y11 = (double) img_walk_1[c11_id_img_walk][0];
        double x11 = (double) img_walk_1[c11_id_img_walk][1];

        // if Euler dist delta is too big => return
        double distanceP0Corners = sqrt(pow(y00 - y01, 2) + pow(x00 - x01, 2));
        double distanceP1Corners = sqrt(pow(y10 - y11, 2) + pow(x10 - x11, 2));
        if (distanceP0Corners / distanceP1Corners > 1.15 || distanceP0Corners / distanceP1Corners < 0.85) {
            return -1.0;
        }

        // compute rotation angle of p1
        double rotation_alfa = computeRotationAlfa(y00, x00, y01, x01, y10, x10, y11, x11);

        // rotate img_walk_1 by rotation_alfa around (y10, x10)
        int[][] img_walk_1_new = getP1RotatedWalk(img_walk_1, p1.getCenter_y(), p1.getCenter_x(), rotation_alfa);

        // get rotated-around corner (y10, x10) of p1
        int rotated_corner_y = img_walk_1_new[c10_id_img_walk][0];
        int rotated_corner_x = img_walk_1_new[c10_id_img_walk][1];

        // get corner (y01, x01) of p0
        int[][] piece0_walk = p0.getImg_walk();
        int static_corner_y = img_walk_0[c01_id_img_walk][0];
        int static_corner_x = img_walk_0[c01_id_img_walk][1];

        // count steps on pieces between said corners
        double stepsP0 = Math.abs(img_walk_0.length + c00_id_img_walk - c01_id_img_walk) % img_walk_0.length;
        double stepsP1 = Math.abs(img_walk_1.length + c10_id_img_walk - c11_id_img_walk) % img_walk_1.length;

        // if steps dist delta is too big => return
        double proportionStepsP0P1 = stepsP0 / stepsP1;
        if (proportionStepsP0P1 > 1.05d || proportionStepsP0P1 < 0.95d) {
            return -1.0d;
        }

        // step proportionally and sum distances
        // compute direction (static center) -> (offsetted rotated center)
        double staticCenterY = p0.getCenter_y();
        double staticCenterX = p0.getCenter_x();

        double rotatedOffsettedCenterY = p1.getCenter_y() - rotated_corner_y + static_corner_y;
        double rotatedOffsettedCenterX = p1.getCenter_x() - rotated_corner_x + static_corner_x;

        double deltaCenterY = rotatedOffsettedCenterY - staticCenterY;
        double deltaCenterX = rotatedOffsettedCenterX - staticCenterX;

        double alfaCenter = Math.atan(deltaCenterY / deltaCenterX);
        double miniAdjustY = MINI_ADJUST * sin(alfaCenter);
        double miniAdjustX = MINI_ADJUST * cos(alfaCenter);

        if (deltaCenterY > 0) {
            miniAdjustY *= -1;
        }
        if (deltaCenterX < 0) {
            miniAdjustX *= -1;
        }


        // TODO invert order if other is shorter than this
        Graphics g0 = null;
        Graphics g1 = null;
//        g0 = p0.draw_with_corners();
//        g1 = p1.draw_with_corners(img_walk_1_new);
//        g0.setColor(Color.MAGENTA);
//        g1.setColor(Color.MAGENTA);
        double sumDistances =
                getSumDistances(p0, p1, img_walk_0, img_walk_1_new, c00_id_img_walk, c11_id_img_walk, rotated_corner_y, rotated_corner_x, static_corner_y, static_corner_x, stepsP0, stepsP1, miniAdjustY, miniAdjustX, g0, g1);

        // return sum distances / steps count
        double differenceNormalized = sumDistances / (stepsP0 < stepsP1 ? stepsP0 : stepsP1);
        if (differenceNormalized < 10) {
            drawFittedPieces(p0, img_walk_1_new, rotated_corner_y, rotated_corner_x, piece0_walk, static_corner_y, static_corner_x,
                    miniAdjustY, miniAdjustX);
        }
        return differenceNormalized;
    }

    private static double getSumDistances(Piece p0, Piece p1, int[][] img_walk_0, int[][] img_walk_1, int c00_id_img_walk, int c11_id_img_walk, int rotated_corner_y, int rotated_corner_x, int static_corner_y, int static_corner_x, double stepsP0, double stepsP1, double miniAdjustY, double miniAdjustX, Graphics g0, Graphics g1) {
        double sumDistances = 0.0;
        double stepsProportion = stepsP1 / stepsP0;

        int lengthP1 = p1.getImg_walk().length;
        int lengthP0 = p0.getImg_walk().length;

        int sameCanvasOffsetY = -rotated_corner_y + static_corner_y;
        int sameCanvasOffsetX = -rotated_corner_x + static_corner_x;
        for (int i = 0; i < stepsP0; i++) {
            // point on static piece
            int absPos0 = (c00_id_img_walk - i + lengthP0) % lengthP0;
            double y0 = img_walk_0[absPos0][0];
            double x0 = img_walk_0[absPos0][1];

            if (g0 != null)
                g0.drawOval((int) x0, (int) y0, 1, 1);

            // point on rotated piece
            // move to same canvas y1 and x1
            int ablPos1 = (c11_id_img_walk + (int) (i * stepsProportion)) % lengthP1;
            double y1 = img_walk_1[ablPos1][0]
                    + sameCanvasOffsetY
                    + miniAdjustY;
            double x1 = img_walk_1[ablPos1][1]
                    + sameCanvasOffsetX
                    + miniAdjustX;

            if (g1 != null)
                g1.drawOval(img_walk_1[ablPos1][1], img_walk_1[ablPos1][0], 1, 1);

            double increment = sqrt((y1 - y0) * (y1 - y0) + (x1 - x0) * (x1 - x0));
            sumDistances += increment;
        }
        return sumDistances;
    }

    private static void drawFittedPieces(Piece p0, int[][] img_walk_1_new,
                                         int rotated_corner_y, int rotated_corner_x,
                                         int[][] piece0_walk,
                                         int static_corner_y, int static_corner_x,
                                         double miniAdjustY, double miniAdjustX) throws InterruptedException {
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
            g.fillOval(piece0_walk[i][1], piece0_walk[i][0] + height_p1 / 4, 1, 1);
        }

        int y_offset = height_p1 / 4 - rotated_corner_y + static_corner_y;
        int x_offset = -rotated_corner_x + static_corner_x;

        g.setColor(Color.BLUE);
        for (int i = 0; i < img_walk_1_new.length; i++) {
            g.fillOval(img_walk_1_new[i][1] + x_offset + (int) miniAdjustX, img_walk_1_new[i][0] + y_offset + (int) miniAdjustY, 1, 1);
        }
    }

    private static int[][] getP1RotatedWalk(int[][] img_walk_1, double yc, double xc, double p1_rotation_alfa) {
        int[][] img_walk_1_new = new int[img_walk_1.length][img_walk_1[0].length];

        double l = sqrt(yc * yc + xc * xc);
        double original_alfa = Math.atan(yc / xc);
        double new_alfa = original_alfa + p1_rotation_alfa;
        double offsetCenterY = sin(new_alfa) * l - yc;
        double offsetCenterX = cos(new_alfa) * l - xc;

        for (int i = 0; i < img_walk_1.length; i++) {
            double y = (double) img_walk_1[i][0];
            double x = (double) img_walk_1[i][1];

            l = sqrt(y * y + x * x);
            original_alfa = Math.atan(y / x);
            new_alfa = original_alfa + p1_rotation_alfa;

            double new_y = sin(new_alfa) * l - offsetCenterY;
            double new_x = cos(new_alfa) * l - offsetCenterX;

            img_walk_1_new[i][0] = (int) new_y;
            img_walk_1_new[i][1] = (int) new_x;
        }

        double minY = 0;
        double minX = 0;
        for (int[] yx : img_walk_1_new) {
            if (yx[0] < minY)
                minY = yx[0];
            if (yx[1] < minX)
                minX = yx[1];
        }

        if (minX < 0) {
            minX *= -1;
            minX += 10;
            for (int i = 0; i < img_walk_1_new.length; i++)
                img_walk_1_new[i][1] += minX;
        }
        if (minY < 0) {
            minY *= -1;
            minY += 10;
            for (int i = 0; i < img_walk_1_new.length; i++)
                img_walk_1_new[i][0] += minY;
        }

        return img_walk_1_new;
    }

    private static double computeRotationAlfa(double y00, double x00, double y01, double x01,
                                              double y10, double x10, double y11, double x11) {
        double alfa0 = getAlfaFromEndpoints(y00, x00, y01, x01);
        double alfa1 = getAlfaFromEndpoints(y11, x11, y10, x10);
        return alfa0 - alfa1;
    }

    private static double getAlfaFromEndpoints(double y00, double x00, double y01, double x01) {
        double m0 = (y01 - y00) / (x01 - x00);
        double alfa0 = Math.atan(m0);
        if (y01 <= y00 && x01 <= x00) {
            alfa0 += Math.PI;
        } else if (y01 >= y00 && x01 <= x00) {
            alfa0 += Math.PI;
        }
        return alfa0;
    }

    private static int[][] clone_array(int[][] original) {
        int[][] clone = new int[original.length][original[0].length];
        for (int i = 0; i < original.length; i++) {
            System.arraycopy(original[i], 0, clone[i], 0, original[0].length);
        }
        return clone;
    }
}
