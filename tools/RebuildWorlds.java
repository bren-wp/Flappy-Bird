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
    /** Clears cropped screenshot header fragments without changing source artwork. */
    private static void removePillarHeader(Path assets,int world) throws IOException {
        Path file=assets.resolve("pillar_"+world+"_top.png");
        BufferedImage original=ImageIO.read(file.toFile());
        if(original==null)throw new IOException("Missing illustrated pillar: "+file);
        BufferedImage image=new BufferedImage(original.getWidth(),original.getHeight(),
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();
        g.drawImage(original,0,0,null);
        g.dispose();
        // Per-image crop thresholds checked against the supplied six top sprites.
        // Preserve the original cap, leaves, clouds, crystals and gold emblems.
        int[][] trim={{0,0},{166,35},{232,20},{199,26},{0,0},{298,16},{0,0}};
        int width=Math.min(image.getWidth(),trim[world][0]);
        int height=Math.min(image.getHeight(),trim[world][1]);
        for(int y=0;y<height;y++)for(int x=0;x<width;x++)
            image.setRGB(x,y,0);
        if(!ImageIO.write(image,"png",file.toFile()))
            throw new IOException("PNG writer unavailable: "+file);
        if(width>0 && ((ImageIO.read(file.toFile()).getRGB(0,0)>>>24)!=0))
            throw new IOException("Header fragment remains in "+file);
        if(Files.size(file)<5000)
            throw new IOException("Truncated illustrated pillar: "+file);
    }


    /**
     * Remove the hard-coded '63' and '4' from the six original wallet plates.
     * Interpolate within carefully measured navy-field rectangles, preserving
     * every pixel outside the ROI: gold coin, magenta gem, borders and corners.
     * Source artwork stays immutable in the SHA-256 validated ZIP.
     */
    private static void cleanHudCounters(Path assets,int world) throws IOException {
        final int[][] left={
            {132,30,212,91},{113,30,206,105},{119,29,209,105},
            {103,30,193,92},{113,29,209,95},{127,30,209,91}
        };
        final int[][] right={
            {108,30,160,93},{106,30,165,102},{106,30,171,102},
            {110,30,162,95},{112,30,181,94},{111,30,161,95}
        };
        for(int side=0;side<2;side++){
            String name=side==0?"left":"right";
            int[] box=(side==0?left:right)[world-1];
            Path path=assets.resolve("hud_"+name+"_"+world+".png");
            BufferedImage input=ImageIO.read(path.toFile());
            int width=side==0?269:191, height=side==0?113:115;
            if(input==null || input.getWidth()!=width || input.getHeight()!=height)
                throw new IOException("Unexpected HUD crop size for "+path);
            BufferedImage result=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
            for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
                int original=input.getRGB(x,y);
                int color=original;
                if(x>=box[0] && x<box[2] && y>=box[1] && y<box[3]) {
                    double blend=Math.min(1.,Math.min(
                        Math.min((x-box[0])/5.,(box[2]-1-x)/5.),
                        Math.min((y-box[1])/4.,(box[3]-1-y)/4.)));
                    if(blend>0) {
                        double fraction=(x-box[0]+1.)/(box[2]-box[0]+1.);
                        int a=input.getRGB(box[0]-1,y),b=input.getRGB(box[2],y);
                        int r=mix((a>>>16)&255,(b>>>16)&255,fraction);
                        int g=mix((a>>>8)&255,(b>>>8)&255,fraction);
                        int blue=mix(a&255,b&255,fraction);
                        // Worlds 2/3/5 have ornate gem borders intersecting
                        // horizontal samples. A stable navy-field source
                        // avoids cyan/magenta bands across cleaned digits.
                        if(side==1 && (world==2 || world==3 || world==5)){
                            int sampleX=world==2?160:(world==3?165:174);
                            int sampleY=Math.min(87,Math.max(36,y));
                            int navy=input.getRGB(sampleX,sampleY);
                            r=(navy>>>16)&255;
                            g=(navy>>>8)&255;
                            blue=navy&255;
                        }
                        int outR=mix((original>>>16)&255,r,blend);
                        int outG=mix((original>>>8)&255,g,blend);
                        int outB=mix(original&255,blue,blend);
                        color=(original&0xFF000000)|(outR<<16)|(outG<<8)|outB;
                    }
                }
                result.setRGB(x,y,color);
            }
            // Pixel-by-pixel identity outside the intentional text region.
            for(int y=0;y<height;y++)for(int x=0;x<width;x++)
                if((x<box[0]||x>=box[2]||y<box[1]||y>=box[3])
                    && input.getRGB(x,y)!=result.getRGB(x,y))
                    throw new IOException("HUD icon or border modified: "+path);
            if(!ImageIO.write(result,"png",path.toFile()))
                throw new IOException("PNG encoder missing: "+path);
            if(Files.size(path)<10000)
                throw new IOException("HUD output suspiciously small: "+path);
        }
    }


    /**
     * Render a blank, world-matched score face onto the original winged header.
     * Unlike the prior generic in-game badge, the six faces are built once and
     * retain all untouched illustration pixels (wings, ribbons, level names).
     * Source PNGs remain immutable: only Gradle's generated assets are changed.
     */
    private static void cleanScoreHeader(Path assets,int world) throws IOException {
        Path file=assets.resolve("hud_score_"+world+".png");
        BufferedImage original=ImageIO.read(file.toFile());
        if(original==null || original.getWidth()!=367 || original.getHeight()!=210)
            throw new IOException("Unexpected illustrated score header: "+file);
        BufferedImage cleaned=new BufferedImage(367,210,BufferedImage.TYPE_INT_ARGB);
        // Graphics2D.drawImage() composites semi-transparent source pixels,
        // changing exact ARGB values even outside the painted score region.
        // A literal copy preserves those original edge/wing pixels bit-for-bit.
        for(int yy=0;yy<210;yy++)for(int xx=0;xx<367;xx++)
            cleaned.setRGB(xx,yy,original.getRGB(xx,yy));
        Graphics2D g=cleaned.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        final int[][] bounds={
            {91,27,276,150},{106,28,264,143},{106,31,267,135},
            {108,29,261,122},{120,27,257,116},{105,24,272,146}
        };
        // The supplied reference HUD uses a bright winged/cloud score face,
        // not a dark cyan/blue rectangle. Keep the reference's original wings
        // and ribbon pixels untouched, and replace only the baked-in score area.
        final int[][] top={
            {254,255,255},{255,255,255},{250,255,255},
            {246,255,255},{245,241,255},{255,254,239}
        };
        final int[][] bottom={
            {200,238,255},{222,248,255},{191,232,255},
            {212,240,255},{213,215,255},{255,226,164}
        };
        final int[][] edges={
            {12,69,125},{15,71,128},{8,66,139},
            {15,75,128},{46,41,127},{143,74,33}
        };
        int[] b=bounds[world-1], a=top[world-1], c=bottom[world-1], e=edges[world-1];
        int x=b[0],y=b[1],w=b[2]-b[0],h=b[3]-b[1];
        RoundRectangle2D.Float face=new RoundRectangle2D.Float(x,y,w,h,36,36);
        g.setPaint(new GradientPaint(x,y,new Color(a[0],a[1],a[2]),x,y+h,
            new Color(c[0],c[1],c[2])));
        g.fill(face);
        g.setStroke(new BasicStroke(3.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g.setColor(new Color(e[0],e[1],e[2]));
        g.draw(face);
        // Low-opacity cloud crowns on the light surface give the score the
        // rounded reference treatment without redrawing any outside pixels.
        g.setColor(new Color(255,255,255,135));
        g.fill(new Ellipse2D.Float(x+10,y+11,w*.41f,h*.47f));
        g.fill(new Ellipse2D.Float(x+w*.33f,y+6,w*.40f,h*.55f));
        g.fill(new Ellipse2D.Float(x+w*.63f,y+14,w*.28f,h*.42f));
        g.setColor(new Color(255,255,255,195));
        g.setStroke(new BasicStroke(1.8f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g.draw(new Arc2D.Float(x+11,y+8,w-22,52,192,155,Arc2D.OPEN));
        g.dispose();
        // Safety invariant: changing any part of the original wings, ribbon,
        // level-name art or transparent crop OUTSIDE this inset fails the build.
        for(int yy=0;yy<210;yy++)for(int xx=0;xx<367;xx++)
            if((xx<x-3 || xx>b[2]+3 || yy<y-3 || yy>b[3]+3) &&
                original.getRGB(xx,yy)!=cleaned.getRGB(xx,yy))
                throw new IOException("Score header changed outside inset: "+file);
        if(!ImageIO.write(cleaned,"png",file.toFile()) || Files.size(file)<28000)
            throw new IOException("Invalid generated score header: "+file);
    }

    public static void main(String[] args) throws Exception {
        if(args.length!=1) throw new IllegalArgumentException(
            "Usage: java -Djava.awt.headless=true tools/RebuildWorlds.java <assets-directory>");
        for(int level=1;level<=6;level++){
            Path path=Path.of(args[0],"bg_"+level+".jpg");
            rebuild(path,level);
            removePillarHeader(Path.of(args[0]),level);
            cleanHudCounters(Path.of(args[0]),level);
            cleanScoreHeader(Path.of(args[0]),level);
            System.out.println("CLEAN WORLD "+level+" "+Files.size(path)+" bytes; wallets and score face restored");
        }
    }
}
