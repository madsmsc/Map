import java.awt.Color;
import java.awt.Paint;
import java.awt.TexturePaint;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class ColorScheme{
    private static final Color
	col7,
	col5,
	col4,
	col2,
	colW,
	colL, 
	colV;
    
    public static Paint water, land, visited, selected, border, text;

    static {
	col7 = new Color(0.77f, 0.77f, 0.77f);
	col5 = new Color(0.55f, 0.55f, 0.55f);
	col4 = new Color(0.44f, 0.44f, 0.44f);
	col2 = new Color(0.22f, 0.22f, 0.22f);
	colW = new Color(115.0f/255.0f, 151.0f/255.0f, 240.0f/255.0f);
	colL = new Color(95.0f/255.0f, 165.0f/255.0f, 114.0f/255.0f);
	colV = new Color(120.0f/255.0f, 185.0f/255.0f, 135.0f/255.0f);

	setGrey();
    }
    
    private ColorScheme(){

    }

    private static Paint getTexture(String s){
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

    public static void setTexture(){
	water = getTexture("data/water.jpg");
	land = getTexture("data/grass.jpg");
	visited = getTexture("data/grid.jpg");
	selected = Color.darkGray;
	border = Color.lightGray;
	text = Color.white;
    }	

    public static void setBlueGreen(){
	water = colW;
	land = colL;
	visited = colV;
	selected = Color.darkGray;
	border = Color.black;
	text = Color.white;
    }

    public static void setGrey(){
	water = col7;
	land = col5;
	visited = col4;
	selected = col2;
	border = col7;
	text = Color.white;
    }
}
