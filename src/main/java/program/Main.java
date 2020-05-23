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
        BufferedImage img = ImageIO.read(new File("photos/resized3.jpg"));
        List<Piece> pieces = ImageProcessingUtils.getPiecesFromImage(img);
        pieces = filterPimples(pieces);

        pieces.forEach(Piece::walk);
        pieces.forEach(Piece::find_corner_points_based_on_sum_d2_length);
        pieces.forEach(Piece::draw_with_corners);
        pieces.forEach(Piece::compute_steps_between_corners);

        tryHard(pieces.get(0), pieces.get(1), 0, 3, 3, 2);
    }

    private static List<Piece> filterPimples(List<Piece> pieces) throws Exception {
        double avg_perimeter = pieces.stream().mapToInt(p -> p.getImg().length).average().orElseThrow(() -> new Exception("WTF?? No perimeter of pieces exists??"));
        pieces = pieces.stream().filter(p -> p.getImg().length > avg_perimeter / 3).collect(Collectors.toList());
        return pieces;
    }

}
