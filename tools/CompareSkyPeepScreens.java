import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * Compare real emulator captures against the checked-in, user-provided
 * reference illustrations. Metrics are diagnostic and never imply 1:1 parity.
 * The viewport is center-fitted to the reference without distortion.
 */
class CompareSkyPeepScreens {
    private static final String[][] SCENES = {
        {"menu", "menu.png", "menu.jpg"},
        {"levels", "levels.png", "levels.jpg"},
        {"skins", "skins.png", "skins.jpg"},
        {"world-1", "world-1.png", "gameplay.jpg"}
    };
    private static BufferedImage load(Path path) throws IOException {
        if (!Files.isRegularFile(path)) throw new IOException("Missing screenshot/reference: " + path);
        BufferedImage img = ImageIO.read(path.toFile());
        if (img == null || img.getWidth() < 100 || img.getHeight() < 100)
            throw new IOException("Invalid screenshot/reference: " + path);
        return img;
    }
    private static BufferedImage fit(BufferedImage capture, int w, int h) {
        double scale = Math.min(capture.getWidth() / (double) w, capture.getHeight() / (double) h);
        int width = Math.min(capture.getWidth(), (int) Math.round(w * scale));
        int height = Math.min(capture.getHeight(), (int) Math.round(h * scale));
        int x = (capture.getWidth() - width) / 2;
        int y = (capture.getHeight() - height) / 2;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = img.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(capture, 0, 0, w, h, x, y, x + width, y + height, null);
        graphics.dispose();
        return img;
    }
    private static double meanAbsoluteError(BufferedImage actual, BufferedImage reference) {
        long difference = 0;
        long count = 0;
        for (int y = 0; y < reference.getHeight(); y += 4) {
            for (int x = 0; x < reference.getWidth(); x += 4) {
                int a = actual.getRGB(x, y);
                int b = reference.getRGB(x, y);
                difference += Math.abs(((a >> 16) & 255) - ((b >> 16) & 255));
                difference += Math.abs(((a >> 8) & 255) - ((b >> 8) & 255));
                difference += Math.abs((a & 255) - (b & 255));
                count += 3;
            }
        }
        return difference / (double) count;
    }
    // Sample a fixed, unoccluded illustrated island in Sky Start. The old
    // overwide pillar mask erased this region despite no bird or pillar being
    // present there. This is reference-to-real-emulator evidence, not an image.
    private static double regionMAE(
        BufferedImage actual, BufferedImage reference,
        double left, double top, double right, double bottom) {
        int x0=(int)(left*reference.getWidth());
        int x1=(int)(right*reference.getWidth());
        int y0=(int)(top*reference.getHeight());
        int y1=(int)(bottom*reference.getHeight());
        long diff=0,count=0;
        for(int y=y0;y<y1;y+=2)for(int x=x0;x<x1;x+=2){
            int a=actual.getRGB(x,y),b=reference.getRGB(x,y);
            for(int shift:new int[]{0,8,16}){
                diff+=Math.abs(((a>>>shift)&255)-((b>>>shift)&255));
                count++;
            }
        }
        return diff/(double)Math.max(1,count);
    }
    public static void main(String[] args) throws Exception {
        Path output = Path.of("smoke-results/visual-comparison.txt");
        StringBuilder report = new StringBuilder();
        report.append("SkyPeep reference-vs-emulator diagnostic (current Android CI build)\n")
              .append("MAE: mean absolute RGB channel difference (0=exact, 255=max)\n")
              .append("Captured pixels are center-fitted to illustration aspect ratio.\n")
              .append("Dynamic HUD, bird, gameplay and scaled text may differ legitimately.\n")
              .append("No automated 1:1 visual-parity claim is made from this metric.\n\n");
        for (String[] scene : SCENES) {
            BufferedImage screenshot = load(Path.of("smoke-results", scene[1]));
            BufferedImage reference = load(Path.of("assets/previews", scene[2]));
            BufferedImage adjusted = fit(screenshot, reference.getWidth(), reference.getHeight());
            double error = meanAbsoluteError(adjusted, reference);
            report.append(String.format(java.util.Locale.ROOT,
                "%s | MAE=%.2f/255 | screenshot=%dx%d | reference=%dx%d%n",
                scene[0], error, screenshot.getWidth(), screenshot.getHeight(),
                reference.getWidth(), reference.getHeight()));
            if("world-1".equals(scene[0])){
                double islandError=regionMAE(adjusted,reference,.33,.23,.45,.35);
                report.append(String.format(java.util.Locale.ROOT,
                    "sky-start-original-midground-island | ROI MAE=%.2f/255%n",
                    islandError));
                // This metric is diagnostic until the revised masks are
                // verified against the actual emulator capture.
            }
        }
        // Real Android screenshot guard, not just a generated-asset check.
        // Sample an unobstructed interior point on the winged score face; it
        // must remain pale in each of the six individually themed worlds.
        // The X/Y fractions are viewport-independent for portrait screens.
        for (int world=1;world<=6;world++) {
            BufferedImage screenshot=load(Path.of("smoke-results","world-"+world+".png"));
            // Probe the left, lower interior of the rendered score face.
            // The original white cloud crowns live higher up, while the
            // dynamic numeral is centered horizontally. These four samples
            // test the actual background without intersecting either.
            int minimum=255, total=0, count=0;
            for(double fx:new double[]{.445,.453})
                for(double fy:new double[]{.065,.066}) {
                    int x=(int)(screenshot.getWidth()*fx);
                    int y=(int)(screenshot.getHeight()*fy);
                    int rgb=screenshot.getRGB(x,y);
                    int luminance=(((rgb>>>16)&255)*299
                        +((rgb>>>8)&255)*587+(rgb&255)*114)/1000;
                    minimum=Math.min(minimum,luminance);
                    total+=luminance;
                    count++;
                }
            int average=total/count;
            report.append("world-").append(world).append(
                " | score-face base minimum=").append(minimum)
                .append("/255 average=").append(average).append("/255\n");
            if(minimum<180 || average<195)throw new IOException(
                "Dark score-face base in world "+world+
                " (minimum="+minimum+", average="+average+"). Check screenshot.");
        }
        Files.writeString(output, report.toString(), StandardCharsets.UTF_8);
        System.out.print(report);
    }
}
