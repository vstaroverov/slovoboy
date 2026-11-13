import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;

/** Original geometric icon; same paths as Android ic_launcher.xml. Also crops raw device screenshots. */
public class IconAssets {
    public static void main(String[] args) throws Exception {
        if(args[0].equals("crop")) {
            BufferedImage input=ImageIO.read(new File(args[1]));
            BufferedImage crop=input.getSubimage(0,Integer.parseInt(args[3]),input.getWidth(),input.getHeight()-Integer.parseInt(args[3])-Integer.parseInt(args[4]));
            ImageIO.write(crop,"png",new File(args[2]));return;
        }
        BufferedImage image=new BufferedImage(512,512,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.scale(512.0/108,512.0/108);
        g.setColor(new Color(0x7051df));g.fillRect(0,0,108,108);
        g.setColor(new Color(0x5032b0));g.fill(new RoundRectangle2D.Float(18,28,48,48,18,18));g.fill(new RoundRectangle2D.Float(44,44,47,48,18,18));
        g.setColor(Color.WHITE);g.fill(new RoundRectangle2D.Float(18,24,48,48,18,18));
        g.setColor(new Color(0xe0f4eb));g.fill(new RoundRectangle2D.Float(44,40,47,48,18,18));
        g.setStroke(new BasicStroke(4,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        Path2D c=new Path2D.Float();c.moveTo(49,38);c.curveTo(35,31,29,56,42,58);c.curveTo(45,59,48,58,50,56);
        g.setColor(new Color(0x7051df));g.draw(c);
        Path2D b=new Path2D.Float();b.moveTo(76,52);b.lineTo(58,52);b.lineTo(58,76);b.lineTo(67,76);b.curveTo(80,76,80,63,67,63);b.lineTo(58,63);
        g.setColor(new Color(0x23896a));g.draw(b);g.dispose();
        ImageIO.write(image,"png",new File(args[0]));
    }
}
