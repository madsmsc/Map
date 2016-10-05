import java.awt.event.*;
import java.awt.geom.*;
import java.awt.*;
import java.awt.geom.Point2D.Double;

public class MyListener implements MouseListener, MouseMotionListener, 
				   MouseWheelListener, ActionListener{
    public int minZoomLevel = -40, maxZoomLevel = 0, zoomLevel = 0;
    public double zoomMult = 1.1;
    private Point drag1, drag2;
    private Gui g;

    public MyListener(Gui g){
	this.g = g;
    }

    public void mouseClicked(MouseEvent e){}
    public void mouseReleased(MouseEvent e){}
    public void mouseEntered(MouseEvent e){}  
    public void mouseExited(MouseEvent e){}  
    public void mouseMoved(MouseEvent e){}  
    public void mouseDragged(MouseEvent e){
	moveCamera(e);
    }

    public void mouseWheelMoved(MouseWheelEvent e){
	zoomCamera(e);
    }
    
    public void mousePressed(MouseEvent e){
	drag1 = e.getPoint();
	if(MouseEvent.BUTTON3 == e.getButton())
	    g.selectCountry(tp(e.getPoint()));
    }

    public Double p2d(Point p){ // Point to Double
	return new Double((double) p.x, (double) p.y);
    }

    private Double tp(Point p){ // transform Point to Double using coordTransform
	return g.map.transformPoint(p2d(p));
    }
    
    private void moveCamera(MouseEvent e){
	try{
	    drag2 = e.getPoint();
	    Double drag1t = tp(drag1), drag2t = tp(drag2);
	    double dx = drag2t.x-drag1t.x, dy = drag2t.y-drag1t.y;
	    g.map.coordTransform.translate(dx, dy);
	    g.map.capTransform();
	    drag1 = drag2;
	    g.map.repaint();
	}catch(Exception ex){ex.printStackTrace();}
    }
    
    private void zoomCamera(MouseWheelEvent e){
	int wheelRotation = e.getWheelRotation();
	Point p = e.getPoint();
	boolean zoomed = false;
	Double p2, p1 = tp(p);
	if(wheelRotation > 0 && zoomLevel < maxZoomLevel){
	    zoomed = true;
	    zoomLevel++;
	    g.map.coordTransform.scale(1/zoomMult, 1/zoomMult);
	}else if(zoomLevel > minZoomLevel){
	    zoomed = true;
	    zoomLevel--;
	    g.map.coordTransform.scale(zoomMult,zoomMult);
	}if(zoomed){
	    p2 = tp(p);
	    g.map.coordTransform.translate(p2.x-p1.x, p2.y-p1.y);
	    g.map.repaint();
	}
    }
    
    public void actionPerformed(ActionEvent e){
	g.buttonAction(e.getActionCommand());
    }
}
