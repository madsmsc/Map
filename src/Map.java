/* TODO:
can names.txt hold label positions for country names?
  then they could be placed more appropriately.
implement images somehow
  how much space is needed?
    this is probably not feasible, unless thumbnails are used
  get images from parents and Marcus.
*/

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.imageio.*;
import java.awt.geom.Point2D.Double;

public class Map extends Canvas{
    private boolean init = true;
    private ArrayList<Poly> polys;
    private MyListener ml;
    private Parser parser;
    public AffineTransform coordTransform;
    public BufferStrategy bs;
    public String closestCountry;
    public ColorScheme cs;

    public Map(Parser p, MyListener ml){
	this.parser = p;
	this.polys = p.polys;
	this.ml = ml;
	cs = new ColorScheme();
	closestCountry = "";
	addMouseListener(ml);
	addMouseMotionListener(ml);
	addMouseWheelListener(ml);
    }

    String poly2string(Poly p){
	String text = "<font size='3' face='Verdana'>"+
	    "<font size='6'><b>"+p.name+"</b></font><br/><br/>";
	for(City c : p.cities)
	    text += "<b>"+c.name+":</b> "+c.info+"<br/>";
	return text+"</font>";
    }

    public void capTransform(){
	boolean CAP = false;
	if(!CAP) return;
	float x = (float) coordTransform.getTranslateX();
	float y = (float) coordTransform.getTranslateY();
	float scaleX = (float) coordTransform.getScaleX();
	float scaleY = (float) coordTransform.getScaleY();
	float minX = (float) parser.xrange.getX();
	float maxX = (float) parser.xrange.getY();
	float minY = (float) parser.yrange.getX();
	float maxY = (float) parser.yrange.getY();

	x = x < minX ? minX : x;
	x = x > maxX ? maxX : x;
	y = y < minY ? minY : y;
	y = y > maxY ? maxY : y;
	coordTransform.setToIdentity();
	coordTransform.translate(x, y);
	coordTransform.scale(scaleX, scaleY);
    }
    
    public Double transformPoint(Double p1){
	Double p2 = new Double();
	try{
	    AffineTransform inverse = coordTransform.createInverse();
	    inverse.transform(p1, p2);
	}catch(Exception e){
	    e.printStackTrace();
	}
	return p2;
    }
  
    public void paint(Graphics g){
	Graphics2D g2=null;
	try{ g2 = (Graphics2D) bs.getDrawGraphics();}
	catch(Exception e){ return; }
	g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
			    RenderingHints.VALUE_ANTIALIAS_OFF);	
	initialize(g2);
	g2.setTransform(coordTransform);
	drawGeom(g2);
	setupFont(g2);
	drawText(g2);
	g2.dispose();
	bs.show();
    }

    private void drawGeom(Graphics2D g2){
	g2.setPaint(cs.water);
	Dimension dim = getSize();
	g2.fillRect(-dim.width, -dim.height, dim.width*2, dim.height*2);
	g2.setStroke(new BasicStroke(0.0000000001f));
	for(Poly poly : polys)
	    for(GeneralPath path : poly.paths)
		drawLand(g2, path, poly);
    }

    private void initialize(Graphics2D g2){
	if(!init)
	    return;
	initSpace(g2);
	initPosition();
	makePaths();
	init = false;
    }
    
    private void initPosition(){
	int level = 3;
	ml.zoomLevel-=level;
	Dimension dim = getSize();
	Point p = new Point(dim.width/2, dim.height/2+5);
	Double p1 = transformPoint(ml.p2d(p));
	coordTransform.scale(ml.zoomMult*level,ml.zoomMult*level);
	Double p2 = transformPoint(ml.p2d(p));
	coordTransform.translate(p2.x-p1.x, p2.y-p1.y);
    }
    
    private void initSpace(Graphics2D g2){
	Dimension d = getSize();
	int xc = d.width / 2;
	int yc = d.height / 2;
	g2.translate(xc, yc);
	g2.scale(1, -1);
	coordTransform = g2.getTransform();
    }
    
    public void update(Graphics g){
	paint(g);
    }
    
    private void makePaths(){
	double x,y,xold=0,yold=0;
	GeneralPath p = null;
	for(int i=0; i<polys.size(); i++){
	    ArrayList<Point2D.Double> ps = polys.get(i).points;
	    if(ps == null || ps.size() < 1) continue;      
	    boolean first = true;
	    for(int j=0; j<ps.size(); j++){
		if(first){
		    first = false;
		    p = new GeneralPath(GeneralPath.WIND_EVEN_ODD, ps.size());
		    xold = ps.get(j).x;
		    yold = ps.get(j).y;
		    p.moveTo(xold, yold);
		    continue;
		}
		x = ps.get(j).x;
		y = ps.get(j).y;
		p.lineTo(x, y);
		if(xold == x && yold == y){
		    polys.get(i).paths.add(p);
		    first = true;
		}
	    }
	    polys.get(i).paths.add(p);
	}
    }
    
    private void setupFont(Graphics2D g2){
	g2.setFont(new Font("TimesRoman", Font.PLAIN, 1));
	Font font = g2.getFont();
	AffineTransform affineTransform = new AffineTransform();
	affineTransform.scale(1, -1);
	float sc = 2.0f/((ml.zoomLevel*-1.0f)/40.0f*8.0f);
	sc = sc == 0 ? 1 : sc;
	affineTransform.scale(sc,sc);
	g2.setFont(font.deriveFont(affineTransform));
	g2.setPaint(cs.text);
    }

    private void drawText(Graphics2D g2){
	if(ml.zoomLevel > -5)
	    return;
	City old = null;
	for(int i=0; i<polys.size(); i++){
	    if(polys.get(i).name.equals(closestCountry)){
		for(City c : polys.get(i).cities){
		    float y = (float) c.pos.x, x = (float) c.pos.y; // x/y swapped
		    if(!c.equals(polys.get(i).cities.get(0))){
			    float oy = (float) old.pos.x, ox = (float) old.pos.y;
			    g2.draw(new Line2D.Double(x,y,ox,oy));
            }
		    old = c;
		    g2.setPaint(Color.red);
		    float l = 0.1f, o = l/2.0f;
		    g2.draw(new Line2D.Float(x-l, y, x+l, y));
		    g2.draw(new Line2D.Float(x, y-l, x, y+l));
		    g2.setPaint(cs.text);
		    g2.drawString(c.name,x+o,y+o);
		}
	    }else{
		float x = (float) polys.get(i).center.x;
		float y = (float) polys.get(i).center.y;
		g2.drawString(polys.get(i).name,x,y);
	    }
	}
    }


    private float cityDist(City c1, City c2){
	return (float) Math.sqrt((c2.pos.x-c1.pos.x)*(c2.pos.x-c1.pos.x)+
			 (c2.pos.y-c1.pos.y)*(c2.pos.y-c1.pos.y));
    }
    
    void drawLand(Graphics2D g2, GeneralPath p, Poly poly){
	p.closePath();
	if(!poly.cities.isEmpty())
	    g2.setPaint(cs.visited);
	else
	    g2.setPaint(cs.land);
	if(poly.name == closestCountry)
	    g2.setPaint(cs.selected);
	g2.fill(p);
	g2.setPaint(cs.border);
	g2.draw(p);
    }
}
