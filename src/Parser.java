import java.awt.geom.Point2D.Double;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class Parser{
  public ArrayList<Poly> polys;
  public Double xrange, yrange;

  public Parser(){
    byte[] data = readBytes("data/large.shp");
    ArrayList<String> names = readStrings("data/names.txt");
    ArrayList<String> visited = readStrings("data/visited.txt");
    ArrayList<String> info = parseVisited(visited);
    
    ByteBuffer bb = ByteBuffer.wrap(data);
    parseHeader(bb);
    polys = parseObjects(bb);
    postParsing(polys, names, visited, info);
  }

  private ArrayList<String> parseVisited(ArrayList<String> visited){
    ArrayList<String> info = new ArrayList<String>();
    ArrayList<String> tmp = new ArrayList<String>();
    String vstr = "", res = "";
    for(String str : visited)
	vstr += str;
    String[] countries = vstr.split("#");
    for(String country : countries){
	if(country.equals(countries[0])) 
	    continue; // nothing before first hash
	String[] cities = country.split("\\|");
	tmp.add(cities[0].trim());
	info.add(country);
    }
    visited.clear();
    for(String s : tmp)
	visited.add(s);
    return info;
  }

  private byte[] readBytes(String name){
    byte[] data = null;
    try{
      File f = new File(name);
      data = new byte[(int) f.length()];
      DataInputStream s = 
	new DataInputStream(new FileInputStream(f));
      s.readFully(data);
      s.close();
    }catch(Exception e){
      e.printStackTrace();
      System.exit(1);
    }
    return data;
  }

  private ArrayList<String> readStrings(String file){
    ArrayList<String> A = new ArrayList<String>();
    String line; 
    try{
      File f = new File(file);
      BufferedReader br = new BufferedReader(new FileReader(f));
      while ((line = br.readLine()) != null)
	A.add(line);
    }catch(Exception e){
      e.printStackTrace();
      System.exit(1);
    }
    return A;
  }

  void parseHeader(ByteBuffer bb){
    int fileCode = bb.getInt();
    for(int i = 0; i < 5; i++)
      bb.getInt(); // 5 unused ints
    int fileLength = bb.getInt();
    bb.order(ByteOrder.LITTLE_ENDIAN); // changes from big to small
    int version = bb.getInt();
    int shapeType = bb.getInt();
    double minX = bb.getDouble();
    double minY = bb.getDouble();
    double maxX = bb.getDouble();
    double maxY = bb.getDouble();
    double minZ = bb.getDouble();
    double maxZ = bb.getDouble();
    double minM = bb.getDouble();
    double maxM = bb.getDouble();
    xrange = new Double(minX, maxX);
    yrange = new Double(minY, maxX);
    System.out.println("File length: " + fileLength +
		       "\nVersion: " + version +
		       "\nShape type: " + shapeType +
		       "\nMBR: "+(int)minX+", "+(int)minY+", "+
		       (int)maxX+", "+(int)maxY);
  }

  ArrayList<Poly> parseObjects(ByteBuffer bb){
    ArrayList<Poly> polys = new ArrayList<Poly>();
    for(int r = 0; r < 100000; r++){
      try{
	Poly poly = new Poly();
	poly.number = bb.getInt();
	poly.length = bb.getInt();
	poly.type = bb.getInt();
	poly.minX = bb.getDouble();
	poly.minY = bb.getDouble();
	poly.maxX = bb.getDouble();
	poly.maxY = bb.getDouble();
	int parts = bb.getInt();
	int points = bb.getInt();
	for(int i = 0; i < parts; i++)
	    poly.parts.add(bb.getInt());
	for(int i = 0; i < points; i++){
	    double x = bb.getDouble();
	    double y = bb.getDouble();
	    poly.points.add(new Double(x,y));
	    poly.center.x += x;
	    poly.center.y += y;
	}
	poly.center.x /= points;
	poly.center.y /= points;
	polys.add(poly);
	// try to parse everyhing as a polyline/polygon
      }catch(Exception e){}
    }
    System.out.println(polys.size() + " objects read (max 100k)");
    return polys;
  }

  void postParsing(ArrayList<Poly> polys, 
		   ArrayList<String> names,
		   ArrayList<String> visited,
		   ArrayList<String> info){
    if(polys.size() != names.size()){
      System.out.println("Shapefile does not match names.txt.");
      System.exit(1);
    }
    // set name and visited for all polys
    for(int i = 0; i < polys.size(); i++){
	polys.get(i).name = names.get(i);
	int index = visited.indexOf(names.get(i));
	if(index != -1){
	    String cities[] = info.get(index).split("\\|");
	    for(String city : cities){
		if(city.equals(cities[0]))
		    continue;
		String str[] = city.split("\\*");
		if(str.length == 4){
		    double x = java.lang.Double.parseDouble(str[1]);
		    double y = java.lang.Double.parseDouble(str[2]);
		    polys.get(i).cities.add
			(new City(str[0], x, y, str[3]));
		}else if(str.length == 2){
		    polys.get(i).cities.add
			(new City(str[0], 0, 0, str[1]));
		}
	    }
	}
    }
    // check all records are of the same type
    boolean sameType = true;
    int whichType = polys.get(0).type;
    for(int i = 0; i < polys.size(); i++)
      if(polys.get(i).type != whichType)
	sameType = false;
    // print info
    System.out.println("Are all objects of type " + whichType +
		       "? " + (sameType?"YES":"NO"));
    System.out.println("> starting gui.");
  }
}
