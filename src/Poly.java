import java.util.ArrayList;
import java.awt.geom.Point2D.Double;
import java.awt.geom.GeneralPath;

public class Poly{
  public int number, length, type;
  public double minX, minY, maxX, maxY;
  public ArrayList<Integer> parts;
  public ArrayList<Double> points;
  public ArrayList<GeneralPath> paths;
  public ArrayList<City> cities;
  public Poly(){
    parts = new ArrayList<Integer>();
    points = new ArrayList<Double>();
    center = new Double(0,0);
    paths = new ArrayList<GeneralPath>();
    cities = new ArrayList<City>();
  }
  public Double center;
  public String name;
}

class City{
    Double pos; // WGS84
    String name,info;
    public City(String name, double x, double y, String info){
	pos = new Double(x,y);
	this.name = name;
	this.info = info;
    }
}
