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

public class Gui{
    public static void main(String args[]){
	new Gui();
    }

    private JEditorPane editorPane, dateEditorPane;
    private JScrollPane scrollPane, dateScrollPane;
    private boolean infoHidden = true;
    private int width = 1200, height = 600, taWidth = 400, taHeight = 372;
    private JButton textViewButton, mapViewButton, colorMapButton, greyMapButton,
	        textureMapButton, showInfoButton, hideInfoButton;
    private JPanel viewMenu, colorMenu;
    private Container con;
    private JPanel pane, menu;
    private JFrame frame;
    private MyListener ml;
    private Parser parser;
    private String welcomeText;
    public Map map;
    
    public Gui(){
    	parser = new Parser();
    	ml = new MyListener(this);
    	map = new Map(parser, ml);
    	setupGui();
    }

    private void setupGui(){
    	welcomeText = "<font size='3' face='Verdana'>"+
    	    "Welcome! Select a country...</font>";
    	frame = new JFrame("MapRender");
    	con = frame.getContentPane();
    	con.setLayout(new BorderLayout());
    	pane = new JPanel();
    	pane.setLayout(new BorderLayout());
    	
    	editorPane = new JEditorPane("text/html", null);
    	editorPane.setEditable(false);
    	editorPane.setPreferredSize(new Dimension(taWidth,taHeight));
    	editorPane.setText(welcomeText);
    	scrollPane = new JScrollPane(editorPane);
    	scrollPane.setBorder(BorderFactory.createEmptyBorder());
    	scrollPane.setPreferredSize(new Dimension(taWidth,taHeight));
    	
    	dateEditorPane = new JEditorPane("text/html", null);
    	dateEditorPane.setEditable(false);
    	dateEditorPane.setPreferredSize(new Dimension(width,height));
    	dateScrollPane = new JScrollPane(dateEditorPane);
    	dateScrollPane.setBorder(BorderFactory.createEmptyBorder());
    	dateScrollPane.setPreferredSize(new Dimension(width,height));
    	setupDateEditorPane();
    
    	float rgb = 238.0f / 255.0f;
    	Color back = new Color(rgb, rgb, rgb);
    	editorPane.setBackground(back);
    	con.setBackground(back);
    
    	con.add(map, BorderLayout.WEST);
    	pane.add(scrollPane);
    	createButtons();
    	updateMapSize();
    	
    	frame.addWindowListener(new WindowAdapter() {
    		public void windowClosing(WindowEvent e) {System.exit(0);}
    	    });
    	frame.setResizable(false);
    	frame.pack();
    	frame.setVisible(true);
    
    	map.createBufferStrategy(2);
    	map.bs = map.getBufferStrategy();
    	map.requestFocus();
    }

    private void updateMapSize(){
	Dimension dim = null;
	if(infoHidden)
	    dim = new Dimension(width, height);
	else
	    dim = new Dimension(width-taWidth, height);
	map.setPreferredSize(dim);
    }

    public void selectCountry(Double p){
	for(Poly poly : parser.polys){
	    for(GeneralPath path : poly.paths){
		if(path.contains(p.x, p.y)){
		    map.closestCountry = poly.name;
		    String text = "";
		    if(poly.cities.isEmpty())
			text = "<font size='3' face='Verdana'>"+
			    poly.name+"... never been there.</font>";
		    else
			text = map.poly2string(poly);
		    editorPane.setText(text);
		    map.repaint();
		    return;
		}
	    }
	}
	editorPane.setText(welcomeText);
	map.repaint();
    }

    private void setupDateEditorPane(){
        ArrayList<String> list = new ArrayList<String>();
	String text = "";
	for(Poly p : parser.polys){
	    if(p.cities.isEmpty())
		continue;
	    if(list.contains(p.name))
		continue;
	    list.add(p.name);
	    text += map.poly2string(p)+"<br/>";
	}
	dateEditorPane.setText(text);
    }

    private void showText(){
	infoHidden = false;
	showInfoButton.setEnabled(false);
	hideInfoButton.setEnabled(true);
	con.add(pane, BorderLayout.EAST);
	updateMapSize();
	updateFrame();
    }

    private void hideText(){
	infoHidden = true;
	showInfoButton.setEnabled(true);
	hideInfoButton.setEnabled(false);
	con.remove(pane);
	updateMapSize();
	updateFrame();	    
	con.removeAll();
	con.add(map, BorderLayout.WEST);
	con.add(menu, BorderLayout.SOUTH);
	updateFrame();
	//hideText();
	//showText();
    }

