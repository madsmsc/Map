import java.util.ArrayList;
import java.awt.geom.Point2D.Double;
import java.awt.geom.GeneralPath;

public class Poly{
    private static final String NOT_VISITED =
	"<font size='3' face='Verdana'>%s... never been there.</font>";
    private static final String VISITED =
	"<font size='3' face='Verdana'><font size='6'><b>%s</b></font><br/><br/>";
    public int number, length, type;
    public double minX, minY, maxX, maxY;
    public ArrayList<Integer> parts;
    public ArrayList<Double> points;
    public ArrayList<GeneralPath> paths;
    public ArrayList<City> cities;
    public Double center;
    public String name;
    
    public Poly() {
	parts = new ArrayList<Integer>();
	points = new ArrayList<Double>();
	center = new Double(0,0);
	paths = new ArrayList<GeneralPath>();
	cities = new ArrayList<City>();
    }

    public String toString(){
	if(cities.isEmpty()) {
	    return String.format(NOT_VISITED, name);
	}
	StringBuilder sb = new StringBuilder();
	sb.append(String.format(VISITED, name));
	for(City c : cities){
	    sb.append("<b>");
	    sb.append(c.name);
	    sb.append(":</b> ");
	    sb.append(c.info);
	    sb.append("<br/>");
	}
	sb.append("</font>");
	return sb.toString();
    }
}

