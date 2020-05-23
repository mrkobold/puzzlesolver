package program;

import lombok.Getter;

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.abs;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;

@Getter
class Piece {
    private static final int[][] dirs = new int[][]{{-1, 0}, {-1, 1}, {0, 1}, {1, 1}, {1, 0}, {1, -1}, {0, -1}, {-1, -1}};

    private int height, width;
    private double[] slopes;
    private double[] delta_slopes;

    private int[][] img;
    private int[][] img_walk;
    private List<Integer> corner_ids_on_img_walk;
    private List<Double> distances_between_corners;

    private int center_y;
    private int center_x;

    Piece(int[][] img) {
        this.img = img;
        height = img.length;
        width = img[0].length;
    }

    /**
     * record path of perimeter
     */
    void walk() {
        // will hold the starting point of the walk around perimeter
        int s_y = -1, s_x = -1;
        boolean found_s = false;
        for (int y = 0; y < height && !found_s; y++) {
            for (int x = 0; x < width && !found_s; x++) {
                if (img[y][x] == 255) {
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
        while (img[n_y][n_x] != 255 || (n_y == p_y && n_x == p_x)) {
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
            img_cleansed[c_y][c_x] = 255;
            img_walkk[curr_pix][0] = c_y;
            img_walkk[curr_pix][1] = c_x;
            curr_pix++;
            // collected data

            while (img[n_y][n_x] != 255 || (n_y == p_y && n_x == p_x) || (n_y == c_y && n_x == c_x)) {
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

    /**
     * find corners based on normalized distance sum (d^2) / length
     * a - starting point of line
     * d - end point of line
     * c - point on puzzle
     * b - point on line for c
     */
    void find_corner_points_based_on_sum_d2_length() {
        int n = img_walk.length;
        int CHECK_DISTANCE = 20;
        int MIN_CORNER_DISTANCE = 100;
        double NORM_D_THRESHOLD = 2.7;

        List<Pair<Integer, Double>> corner_candidates = new ArrayList<>(100);
        for (int i = 0; i < img_walk.length; i++) {
            double a_y = (double) img_walk[i][0];
            double a_x = (double) img_walk[i][1];

            double d_y = (double) img_walk[(i + CHECK_DISTANCE) % n][0];
            double d_x = (double) img_walk[(i + CHECK_DISTANCE) % n][1];

            double line_length = sqrt((d_y - a_y) * (d_y - a_y) + (d_x - a_x) * (d_x - a_x));
            double m = d_x == a_x ? 0 : (d_y - a_y) / (d_x - a_x);

            double distance_sum = 0;
            for (int j = i; j <= i + CHECK_DISTANCE; j++) {
                double c_y = (double) img_walk[j % n][0];
                double c_x = (double) img_walk[j % n][1];

                double b_x = (m) / (m * m + 1) * (c_y + 1 / m * c_x - a_y + m * a_x);
                double b_y = a_y + m * (b_x - a_x);

                double dist = sqrt((c_x - b_x) * (c_x - b_x) + (c_y - b_y) * (c_y - b_y));
                distance_sum += dist;
            }

            double normalized_distance_sum = distance_sum / line_length;
            if (normalized_distance_sum > NORM_D_THRESHOLD) {
                corner_candidates.add(new Pair<>((i + CHECK_DISTANCE / 2) % n, normalized_distance_sum));
            }
        }

        corner_ids_on_img_walk = new ArrayList<>(10);
        for (int i = 0; i < corner_candidates.size(); i++) {
            Pair<Integer, Double> ith_potential_corner = corner_candidates.get(i);

            int j = i + 1;
            boolean same_group = true;
            while (j < corner_candidates.size() && same_group) {
                Pair<Integer, Double> jth_potential_corner = corner_candidates.get(j);

                int y_at_imgwalk_j = img_walk[jth_potential_corner.getKey()][0];
                int x_at_imgwalk_j = img_walk[jth_potential_corner.getKey()][1];
                int y_at_imgwalk_i = img_walk[ith_potential_corner.getKey()][0];
                int x_at_imgwalk_i = img_walk[ith_potential_corner.getKey()][1];
                double distance = sqrt((y_at_imgwalk_i - y_at_imgwalk_j) * (y_at_imgwalk_i - y_at_imgwalk_j) +
                        (x_at_imgwalk_i - x_at_imgwalk_j) * (x_at_imgwalk_i - x_at_imgwalk_j));
                if (abs(distance) > MIN_CORNER_DISTANCE) {
                    same_group = false;
                } else {
                    j++;
                }
            }

            // group goes from index [i, j - 1) exclusive => find strongest corner from candidates
            double max_fitness = -1;
            int id_max_fitness = -1;
            for (int k = i; k < j; k++) {
                if (corner_candidates.get(k).getValue() > max_fitness) {
                    max_fitness = corner_candidates.get(k).getValue();
                    id_max_fitness = k;
                }
            }
            corner_ids_on_img_walk.add(corner_candidates.get(id_max_fitness).getKey());
            i = j - 1;
        }
        center_y = (int) corner_ids_on_img_walk.stream().mapToInt(i -> img_walk[i][0]).average().getAsDouble();
        center_x = (int) corner_ids_on_img_walk.stream().mapToInt(i -> img_walk[i][1]).average().getAsDouble();
    }

    void compute_steps_between_corners() {
        int corner_count = corner_ids_on_img_walk.size();
        distances_between_corners = new ArrayList<>(corner_count);

        for (int i = 0; i < corner_count; i++) {
            distances_between_corners.add((double) Math.abs(corner_ids_on_img_walk.get(i) - corner_ids_on_img_walk.get((i + 1) % corner_count)));
        }
    }

    void draw_with_corners() {
        Graphics g = createFrame("corners: maximum from groups of sum(d^2) / len");

        g.setColor(Color.GREEN);
        for (int i = 0; i < img_walk.length; i++) {
            g.fillOval(img_walk[i][1], img_walk[i][0], 1, 1);
        }

        int R = 12;
        int corner_id = 0;
        for (Integer corner_id_on_img_walk : corner_ids_on_img_walk) {
            g.setColor(Color.RED);
            int x_corner = img_walk[corner_id_on_img_walk][1] - R / 2;
            int y_corner = img_walk[corner_id_on_img_walk][0] - R / 2;
            g.fillOval(x_corner, y_corner, R, R);

            g.setColor(Color.CYAN);
            g.drawString(Integer.toString(corner_id), x_corner - Integer.signum(x_corner - center_x) * 10, y_corner - Integer.signum(y_corner - center_y) * 10);
            corner_id++;
        }

        g.setColor(Color.YELLOW);
        g.fillOval(center_x, center_y, R, R);
    }

    void draw_curves() {
        Graphics g = createFrame("Curves");

        int n = img_walk.length;
        int L = 5;
        Color c;
        for (int i = 0; i < n; i++) {
            int id_p = i - L;
            if (id_p < 0)
                id_p += n;
            int id_n = i + L;
            if (id_n > n - 1) {
                id_n -= n;
            }

            double dp_m = slopes[i] - slopes[id_p];
            double dn_m = slopes[id_n] - slopes[i];

            double curve_threshold = 4;
            if (abs(dp_m) > curve_threshold || abs(dn_m) > curve_threshold) {
                c = Color.RED;
            } else {
                c = Color.WHITE;
            }

            g.setColor(c);
//            g.drawOval(img_walk[i][1], img_walk[i][0], 1, 1);
            g.drawLine(img_walk[i][1], img_walk[i][0], img_walk[i][1], img_walk[i][0]);
        }
    }

    void draw_curves_3points_angles_approach() {
        Graphics g = createFrame("Curves");

        int n = img_walk.length;
        int L = 10;
        for (int i = 0; i < n; i++) {
            int p_id = i < L ? i + n - L : i - L;
            int p_y = img_walk[p_id][0];
            int p_x = img_walk[p_id][1];

            int c_y = img_walk[i][0];
            int c_x = img_walk[i][1];

            int n_id = i + L > n - 1 ? i + L - n : i + L;
            int n_y = img_walk[n_id][0];
            int n_x = img_walk[n_id][1];

            double m_p = ((double) c_y - (double) p_y) / ((double) c_x - (double) p_x);
            double m_n = ((double) n_y - (double) c_y) / ((double) n_x - (double) c_x);

            double a1 = Math.atan(m_p);
            double a2 = Math.atan(m_n);

            double threshold = 0.05;
            Color c = a2 + threshold < a1 ? Color.GREEN : Color.RED;

            g.setColor(c);
            g.drawOval(c_x, c_y, 1, 1);
        }
    }

    void draw_curves_slopes_approach() {
        Graphics g = createFrame("Curves");

        int L = 50;
        for (int i = 0; i < img_walk.length; i++) {
            int y = img_walk[i][0];
            int x = img_walk[i][1];

            int y_p = img_walk[(i - L + img_walk.length) % img_walk.length][0];
            int x_p = img_walk[(i - L + img_walk.length) % img_walk.length][1];
            double m = slopes[(i - L + img_walk.length) % img_walk.length];

            int d;
            if (x_p < x) {
                d = (int) (y + (y_p + (x - x_p) * m));
            } else {
                d = (int) (y - (y_p + (x_p - x) * m));
            }

            int add = (int) (Math.atan(d) / (Math.PI * 2) * 125) + 125;
            int green = add;
            int blue = 255 - add;
            Color c = new Color(125, green, blue);

            g.setColor(c);
            g.drawOval(x, y, 1, 1);
        }
    }

    void draw_slopes() {
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
            int steps = width / 20;
            if (i % steps == 0) {
                int y_c = img_walk[i][0];
                int x_c = img_walk[i][1];

                int d_x = (int) (sqrt((l * l) / (1 + slope * slope)));
                int d_y = (int) (d_x * slope);
                g.drawLine(x_c - d_x, y_c - d_y, x_c + d_x, y_c + d_y);
            }
        }
    }

    /**
     * draw curves based on sudden slopes-change in interval
     */
    void draw_curves_based_on_sudden_slopes_change() {
        Graphics g = createFrame("curves sudden slopes change");

        int n = slopes.length;
        int WINDOW_SIZE = n / (50 * 2);
        double SLOPE_CHANGE_THRESHOLD = 15.0d;

        for (int i = WINDOW_SIZE; i < n - WINDOW_SIZE - 1; i++) {
//            double avg_before = 0;
//            for (int j = i - WINDOW_SIZE; j < i; j++) {
//                avg_before += slopes[j];
//            }
//            avg_before /= WINDOW_SIZE;
//
//            double avg_after = 0;
//            for (int j = i + 1; j < i + WINDOW_SIZE + 1; j++) {
//                avg_after += slopes[j];
//            }
//            avg_after /= WINDOW_SIZE;
//
//            if (abs(avg_after - avg_before) > SLOPE_CHANGE_THRESHOLD) {
//                g.setColor(Color.RED);
//                g.fillOval(img_walk[i][1], img_walk[i][0], 1, 1);
//            }
            if ((slopes[i - WINDOW_SIZE] - slopes[i + WINDOW_SIZE]) > SLOPE_CHANGE_THRESHOLD) {
                g.setColor(Color.RED);
                g.fillOval(img_walk[i][1], img_walk[i][0], 1, 1);
            }
        }

    }


    /**
     * curves base on delta_slopes averages
     */
    void draw_curves_based_on_slopes_avg() {
        Graphics g = createFrame("curves delta slopes");

        int n = delta_slopes.length;
        int CHECK_DISTANCE = 1;
        double COLOR_THRESHOLD = 0.0001;
        double CURVE_THRESHOLD = 15;

        // AVG in every CHECK_DISTANCE length segment
        // if everybody within THRESHOLD distance from AVG =>
        for (int i = 0; i < n - CHECK_DISTANCE - 1; i++) {
            double segment_sum = 0;
            for (int j = i; j < i + CHECK_DISTANCE; j++)
                segment_sum += delta_slopes[j];
            double avg_delta_slope = segment_sum / CHECK_DISTANCE;

            boolean uniform = true;
            for (int j = i; j < i + CHECK_DISTANCE && uniform; j++)
                if (abs(delta_slopes[j] - avg_delta_slope) > CURVE_THRESHOLD)
                    uniform = false;

            if (uniform) {
                Color c;
                double slope_change_at_i = delta_slopes[i + CHECK_DISTANCE / 2];
                if (slope_change_at_i > COLOR_THRESHOLD)
                    c = new Color((int) min(255, slope_change_at_i * 1000), 0, 0);
                else if (slope_change_at_i < -COLOR_THRESHOLD)
                    c = new Color(0, (int) min(255, slope_change_at_i * -1000), 0);
                else
                    c = Color.WHITE;

                g.setColor(c);
                g.fillOval(img_walk[i + CHECK_DISTANCE / 2][1], img_walk[i + CHECK_DISTANCE / 2][0], 1, 1);
            }
        }
    }

    /**
     * delta_slopes[i] = slopes[i] - slopes[i - 1]
     */
    void compute_delta_slopes() {
        int n = slopes.length;
        double delta_slopes[] = new double[n];

        for (int i = 1; i < n; i++) {
            delta_slopes[i] = slopes[i] - slopes[i - 1];
        }
        delta_slopes[0] = slopes[n - 1] - slopes[0];
        this.delta_slopes = delta_slopes;
    }

    void compute_slopes() {
        int n = img_walk.length;
        int SLOPE_COMPUTE_LENGTH = n / 50;
        for (int i = 0; i < n; i++) {
            int[] before = img_walk[(i - SLOPE_COMPUTE_LENGTH / 2 + n) % n];
            int[] after = img_walk[(i + SLOPE_COMPUTE_LENGTH / 2) % n];
            slopes[i] = ((double) (after[0] - before[0])) / ((double) (after[1] - before[1]));
        }
    }

    private Graphics createFrame(String text) {
        try {
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
        } catch (Exception e) {
            return null;
        }
    }
}