    private void mapView(){
	showInfoButton.setEnabled(true);
	colorMapButton.setEnabled(true);
	greyMapButton.setEnabled(true);
	textViewButton.setEnabled(true);
	mapViewButton.setEnabled(false);
	con.removeAll();
	con.add(map, BorderLayout.WEST);
	con.add(menu, BorderLayout.SOUTH);
	updateFrame();
	if(!infoHidden){
	    hideText();
	    showText();
	}
    }

    private void textView(){
	infoHidden = true;
	showInfoButton.setEnabled(false);
	colorMapButton.setEnabled(false);
	greyMapButton.setEnabled(false);
	textViewButton.setEnabled(false);
	mapViewButton.setEnabled(true);
	con.removeAll();
	con.add(dateScrollPane, BorderLayout.NORTH);
	con.add(menu, BorderLayout.SOUTH);
	updateFrame();
    }

    public void buttonAction(String s){
	if(s.equals(showInfoButton.getText())){
	    showText();
	}else if(s.equals(hideInfoButton.getText())){
	    hideText();
	}else if(s.equals(textViewButton.getText())){
	    textView();
	}else if(s.equals(mapViewButton.getText())){
	    mapView();
	}else if(s.equals(colorMapButton.getText())){
	    colorMapButton.setEnabled(false);
	    greyMapButton.setEnabled(true);
	    map.cs.setBlueGreen();
	    map.repaint();
	}else if(s.equals(greyMapButton.getText())){
	    colorMapButton.setEnabled(true);
	    greyMapButton.setEnabled(false);
	    map.cs.setGrey();
	    map.repaint();
	}else if(s.equals(textureMapButton.getText())){
	    map.cs.setTexture();
	    map.repaint();
	}else
	    System.out.println("no such button = "+s);
    }

    private void updateFrame(){
	frame.pack();
	//frame.revalidate();
	frame.repaint();;
    }

    private void createButtons(){
	textViewButton = new JButton("<html>text view</html>");
	mapViewButton = new JButton("<html>map view</html>");
	mapViewButton.setEnabled(false);
	colorMapButton = new JButton("<html>blue-green</html>");
	greyMapButton = new JButton("<html>greyscale</html>");
	greyMapButton.setEnabled(false);
	textureMapButton = new JButton("<html>texture</html>");
	textureMapButton.setEnabled(false);
	showInfoButton = new JButton("<html>show text</html>");
	hideInfoButton = new JButton("<html>hide text</html>");
	hideInfoButton.setEnabled(false);
	
	textViewButton.addActionListener(ml);
	mapViewButton.addActionListener(ml);
	colorMapButton.addActionListener(ml);
	greyMapButton.addActionListener(ml);
	textureMapButton.addActionListener(ml);
	showInfoButton.addActionListener(ml);
	hideInfoButton.addActionListener(ml);
	
	menu = new JPanel();
	menu.setLayout(new BoxLayout(menu, BoxLayout.X_AXIS));

	// transparent containers
	// where to put show/hide text button?
	// padding outside border on all four panels

	viewMenu = new JPanel();
	viewMenu.setBorder(BorderFactory.createLineBorder(Color.black));
	viewMenu.add(new JLabel("View: ")); 
	viewMenu.add(Box.createRigidArea(new Dimension(5,0)));
	viewMenu.add(textViewButton); 
	viewMenu.add(Box.createRigidArea(new Dimension(5,0)));
	viewMenu.add(mapViewButton);
	viewMenu.add(Box.createRigidArea(new Dimension(5,0)));
	viewMenu.add(showInfoButton);
	viewMenu.add(Box.createRigidArea(new Dimension(5,0)));
	viewMenu.add(hideInfoButton);
	menu.add(viewMenu);
	
	colorMenu = new JPanel();
	colorMenu.setBorder(BorderFactory.createLineBorder(Color.black));
	colorMenu.add(new JLabel("Color: "));
	colorMenu.add(Box.createRigidArea(new Dimension(5,0)));
	colorMenu.add(colorMapButton);
	colorMenu.add(Box.createRigidArea(new Dimension(5,0)));
	colorMenu.add(greyMapButton);
	colorMenu.add(Box.createRigidArea(new Dimension(5,0)));
	colorMenu.add(textureMapButton);
	menu.add(colorMenu);

	Component[] cs = menu.getComponents();
	for(Component c : cs)
	    c.setBackground((Color) map.cs.water);
	con.add(menu, BorderLayout.SOUTH);
    }
}
