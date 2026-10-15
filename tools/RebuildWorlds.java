import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;
import java.io.*;
import java.nio.file.*;

/**
 * Deterministically reconstructs the transparent flight corridor of each
 * existing SkyPeep 2D world from its art palette and unobstructed side scenery.
 * Removes baked-in HUD, bird, obstacle and banded inpainting artifacts.
 * Requires JDK 17 only: no internet, generated chat images or other assets.
 */
class RebuildWorlds {
    private record Area(double x1,double y1,double x2,double y2) {
        double alpha(double x,double y,double feather) {
            double inside=Math.min(Math.min(x-x1,x2-x),Math.min(y-y1,y2-y));
            if(x>=x1&&x<=x2&&y>=y1&&y<=y2) return clamp(.5+inside/feather);
            double dx=Math.max(Math.max(x1-x,x-x2),0);
            double dy=Math.max(Math.max(y1-y,y-y2),0);
            return clamp(.5-Math.hypot(dx,dy)/feather);
        }
    }
    private static double clamp(double v){return Math.max(0,Math.min(1,v));}
    private static final int[][][] PALETTE = {
      {{35,163,236},{29,171,237},{112,202,245},{178,231,253}},
      {{38,153,236},{50,177,243},{119,207,245},{162,231,250}},
      {{19,149,236},{22,148,233},{92,185,242},{166,230,255}},
      {{38,157,237},{53,185,241},{126,208,246},{179,229,246}},
      {{16,23,125},{18,55,169},{57,104,214},{119,138,242}},
      {{255,157,98},{255,198,91},{255,221,115},{255,238,186}}
    };
    // 706 x 1254 world assets, with original artwork coordinates.
    private static final Area[][] MASKS = {
      {new Area(0,0,706,210),new Area(238,115,551,520),new Area(-20,433,574,765),new Area(220,698,574,1280)},
      {new Area(0,0,706,212),new Area(244,110,556,525),new Area(-20,429,606,771),new Area(238,707,587,1280)},
      {new Area(0,0,706,207),new Area(234,110,584,533),new Area(-20,421,612,773),new Area(223,703,591,1280)},
      {new Area(0,0,706,216),new Area(194,105,651,533),new Area(-20,424,687,771),new Area(223,697,590,1280),new Area(594,642,746,1100)},
      {new Area(0,0,706,207),new Area(233,103,580,528),new Area(-20,418,618,774),new Area(219,695,591,1280)},
      {new Area(0,0,706,210),new Area(277,106,574,534),new Area(-20,418,618,774),new Area(251,700,586,1280)}
    };
    private static final Area[][] COINS = {
      {}, {new Area(448,562,691,772)}, {},
      {new Area(393,553,689,721)},
      {new Area(339,560,694,751)}, {}
    };
    private static int mix(int a,int b,double t){return (int)Math.round(a+(b-a)*t);}
    private static Color cloudColor(int world,boolean shadow) {
        if(world==5) return shadow?new Color(70,82,179,42):new Color(185,199,255,68);
        if(world==6) return shadow?new Color(255,216,158,55):new Color(255,255,227,85);
        return shadow?new Color(129,209,243,43):new Color(255,255,255,91);
    }
    private static void cloud(Graphics2D g,int world,int cx,int cy,double scale) {
        // Union the lobes into one silhouette BEFORE applying transparency.
        // Alpha-compositing individual ellipses produced visibly repeated circles.
        double rx=110*scale,ry=53*scale;
        java.awt.geom.Area silhouette=new java.awt.geom.Area(
            new Ellipse2D.Double(cx-rx,cy-ry*.3,rx*2,ry*1.25));
        double[][] forms={{-.64,-.1,.75},{-.2,-.40,1.05},{.3,-.53,1.12},{.71,-.07,.80}};
        for(double[] f:forms){
            double r=f[2]*ry;
            silhouette.add(new java.awt.geom.Area(
                new Ellipse2D.Double(cx+f[0]*rx-r,cy+f[1]*ry-r,2*r,2*r)));
        }
        g.setColor(cloudColor(world,true));
        g.fill(silhouette.createTransformedArea(
            java.awt.geom.AffineTransform.getTranslateInstance(0,6*scale)));
        g.setColor(cloudColor(world,false));
        g.fill(silhouette);
    }

