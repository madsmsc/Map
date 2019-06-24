import java.util.ArrayList;
import java.awt.geom.Point2D.Double;
import java.awt.geom.GeneralPath;

class City {
    Double pos; // WGS84
    String name,info;
    public City(String name, double x, double y, String info){
	pos = new Double(x,y);
	this.name = name;
	this.info = info;
    }
}
