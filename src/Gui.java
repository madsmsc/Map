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

public class Gui {
    private static final int width = 1200, height = 600, taWidth = 400, taHeight = 372;
    private static String WELCOME_TEXT = "<font size='3' face='Verdana'>" +
	"Welcome! Select a country...</font>";
    
    private JEditorPane editorPane;
    private JScrollPane dateScrollPane;
    private Container con;
    private JPanel pane;
    private JFrame frame;
    private MyListener ml;
    private Parser parser;
    public Map map;

    public static void main(String args[]){
	new Gui();
    }
    
    public Gui(){
    	parser = new Parser();
    	ml = new MyListener(this);
    	map = new Map(parser, ml);
    	setupGui();
	map.setupBuffers();
    }

    private void setupGui(){
	setupContainer();
    	setupMenu();
    	setupDatePanes();
	setupMainPanes();    
    	updateMapSize(true);
	setupFrames();
    }

    private void setupFrames(){
   	frame.addWindowListener(new WindowAdapter() {
    	    public void windowClosing(WindowEvent e) {System.exit(0);}
 	});
    	frame.setResizable(false);
    	frame.pack();
    	frame.setVisible(true);
    }

    private void setupContainer(){
    	frame = new JFrame("MapRender");
    	con = frame.getContentPane();
    	con.setLayout(new BorderLayout());
    	pane = new JPanel();
    	pane.setLayout(new BorderLayout());
    }

    private void setupMenu(){
	JMenuBar menuBar = new JMenuBar();
	frame.setJMenuBar(menuBar);

	JMenu menu = new JMenu("Files");
	menu.getAccessibleContext().setAccessibleDescription("?");
	menuBar.add(menu);
	JMenuItem menuItem = new JMenuItem("Show data");
	menuItem.addActionListener(ml);
	menu.add(menuItem);
	
	menu = new JMenu("View");
	menu.getAccessibleContext().setAccessibleDescription("?");
	menuBar.add(menu);
	ButtonGroup group = new ButtonGroup();
	// TODO MPE: refactor disse til en Const class med statiske felter.
	menuItem = new JRadioButtonMenuItem("Map view");
	menuItem.setSelected(true);
	menuItem.addActionListener(ml);
	group.add(menuItem);
	menu.add(menuItem);
	menuItem = new JRadioButtonMenuItem("Text view");
	menuItem.addActionListener(ml);
	group.add(menuItem);
	menu.add(menuItem);
	menuItem = new JRadioButtonMenuItem("Map/Text view");
	menuItem.addActionListener(ml);
	group.add(menuItem);
	menu.add(menuItem);

	menu = new JMenu("Color");
	menu.getAccessibleContext().setAccessibleDescription("?");
	menuBar.add(menu);
	group = new ButtonGroup();
	menuItem = new JRadioButtonMenuItem("Greyscale");
	menuItem.setSelected(true);
	menuItem.addActionListener(ml);
	group.add(menuItem);
	menu.add(menuItem);
	menuItem = new JRadioButtonMenuItem("Blue/Green");
	menuItem.addActionListener(ml);
	group.add(menuItem);
	menu.add(menuItem);
	menuItem = new JRadioButtonMenuItem("Textures");
	menuItem.addActionListener(ml);
	group.add(menuItem);
	menu.add(menuItem);
    }

    private void setupMainPanes(){
	editorPane = new JEditorPane("text/html", null);
    	editorPane.setEditable(false);
    	editorPane.setPreferredSize(new Dimension(taWidth,taHeight));
    	editorPane.setText(WELCOME_TEXT);
    	JScrollPane scrollPane = new JScrollPane(editorPane);
    	scrollPane.setBorder(BorderFactory.createEmptyBorder());
    	scrollPane.setPreferredSize(new Dimension(taWidth,taHeight));
    	
    	editorPane.setBackground(getBackgroundColor());
    	con.setBackground(getBackgroundColor());
    	con.add(map, BorderLayout.WEST);
    	pane.add(scrollPane);
    }

    private Color getBackgroundColor(){
	float rgb = 238.0f / 255.0f;
    	return new Color(rgb, rgb, rgb);
    }

    private void updateMapSize(boolean infoHidden){
	Dimension dim = new Dimension(width - (infoHidden ? 0 : taWidth), height);
	map.setPreferredSize(dim);
    }

    public void selectCountry(Double p){
	for(Poly poly : parser.polys){
	    for(GeneralPath path : poly.paths){
		if(!path.contains(p.x, p.y)){
		    continue;
		}
		map.closestCountry = poly.name;
		editorPane.setText(poly.toString());
		map.repaint();
		return;
	    }
	}
	editorPane.setText(WELCOME_TEXT);
	map.repaint();
    }

    private void setupDatePanes(){
    	JEditorPane dateEditorPane = new JEditorPane("text/html", null);
    	dateEditorPane.setEditable(false);
    	dateEditorPane.setPreferredSize(new Dimension(width,height));
    	dateScrollPane = new JScrollPane(dateEditorPane);
    	dateScrollPane.setBorder(BorderFactory.createEmptyBorder());
    	dateScrollPane.setPreferredSize(new Dimension(width,height));

        java.util.List<String> names = new ArrayList<String>();
	StringBuilder sb = new StringBuilder();
	for(Poly poly : parser.polys){
	    if(poly.cities.isEmpty() || names.contains(poly.name)){
		continue;
	    }
	    names.add(poly.name);
	    sb.append(poly.toString());
	    sb.append("<br/>");
	}
	dateEditorPane.setText(sb.toString());
    }

    public void showText(){
	con.add(pane, BorderLayout.EAST);
	updateMapSize(false);
	updateFrame();
    }

    public void hideText(){
	con.remove(pane);
	updateMapSize(true);
	updateFrame();	    
	con.removeAll();
	con.add(map, BorderLayout.WEST);
	updateFrame();
    }

    public void mapView(){
	con.removeAll();
	con.add(map, BorderLayout.WEST);
	updateMapSize(true);
	updateFrame();
    }

    public void textView(){
	con.removeAll();
	con.add(dateScrollPane, BorderLayout.NORTH);
	updateMapSize(true);
	updateFrame();
    }

    private void updateFrame(){
	frame.pack();
	frame.repaint();
    }
}
