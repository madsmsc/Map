/* TODO:
   remove init. shouldnt be necessary.
   and see if i can remove any other fields.
   dont let the main stay in gui. make a small entry point class.
   Map#drawText() draws characters on top of each other.
     i need to google this - maybe a mac issue?
   the scrolling doesn't work properly
     i need to google this - maybe a mac issue?
   the initialize method could probably
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
    private ArrayList<Poly> polys;
    private MyListener ml;
    public AffineTransform coordTransform;
    public BufferStrategy bs;
    public String closestCountry;

    public Map(Parser p, MyListener ml){
	this.polys = p.polys;
	this.ml = ml;
	closestCountry = "";
	addMouseListener(ml);
	addMouseMotionListener(ml);
	addMouseWheelListener(ml);
    }

    public void capTransform(){
	/*
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
	*/
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
	Graphics2D g2 = null;
	try{
	    g2 = (Graphics2D) bs.getDrawGraphics();
	}
	catch(Exception e){
	    e.printStackTrace();
	    return;
	}
	g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
			    RenderingHints.VALUE_ANTIALIAS_OFF);	
	g2.setTransform(coordTransform);
	drawGeom(g2);
	setupFont(g2);
	drawText(g2);
	g2.dispose();
	bs.show();
    }

    public void setupBuffers(){
	createBufferStrategy(2);
    	bs = getBufferStrategy();
    	requestFocus();

	initSpace();
	initPosition();
	makePaths();
    }

    private void drawGeom(Graphics2D g2){
	g2.setPaint(ColorScheme.water);
	Dimension dim = getSize();
	g2.fillRect(-dim.width, -dim.height, dim.width*2, dim.height*2);
	g2.setStroke(new BasicStroke(0.0000000001f));
	for(Poly poly : polys)
	    for(GeneralPath path : poly.paths)
		drawLand(g2, path, poly);
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
    
    private void initSpace(){
	Graphics2D g2 = (Graphics2D) bs.getDrawGraphics();
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
	float sc = 2.0f / (-ml.zoomLevel / 40.0f * 8.0f);
	sc = sc == 0 ? 1 : sc;
	affineTransform.scale(sc,sc);
	g2.setFont(font.deriveFont(affineTransform));
	g2.setPaint(ColorScheme.text);
    }

    private void drawText(Graphics2D g2){
	if(ml.zoomLevel > -5)
	    return;
	City old = null;
	for(int i = 0; i < polys.size(); i++) {
	    if(polys.get(i).name.equals(closestCountry)) {
		for(City c : polys.get(i).cities) {
		    float y = (float) c.pos.x, x = (float) c.pos.y; // x/y swapped
		    if(!c.equals(polys.get(i).cities.get(0))) {
			float oy = (float) old.pos.x, ox = (float) old.pos.y;
			g2.draw(new Line2D.Double(x, y, ox, oy));
		    }
		    old = c;
		    g2.setPaint(Color.red);
		    float l = 0.4f, o = l/2.0f;
		    g2.draw(new Line2D.Float(x - l, y, x + l, y));
		    g2.draw(new Line2D.Float(x, y - l, x, y + l));
		    g2.setPaint(ColorScheme.text);
		    g2.drawString(c.name, x + o, y + o);
		}
	    }else{
		float x = (float) polys.get(i).center.x;
		float y = (float) polys.get(i).center.y;
		g2.drawString(polys.get(i).name, x, y);
	    }
	}
    }


    private float cityDist(City c1, City c2){
	return (float) Math.sqrt((c2.pos.x-c1.pos.x)*(c2.pos.x-c1.pos.x)+
				 (c2.pos.y-c1.pos.y)*(c2.pos.y-c1.pos.y));
    }
    
    void drawLand(Graphics2D g2, GeneralPath p, Poly poly){
	p.closePath();
	if(!poly.cities.isEmpty()){
	    g2.setPaint(ColorScheme.visited);
	} else {
	    g2.setPaint(ColorScheme.land);
	}if(poly.name == closestCountry){
	    g2.setPaint(ColorScheme.selected);
	}
	g2.fill(p);
	g2.setPaint(ColorScheme.border);
	g2.draw(p);
    }
}