    private static void distantRidge(Graphics2D g,int world,int width,int height) {
        // Subtle silhouettes restore depth to previously damaged sky corridors.
        // Preserved side islands and waterfalls remain actual reference pixels.
        final boolean night=world==5, sunset=world==6;
        int[][] shades=night
            ?new int[][]{{51,82,187,38},{36,75,167,48}}
            :sunset?new int[][]{{239,148,102,43},{227,131,96,52}}
            :new int[][]{{77,160,224,43},{62,137,203,54}};
        for(int layer=0;layer<2;layer++){
            int horizon=(int)(height*(layer==0?.71:.80));
            double shift=world*24+layer*41;
            Path2D.Double line=new Path2D.Double();
            line.moveTo(-12,height);
            line.lineTo(-12,horizon+80);
            for(int x=-12;x<width+75;x+=68){
                double h=53+Math.abs(Math.sin(x*.014+shift*.1))*76;
                if(night)h*=.85;
                if(sunset)h*=1.20;
                line.lineTo(x+24,horizon-h);
                line.lineTo(x+69,horizon+9);
            }
            line.lineTo(width+12,height);
            line.closePath();
            int[] col=shades[layer];
            g.setColor(new Color(col[0],col[1],col[2],col[3]));
            g.fill(line);
        }
    }
    private static BufferedImage sky(int w,int h,int world) {
        BufferedImage out=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        int[] stops={0,(int)(h*.271),(int)(h*.654),h-1};
        int[][] c=PALETTE[world-1];
        for(int y=0;y<h;y++){
            int k=y<=stops[1]?0:y<=stops[2]?1:2;
            double t=(y-stops[k])/(double)Math.max(1,stops[k+1]-stops[k]);
            t=t*t*(3-2*t);
            g.setColor(new Color(mix(c[k][0],c[k+1][0],t),
                mix(c[k][1],c[k+1][1],t),mix(c[k][2],c[k+1][2],t)));
            g.drawLine(0,y,w,y);
        }
        distantRidge(g,world,w,h);
        int[][] clouds={{166,285,65},{414,385,38},{290,865,70},
            {498,1007,65},{185,1115,47},{421,635,30}};
        for(int[] cdata:clouds)cloud(g,world,cdata[0],cdata[1],cdata[2]/100.);
        g.dispose();
        return out;
    }
    private static void rebuild(Path file,int world) throws Exception {
        BufferedImage original=ImageIO.read(file.toFile());
        if(original==null)throw new IOException("Unreadable SkyPeep artwork: "+file);
        int w=original.getWidth(),h=original.getHeight();
        if(w!=706||h!=1254)throw new IOException("Unexpected art size: "+w+"x"+h);
        BufferedImage air=sky(w,h,world);
        BufferedImage output=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            double amount=0;
            for(Area a:MASKS[world-1]) amount=Math.max(amount,a.alpha(x,y,47));
            for(Area a:COINS[world-1]) amount=Math.max(amount,a.alpha(x,y,28));
            int from=original.getRGB(x,y),to=air.getRGB(x,y);
            int r=mix((from>>>16)&255,(to>>>16)&255,amount),
                g=mix((from>>>8)&255,(to>>>8)&255,amount),
                b=mix(from&255,to&255,amount);
            output.setRGB(x,y,(r<<16)|(g<<8)|b);
        }
        ImageWriter writer=ImageIO.getImageWritersByFormatName("jpeg").next();
        try(OutputStream o=Files.newOutputStream(file);
            ImageOutputStream stream=ImageIO.createImageOutputStream(o)) {
            writer.setOutput(stream);
            ImageWriteParam options=writer.getDefaultWriteParam();
            options.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            options.setCompressionQuality(.91f);
            writer.write(null,new IIOImage(output,null,null),options);
        } finally {writer.dispose();}
        if(Files.size(file)<20000)throw new IOException("Invalid clean art size: "+file);
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=1) throw new IllegalArgumentException(
            "Usage: java -Djava.awt.headless=true tools/RebuildWorlds.java <assets-directory>");
        for(int level=1;level<=6;level++){
            Path path=Path.of(args[0],"bg_"+level+".jpg");
            rebuild(path,level);
            System.out.println("CLEAN WORLD "+level+" "+Files.size(path)+" bytes");
        }
    }
}
