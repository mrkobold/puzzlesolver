package program;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

public class Main {

    private static final int VISITED = 4;
    private static final int TO_BE_VISITED = 5;
    private static final int PADDING = 20;

    public static void main(String[] args) throws Exception {
        BufferedImage img = ImageIO.read(new File("photos/resized3.jpg"));

        int[][] pixels = getPixelsRGB(img);
        int[][] grayScale = getGrayScale(pixels);
        int[][] blurred = getBlurred(grayScale);
        int[][] binary = getBinary(grayScale);
        int[][] cleansed = getCleansed(binary);
        int[][] edges = getEdges(cleansed);

        List<Piece> pieces = getObjects(edges);

        // filter out pimples
        double avg_perimeter = pieces.stream().mapToInt(p -> p.getImg().length).average().orElseThrow(() -> new Exception("WTF?? No perimeter of pieces exists??"));
        pieces = pieces.stream().filter(p -> p.getImg().length > avg_perimeter / 3).collect(Collectors.toList());
        for (int i = 0; i < pieces.size(); i++) {
            pieces.get(i).setId(i);
        }

//        pieces.remove(1);
        pieces.forEach(Piece::walk);
        pieces.forEach(Piece::compute_slopes);
        pieces.forEach(Piece::compute_delta_slopes);
//        pieces.forEach(Piece::draw_slopes);
//        pieces.forEach(Piece::draw_curves_based_on_slopes_avg);
//        pieces.forEach(Piece::draw_curves_based_on_sudden_slopes_change);
//        pieces.forEach(Piece::draw_corners_based_on_sum_d2_length);
        pieces.forEach(Piece::find_corner_points_based_on_sum_d2_length);
        pieces.forEach(Piece::draw_with_corners);
        pieces.forEach(Piece::compute_distances_between_corners);

        List<Double> dist_corners_0 = pieces.get(0).getDistances_between_corners();
        List<Double> dist_corners_1 = pieces.get(1).getDistances_between_corners();

//        for (int i = 0; i < dist_corners_0.size(); i++) {
//            double d0 = dist_corners_0.get(i);
//            for (int j = 0; j < dist_corners_1.size(); j++) {
//                double d1 = dist_corners_1.get(j);
//
//                if (Math.abs(d0 - d1) < 10) {
//                    System.out.println("piece0:" + i + "-" + (i + 1) + " *** piece1:" + j + "-" + (j + 1));
//                    tryFit(pieces, i, (i + 1) % dist_corners_0.size(), j, (j + 1) % dist_corners_1.size());
//                }
//            }
//        }

        tryFit(pieces, 0, 3, 3, 2);
    }

