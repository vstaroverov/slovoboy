import java.awt.*;import java.awt.font.*;import java.awt.geom.*;import java.awt.image.*;import javax.imageio.*;import java.nio.file.*;
/** White brand treatment; mark and letter outlines are real vectors. */
public class BrandAssets {
 static final int BG=0xffffff,INK=0x061014,LIME=0xd8ff18,MUTED=0x79738e;
 static Path2D polygon(double... xy){Path2D p=new Path2D.Double();for(int i=0;i<xy.length;i+=2){if(i==0)p.moveTo(xy[i],xy[i+1]);else p.lineTo(xy[i],xy[i+1]);}p.closePath();return p;}
 static Shape accent(){return polygon(38,45,54,64,70,45,65,41,54,54,43,41);}
 static Shape mark(){Area p=new Area(polygon(54,15,64,39,90,42,70,60,76,87,54,73,32,87,38,60,18,42,44,39));p.subtract(new Area(accent()));return p;}
 static String svgPath(Shape shape){StringBuilder b=new StringBuilder();double[] c=new double[6];for(PathIterator it=shape.getPathIterator(null);!it.isDone();it.next()){int t=it.currentSegment(c);b.append("MLQCZ".charAt(t));int count=t<2?2:t==2?4:t==3?6:0;for(int i=0;i<count;i++)b.append(String.format(java.util.Locale.ROOT,"%.3f",c[i])).append(i%2==0?',':' ');}return b.toString();}
 static void paintMark(Graphics2D g){g.setColor(new Color(INK));g.fill(mark());g.setColor(new Color(LIME));g.fill(accent());}
 static void png(Path file,int size,boolean transparent)throws Exception{BufferedImage im=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);Graphics2D g=im.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.scale(size/108.0,size/108.0);if(!transparent){g.setColor(new Color(BG));g.fillRect(0,0,108,108);}paintMark(g);g.dispose();ImageIO.write(im,"png",file.toFile());}
 public static void main(String[] args)throws Exception{
  Path root=Paths.get(args[0]),brand=root.resolve("branding");Files.createDirectories(brand);
  String paths="<path fill=\"#061014\" d=\""+svgPath(mark())+"\"/><path fill=\"#d8ff18\" d=\""+svgPath(accent())+"\"/>";
  String transparent="<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 108 108\">"+paths+"</svg>";
  String icon="<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 108 108\"><rect width=\"108\" height=\"108\" rx=\"18\" fill=\"#ffffff\"/>"+paths+"</svg>";
  Files.writeString(brand.resolve("v-star-games-icon.svg"),icon);Files.writeString(brand.resolve("v-star-games-mark.svg"),transparent);Files.writeString(brand.resolve("v-star-games-mono.svg"),"<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 108 108\"><path fill=\"#061014\" d=\""+svgPath(mark())+"\"/></svg>");
  Font font=Font.createFont(Font.TRUETYPE_FONT,Paths.get(args[1]).toFile());FontRenderContext frc=new FontRenderContext(null,true,true);Shape title=font.deriveFont(Font.BOLD,42f).createGlyphVector(frc,"V-STAR GAMES").getOutline(150,77),subtitle=font.deriveFont(18f).createGlyphVector(frc,"by V-STAR Group").getOutline(152,111);
  String logo="<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 520 148\"><rect width=\"520\" height=\"148\" rx=\"18\" fill=\"#ffffff\"/><g transform=\"translate(20 20)\">"+paths+"</g><path fill=\"#061014\" d=\""+svgPath(title)+"\"/><path fill=\"#79738e\" d=\""+svgPath(subtitle)+"\"/></svg>";Files.writeString(brand.resolve("v-star-games-lockup.svg"),logo);
  BufferedImage preview=new BufferedImage(1040,296,BufferedImage.TYPE_INT_RGB);Graphics2D g=preview.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.scale(2,2);g.setColor(new Color(BG));g.fillRect(0,0,520,148);g.translate(20,20);paintMark(g);g.translate(-20,-20);g.setColor(new Color(INK));g.fill(title);g.setColor(new Color(MUTED));g.fill(subtitle);g.dispose();ImageIO.write(preview,"png",brand.resolve("v-star-games-lockup.png").toFile());
  for(String dir:new String[]{"docs","web/dist"}){Path dest=root.resolve(dir);Files.writeString(dest.resolve("icon.svg"),icon);Files.writeString(dest.resolve("brand-mark.svg"),transparent);for(int size:new int[]{192,512})png(dest.resolve("icon-"+size+".png"),size,false);}
  png(brand.resolve("v-star-games-mark.png"),512,true);
  Files.writeString(root.resolve("app/src/main/res/drawable/ic_launcher.xml"),"<vector xmlns:android=\"http://schemas.android.com/apk/res/android\" android:width=\"108dp\" android:height=\"108dp\" android:viewportWidth=\"108\" android:viewportHeight=\"108\"><path android:fillColor=\"#FFFFFF\" android:pathData=\"M0,0H108V108H0Z\"/><path android:fillColor=\"#061014\" android:pathData=\""+svgPath(mark())+"\"/><path android:fillColor=\"#D8FF18\" android:pathData=\""+svgPath(accent())+"\"/></vector>");
 }
}
