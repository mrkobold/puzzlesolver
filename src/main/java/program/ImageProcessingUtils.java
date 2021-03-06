package program;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static program.Main.PADDING;

class ImageProcessingUtils {

    static List<Piece> getPiecesFromImage(BufferedImage img) {
        int[][] pixels = getPixelsRGB(img);
        int[][] grayScale = getGrayScale(pixels);
        int[][] blurred = getBlurred(grayScale);
        int[][] binary = getBinary(grayScale);
        int[][] cleansed = getCleansed(binary);
        int[][] edges = getEdges(cleansed);
        return getObjects(edges);
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

    private static List<Piece> getObjects(int[][] img) {
        final int TO_BE_VISITED = 5;
        final int VISITED = 4;

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
}