    private static void tryFit(List<Piece> pieces, int a_id, int b_id, int c_id, int d_id) throws InterruptedException {
        Piece p0 = pieces.get(0);
        Piece p1 = pieces.get(1);
        int[][] img_walk_0 = clone_array(p0.getImg_walk());
        int[][] img_walk_1 = clone_array(p1.getImg_walk());

        int a_id_img_walk = p0.getCorner_ids_on_img_walk().get(a_id);
        double ya = (double) img_walk_0[a_id_img_walk][0];
        double xa = (double) img_walk_0[a_id_img_walk][1];

        int b_id_img_walk = p0.getCorner_ids_on_img_walk().get(b_id);
        double yb = (double) img_walk_0[b_id_img_walk][0];
        double xb = (double) img_walk_0[b_id_img_walk][1];


        int c_id_img_walk = p1.getCorner_ids_on_img_walk().get(c_id);
        double yc = (double) img_walk_1[c_id_img_walk][0];
        double xc = (double) img_walk_1[c_id_img_walk][1];

        int d_id_img_walk = p1.getCorner_ids_on_img_walk().get(d_id);
        double yd = (double) img_walk_1[d_id_img_walk][0];
        double xd = (double) img_walk_1[d_id_img_walk][1];

        double m0 = (yb - ya) / (xb - xa);
        double alfa0 = Math.atan(m0); // (-Pi/2, Pi/2)

        double m1 = (yd - yc) / (xd - xc);
        double alfa1 = Math.atan(m1);

        double rotation_alfa = alfa0 - alfa1;
        // rotate each pixel's value in img_walk_1 by rotation_alfa around (yc, xc)
        int[][] img_walk_1_new = new int[img_walk_1.length][img_walk_1[0].length];
        for (int i = 0; i < img_walk_1.length; i++) {
            double y = (double) img_walk_1[i][0];
            double x = (double) img_walk_1[i][1];

            double l = sqrt(y * y + x * x);
            double original_alfa = Math.atan(y / x);
            double new_alfa = original_alfa + rotation_alfa;

            double new_y = sin(new_alfa) * l + yc;
            double new_x = cos(new_alfa) * l + xc;

            img_walk_1_new[i][0] = (int) new_y;
            img_walk_1_new[i][1] = (int) new_x;
        }


        /// show piece and rotated image together
        int rotated_corner_y = img_walk_1_new[c_id_img_walk][0];
        int rotated_corner_x = img_walk_1_new[c_id_img_walk][1];

        int[][] piece0_walk = p0.getImg_walk();
        int piece_corner_y = img_walk_0[b_id_img_walk][0];
        int piece_corner_x = img_walk_0[b_id_img_walk][1];

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

    private static int[][] clone_array(int[][] original) {
        int[][] clone = new int[original.length][original[0].length];
        for (int i = 0; i < original.length; i++) {
            System.arraycopy(original[i], 0, clone[i], 0, original[0].length);
        }
        return clone;
    }

    private static int[][] getBlurred(int[][] img) {
        int h = img.length;
        int w = img[0].length;
        int[][] result = new int[h][w];

        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                result[y][x] = img[y - 1][x - 1] + 2 * img[y - 1][x] + img[y - 1][x + 1] +
                        2 * img[y][x - 1] + 4 * img[y][x] + 2 * img[y][x + 1] +
                        img[y + 1][x - 1] + 2 * img[y + 1][x] + img[y + 1][x + 1];
                result[y][x] /= 16;
            }
        }
        return result;
    }

    private static List<Piece> getObjects(int[][] img) {
        int height = img.length;
        int width = img[0].length;
        List<Piece> objects = new ArrayList<>();

        for (int row = 1; row < height - 1; row++) {
            for (int col = 1; col < width - 1; col++) {
                // visit a piece and gather it into "piece"
                if (img[row][col] == 255) {
                    List<Position> piece = new ArrayList<>();
                    Stack<Position> stack = new Stack<>();
                    stack.add(new Position(row, col));
                    // flood-fill-ish
                    int miny = Integer.MAX_VALUE, maxy = Integer.MIN_VALUE;
                    int minx = Integer.MAX_VALUE, maxx = Integer.MIN_VALUE;
                    while (!stack.isEmpty()) {
                        Position current = stack.pop();
                        piece.add(current);
                        int y = current.y;
                        int x = current.x;
                        img[y][x] = VISITED;
                        // find size of piece
                        if (miny > y) {
                            miny = y;
                        }
                        if (maxy < y) {
                            maxy = y;
                        }
                        if (minx > x) {
                            minx = x;
                        }
                        if (maxx < x) {
                            maxx = x;
                        }
                        // flood-fill piece
                        if (img[y - 1][x - 1] == 255) {
                            stack.add(new Position(y - 1, x - 1));
                            img[y - 1][x - 1] = TO_BE_VISITED;
                        }
                        if (img[y - 1][x] == 255) {
                            stack.add(new Position(y - 1, x));
                            img[y - 1][x] = TO_BE_VISITED;
                        }
                        if (img[y - 1][x + 1] == 255) {
                            stack.add(new Position(y - 1, x + 1));
                            img[y - 1][x + 1] = TO_BE_VISITED;
                        }
                        if (img[y][x - 1] == 255) {
                            stack.add(new Position(y, x - 1));
                            img[y][x - 1] = TO_BE_VISITED;
                        }
                        if (img[y][x + 1] == 255) {
                            stack.add(new Position(y, x + 1));
                            img[y][x + 1] = TO_BE_VISITED;
                        }
                        if (img[y + 1][x - 1] == 255) {
                            stack.add(new Position(y + 1, x - 1));
                            img[y + 1][x - 1] = TO_BE_VISITED;
                        }
                        if (img[y + 1][x] == 255) {
                            stack.add(new Position(y + 1, x));
                            img[y + 1][x] = TO_BE_VISITED;
                        }
                        if (img[y + 1][x + 1] == 255) {
                            stack.add(new Position(y + 1, x + 1));
                            img[y + 1][x + 1] = TO_BE_VISITED;
                        }
                    }

                    int[][] pieceArray = new int[maxy - miny + PADDING][maxx - minx + PADDING];
                    for (int i = 0; i < pieceArray.length; i++) {
                        for (int j = 0; j < pieceArray[0].length; j++) {
                            pieceArray[i][j] = 0;
                        }
                    }
                    for (Position p : piece) {
                        pieceArray[p.y - miny + PADDING / 2][p.x - minx + PADDING / 2] = 255;
                    }
                    objects.add(new Piece(pieceArray));
                }
            }
        }
        return objects;
    }

    private static int[][] getEdges(int[][] image) {
        int height = image.length;
        int width = image[0].length;
        int[][] edges = new int[height][width];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (image[row][col] == 255 &&
                        (image[row - 1][col] == 0 ||
                                image[row][col - 1] == 0 ||
                                image[row][col + 1] == 0 ||
                                image[row + 1][col] == 0)) {
                    edges[row][col] = 255;
                } else {
                    edges[row][col] = 0;
                }
            }
        }
        return edges;
    }

    private static int[][] getCleansed(int[][] binary) {
        int height = binary.length;
        int width = binary[0].length;
        int[][] cleansed = new int[height][width];

        for (int row = 1; row < height - 1; row++) {
            for (int col = 1; col < width - 1; col++) {
                int val;
                byte n = binary[row - 1][col] == 255 ? (byte) 1 : (byte) 0;
                byte e = binary[row][col + 1] == 255 ? (byte) 1 : (byte) 0;
                byte s = binary[row + 1][col] == 255 ? (byte) 1 : (byte) 0;
                byte w = binary[row][col - 1] == 255 ? (byte) 1 : (byte) 0;
                byte sum = (byte) (n + e + s + w);

                if (binary[row][col] == 255 && sum < 2) {
                    val = 0;
                } else if (binary[row][col] == 0 && sum > 2) {
                    val = 255;
                } else {
                    val = binary[row][col];
                }
                cleansed[row][col] = val;
            }
        }
        return cleansed;
    }

    private static int[][] getBinary(int[][] greyScale) {
        int height = greyScale.length;
        int width = greyScale[0].length;
        int[][] binaryPixels = new int[height][width];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                binaryPixels[row][col] = greyScale[row][col] > 150 ? 255 : 0;
            }
        }
        return binaryPixels;
    }

    private static int[][] getGrayScale(int[][] pixels) {
        int height = pixels.length;
        int width = pixels[0].length;
        int[][] grayScalePixels = new int[height][width];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int pixelOriginal = pixels[row][col];

                int red = (pixelOriginal & 0x00ff0000) >> 16;
                int green = (pixelOriginal & 0x0000ff00) >> 8;
                int blue = pixelOriginal & 0x000000ff;
                int colorMean = (red + green + blue) / 3;

                grayScalePixels[row][col] = colorMean;
            }
        }
        return grayScalePixels;
    }

    private static void showImage(int[][] pixels) throws InterruptedException {
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

    private static int[][] getPixelsRGB(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        int[][] pixels = new int[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y][x] = img.getRGB(x, y);
            }
        }
        return pixels;
    }
}
