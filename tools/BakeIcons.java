import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Rebuild the code-native pixel sprites as single, transparent, nearest-filtered textures. */
class BakeIcons {
    static final String[] EYE = {
        "................", "................", "................", ".....ssssss.....",
        "...sswwwwwwss...", "..swwwbbbbwwws..", ".swwbbccccbbwws.", "swwwbcddddcbwwws",
        "swwwbcddddcbwwws", ".swwbbccccbbwws.", "..swwwbbbbwwws..", "...sswwwwwwss...",
        ".....ssssss.....", "................", "................", "................"
    };
    static final String[] RESET = {
        "................", ".....pppppp.....", "...pphhhhhhpp...", "..phhh....hhhp..",
        ".phh........hhp.", ".ph..........hp.", "ph...........pp.", "ph.........pppp.",
        "ph..........ppp.", "ph...........pp.", ".ph.............", ".phh............",
        "..phhh....hhhp..", "...pphhhhhhpp...", ".....pppppp.....", "................"
    };
    static Graphics2D g;
    static void rect(int x, int y, int w, int h, int rgb) { g.setColor(new Color(rgb, true)); g.fillRect(x, y, w, h); }
    static BufferedImage canvas(int w, int h) {
        var image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB); g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF); return image;
    }
    static void pixels(String[] rows) {
        for (int y = 0; y < rows.length; y++) {
            if (rows[y].length() != 16) throw new IllegalStateException("Sprite row must contain 16 pixels: " + rows[y]);
            for (int x = 0; x < 16; x++) {
                int color = switch (rows[y].charAt(x)) {
                    case 'w' -> 0xFFECEAF7; case 's' -> 0xFF9B9DBD; case 'b' -> 0xFF76CEEE;
                    case 'c' -> 0xFF38A5D8; case 'd' -> 0xFF24587F; case 'p' -> 0xFFE67A32; case 'h' -> 0xFFFFCA73; default -> 0;
                };
                if (color != 0) rect(x, y, 1, 1, color);
            }
        }
    }
    static void save(Path root, String name, BufferedImage image) throws Exception {
        g.dispose(); ImageIO.write(image, "png", root.resolve(name + ".png").toFile());
    }
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]); Files.createDirectories(root);
        for (String name : new String[]{"eye", "reset", "display", "players", "setting_tracking", "setting_zeros", "setting_range", "setting_opponent"}) {
            BufferedImage image = canvas(16, 16);
            switch (name) {
                case "eye" -> pixels(EYE);
                case "reset" -> pixels(RESET);
                case "display" -> {
                    rect(1, 2, 14, 10, 0xFFB18C65); rect(2, 3, 12, 8, 0xFF303448);
                    rect(3, 4, 10, 6, 0xFF62B9DC); rect(7, 12, 2, 2, 0xFFBC986F); rect(4, 14, 8, 1, 0xFFDAC29B);
                }
                case "players" -> {
                    rect(2, 2, 5, 5, 0xFFF4BB74); rect(9, 3, 5, 5, 0xFFA6B9DA);
                    rect(1, 10, 7, 5, 0xFFF4BB74); rect(2, 9, 5, 1, 0xFFF4BB74);
                    rect(9, 11, 6, 4, 0xFFA6B9DA); rect(10, 10, 4, 1, 0xFFA6B9DA);
                }
                case "setting_tracking" -> {
                    // Power symbol: open ring and a bright central stem.
                    rect(7, 1, 2, 7, 0xFFFFCA73);
                    rect(3, 3, 2, 2, 0xFFE67A32); rect(11, 3, 2, 2, 0xFFE67A32);
                    rect(2, 5, 2, 6, 0xFFE67A32); rect(12, 5, 2, 6, 0xFFE67A32);
                    rect(3, 11, 2, 2, 0xFFFFCA73); rect(11, 11, 2, 2, 0xFFFFCA73);
                    rect(5, 13, 6, 2, 0xFFFFCA73);
                }
                case "setting_zeros" -> {
                    // A counter tile with a readable pixel zero.
                    rect(1, 1, 14, 14, 0xFFB18C65); rect(2, 2, 12, 12, 0xFF352A25);
                    rect(6, 4, 4, 2, 0xFFFFCA73); rect(6, 10, 4, 2, 0xFFFFCA73);
                    rect(4, 5, 2, 6, 0xFFFFCA73); rect(10, 5, 2, 6, 0xFFFFCA73);
                    rect(7, 7, 2, 2, 0xFFE67A32);
                }
                case "setting_range" -> {
                    // Two outward arrows above ruler marks.
                    rect(1, 5, 14, 2, 0xFFFFCA73);
                    rect(2, 4, 2, 4, 0xFFFFCA73); rect(3, 3, 2, 6, 0xFFFFCA73);
                    rect(12, 4, 2, 4, 0xFFFFCA73); rect(11, 3, 2, 6, 0xFFFFCA73);
                    rect(1, 12, 14, 2, 0xFFB18C65);
                    for (int x = 2; x <= 12; x += 3) rect(x, 10, 1, 3, 0xFFE67A32);
                }
                case "setting_opponent" -> {
                    // One player inside four targeting corners.
                    rect(6, 3, 4, 4, 0xFFFFCA73); rect(5, 9, 6, 4, 0xFFFFCA73);
                    rect(1, 1, 4, 2, 0xFFE67A32); rect(1, 1, 2, 4, 0xFFE67A32);
                    rect(11, 1, 4, 2, 0xFFE67A32); rect(13, 1, 2, 4, 0xFFE67A32);
                    rect(1, 13, 4, 2, 0xFFE67A32); rect(1, 11, 2, 4, 0xFFE67A32);
                    rect(11, 13, 4, 2, 0xFFE67A32); rect(13, 11, 2, 4, 0xFFE67A32);
                }
            }
            save(root, name, image);
        }
        for (String name : new String[]{"flag_de", "flag_en"}) {
            BufferedImage image = canvas(32, 24);
            rect(1, 3, 30, 19, 0xFF131521);
            if (name.equals("flag_de")) {
                rect(3, 5, 26, 5, 0xFF16171C); rect(3, 10, 26, 5, 0xFFE23138); rect(3, 15, 26, 5, 0xFFFFD53C);
            } else {
                for (int i = 0; i < 15; i++) rect(3, 5 + i, 26, 1, (i * 13 / 15) % 2 == 0 ? 0xFFE23B4A : 0xFFF1EFF5);
                rect(3, 5, 12, 8, 0xFF3D4CA5);
                for (int row = 0; row < 4; row++) for (int col = 0; col < 4; col++) rect(4 + col * 3 + row % 2, 6 + row * 2, 1, 1, 0xFFFFFFFF);
            }
            save(root, name, image);
        }
    }
}
