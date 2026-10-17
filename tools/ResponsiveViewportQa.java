import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 * Validates REAL Android emulator screenshots from four physical viewports,
 * not a resized desktop mockup. Central reference art must remain legible
 * and aligned, and the extra device area must contain actual painted art,
 * never FitViewport's empty coloured/black letterbox.
 */
class ResponsiveViewportQa {
    private static final int DESIGN_W = 941;
    private static final int DESIGN_H = 1672;
    private static final int[][] DEVICES = {
        {1080,2400}, {720,1280}, {1200,1920}, {1600,2000}
    };
    private static BufferedImage image(Path file) throws IOException {
        if(!Files.isRegularFile(file))throw new IOException("Missing real Android screen: "+file);
        BufferedImage result=ImageIO.read(file.toFile());
        if(result==null)throw new IOException("Unreadable Android screenshot "+file);
        return result;
    }
    private static int difference(int a,int b) {
        return Math.abs(((a>>>16)&255)-((b>>>16)&255))
            +Math.abs(((a>>>8)&255)-((b>>>8)&255))
            +Math.abs((a&255)-(b&255));
    }
    private static int point(BufferedImage img,int x,int y) {
        return img.getRGB(Math.max(0,Math.min(img.getWidth()-1,x)),
                          Math.max(0,Math.min(img.getHeight()-1,y)));
    }
    public static void main(String[] args) throws Exception {
        BufferedImage reference=image(Path.of("assets/previews/menu.jpg"));
        StringBuilder text=new StringBuilder("Real SkyPeep Android multi-resolution UI audit\n");
        for(int[] dimensions:DEVICES){
            String label=dimensions[0]+"x"+dimensions[1];
            BufferedImage menu=image(Path.of("smoke-results/responsive-menu-"+label+".png"));
            BufferedImage flight=image(Path.of("smoke-results/responsive-gameplay-"+label+".png"));
            BufferedImage pause=image(Path.of("smoke-results/responsive-pause-"+label+".png"));
            for(BufferedImage capture:new BufferedImage[]{menu,flight,pause}){
                if(capture.getWidth()!=dimensions[0] ||
                   capture.getHeight()!=dimensions[1])
                    throw new IOException("Wrong real device dimensions: "+label);
            }
            double scale=Math.min(menu.getWidth()/(double)DESIGN_W,
                                  menu.getHeight()/(double)DESIGN_H);
            int coreW=(int)Math.round(DESIGN_W*scale);
            int coreH=(int)Math.round(DESIGN_H*scale);
            int marginX=(menu.getWidth()-coreW)/2;
            int marginY=(menu.getHeight()-coreH)/2;
            // Compare the centre-fitted unmodified MENU photograph, not the
            // moving bird/HUD and not the synthetic decorative bleed strips.
            long difference=0,channels=0;
            for(int y=48;y<reference.getHeight()-48;y+=6)
                for(int x=32;x<reference.getWidth()-32;x+=6){
                    int sx=marginX+(int)Math.round(
                        ((x+.5)/reference.getWidth())*coreW);
                    int sy=marginY+(int)Math.round(
                        ((y+.5)/reference.getHeight())*coreH);
                    difference+=difference(point(menu,sx,sy),
                        reference.getRGB(x,y));
                    channels+=3;
                }
            double mae=difference/(double)Math.max(1,channels);
            text.append(String.format(Locale.ROOT,
                "%s | menu MAE=%.2f/255 | core=%dx%d | margins=%dx%d%n",
                label,mae,coreW,coreH,marginX,marginY));
            if(mae>28.0)
                throw new IOException("MENU artwork distorted/unaligned on "+label+
                    ": MAE="+mae);
            // Compare art edge pixels to the NEW overdraw area. FitViewport
            // clearing the bars cannot pass this continuity check reliably.
            if(marginY>24){
                int x=menu.getWidth()/2;
                if(difference(point(menu,x,marginY/2),
                              point(menu,x,marginY+2))>65)
                    throw new IOException("Empty/incorrect TOP margin on "+label);
                if(difference(point(menu,x,menu.getHeight()-marginY/2-1),
                              point(menu,x,menu.getHeight()-marginY-3))>65)
                    throw new IOException("Empty/incorrect BOTTOM margin on "+label);
            }
            if(marginX>24){
                int y=menu.getHeight()/2;
                if(difference(point(menu,marginX/2,y),
                              point(menu,marginX+2,y))>75)
                    throw new IOException("Empty/incorrect LEFT margin on "+label);
                if(difference(point(menu,menu.getWidth()-marginX/2-1,y),
                              point(menu,menu.getWidth()-marginX-3,y))>75)
                    throw new IOException("Empty/incorrect RIGHT margin on "+label);
            }
        }
        Path output=Path.of("smoke-results/responsive-viewport-audit.txt");
        Files.writeString(output,text.toString(),StandardCharsets.UTF_8);
        System.out.print(text);
    }
}
