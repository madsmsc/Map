import java.awt.Color;
import java.awt.Paint;
import java.awt.TexturePaint;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class ColorScheme{
    public Paint water, land, visited, selected, border, text;
    public ColorScheme(){
	setGrey();
    }

    private Paint getTexture(String s){
	double scale = 0.2;
	TexturePaint tp = null;
	try{
	    BufferedImage bi = ImageIO.read(new File(s));
	    Rectangle2D rec = new Rectangle2D.
		Double(0, 0, bi.getWidth()*scale, bi.getHeight()*scale);
	    tp = new TexturePaint(bi, rec);
	}catch(Exception e){
	    e.printStackTrace();
	}
	return tp;
    }

    public void setTexture(){
	water = getTexture("data/water.jpg");
	land = getTexture("data/grass.jpg");
	visited = new Color(0.11f, 0.32f, 0.19f);
	selected = Color.darkGray;
	border = Color.lightGray;
	text = Color.white;
    }	

    public void setBlueGreen(){
	water = new Color(115.0f/255.0f, 151.0f/255.0f, 240.0f/255.0f);
	land = new Color(95.0f/255.0f, 165.0f/255.0f, 114.0f/255.0f);
	visited = new Color(120.0f/255.0f, 185.0f/255.0f, 135.0f/255.0f);
	selected = Color.darkGray;
	border = Color.black;
	text = Color.white;
    }

    public void setGrey(){
	water = new Color(0.77f, 0.77f, 0.77f);
	land = new Color(0.55f, 0.55f, 0.55f);
	visited = new Color(0.44f, 0.44f, 0.44f);
	selected = new Color(0.22f, 0.22f, 0.22f);
	border = new Color(0.77f, 0.77f, 0.77f);
	text = Color.white;
    }
}
