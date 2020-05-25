package program;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import static program.FittingUtils.tryHard;

public class Main {

    static final int PADDING = 20;

    public static void main(String[] args) throws Exception {
        BufferedImage img = ImageIO.read(new File("photos/IMG_0756.JPG"));
        List<Piece> pieces = ImageProcessingUtils.getPiecesFromImage(img);
        pieces = filterPimples(pieces);
        for (int i = 0; i < pieces.size(); i++) {
            pieces.get(i).setId(i);
        }

        pieces.forEach(Piece::walk);
        pieces.forEach(Piece::find_corner_points_based_on_sum_d2_length);
        pieces.forEach(Piece::draw_with_corners);
        pieces.forEach(Piece::compute_steps_between_corners);

//        double score = tryHard(pieces.get(0), pieces.get(1), 0, 3, 3, 2);
//        tryThese2Pieces(pieces.get(0), pieces.get(1));
        for (int i = 0; i < pieces.size(); i++) {
            for (int j = i + 1; j < pieces.size(); j++) {
                tryThese2Pieces(pieces.get(i), pieces.get(j));
//                tryThese2Pieces(pieces.get(0), pieces.get(2));
            }
        }
    }

    private static void tryThese2Pieces(Piece p0, Piece p1) throws InterruptedException {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                double score = tryHard(p0, p1, (i + 1) % 4, i, (j + 1) % 4, j);
            }
        }
    }

    private static List<Piece> filterPimples(List<Piece> pieces) throws Exception {
        double avg_perimeter = pieces.stream().mapToInt(p -> p.getImg().length).average().orElseThrow(() -> new Exception("WTF?? No perimeter of pieces exists??"));
        pieces = pieces.stream().filter(p -> p.getImg().length > avg_perimeter / 3).collect(Collectors.toList());
        return pieces;
    }

}
