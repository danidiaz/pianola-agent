package info.danidiaz.pianola;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import javax.swing.AbstractButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class Snapshot {

    private ImageBin imageBin;
    
    private List<WindowWrapper> windowArray = new ArrayList<WindowWrapper>();
    //private Map<Window,BufferedImage> windowImageMap = new HashMap<Window,BufferedImage>();
    private List<Component> componentArray = new ArrayList<Component>();
    
    boolean releaseIsPopupTrigger;
    
    private JsonNode json;
    
	public static Snapshot build(ImageBin imageBin, boolean releaseIsPopupTrigger) throws Exception {
    	Snapshot snapshot = new Snapshot(imageBin, releaseIsPopupTrigger);
    	JsonNode json = snapshot.buildAndWrite();
    	snapshot.setJson(json);
    	return snapshot;
    }

    private Snapshot(ImageBin imageBin, boolean releaseIsPopupTrigger) {
        this.imageBin = imageBin;
        this.releaseIsPopupTrigger = releaseIsPopupTrigger;
    }

    private final JsonNode buildAndWrite() throws Exception {
    	FutureTask<JsonNode> futureTask = new FutureTask<JsonNode>(
    			new Callable<JsonNode>() {
    				@Override
    				public JsonNode call() throws Exception {
                        Window warray[] = Window.getOwnerlessWindows();
                        return writeWindowArray(warray);
    				}
    			}    			
            );

    	try {
            SwingUtilities.invokeAndWait(futureTask);
            return futureTask.get();
    	} finally {
            this.imageBin.flush();
    	}
    }
    
 /*   private static int countShowing(Component[] warray) {
        int visibleCount = 0;
        for (int i=0;i<warray.length;i++) {                            
            if (warray[i].isShowing()) {
                visibleCount++;
            }
        }
        return visibleCount;
    }*/
    
    private JsonNode writeWindowArray(Window warray[]) throws IOException {
    	ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
        for (int i=0;i<warray.length;i++) {
            Window w = warray[i];
            if (w.isShowing()) {
                arrayNode.add(writeWindow(w));
            }        
        }
        return arrayNode;
    }
    
    private ArrayNode writeIntegerPair(int a, int b) {
    	ArrayNode array = JsonNodeFactory.instance.arrayNode();
    	array.add((int)a);
    	array.add((int)b);
    	return array;
    }

    private JsonNode writeWindow(Window w) throws IOException {
    	ObjectNode windowNode = JsonNodeFactory.instance.objectNode();

        int windowId = windowArray.size();
        BufferedImage image = imageBin.obtainImage(w.getSize());
        w.paint(image.getGraphics());
        
        windowNode.put("windowId",(int)windowId);
        
        String title = "";
        if (w instanceof JFrame) {
            title = ((JFrame)w).getTitle();
        } else if (w instanceof JDialog) {
            title = ((JDialog)w).getTitle();                                    
        }

        windowNode.put("title",title);
        
        Point loc = w.getLocationOnScreen();
        windowNode.put("loc", writeIntegerPair(loc.x,loc.y)); 
        windowNode.put("pos", writeIntegerPair((int)w.getHeight(),(int)w.getWidth())); 

        windowNode.put("menuBar", writeMenuBar(w));
        windowNode.put("popupLayer", writePopupLayer(w));
                        
        RootPaneContainer rpc = (RootPaneContainer)w;
        windowNode.put("rootPane",  writeComponent((Component) rpc.getContentPane(), w, false));                                                               
        
        windowNode.put("owned", writeWindowArray(w.getOwnedWindows()));
        windowArray.add(new WindowWrapper(w, windowNode, image));
        return windowNode;
    }
    
    private JsonNode writeMenuBar(Window w) throws IOException {        
        JMenuBar menubar = null;
        if (w instanceof JFrame) {
            menubar = ((JFrame)w).getJMenuBar();
        } else if (w instanceof JDialog) {
            menubar = ((JDialog)w).getJMenuBar();                                    
        }
        
        ArrayNode array = JsonNodeFactory.instance.arrayNode();
        
        if (menubar!=null) {
            for (int i=0; i<menubar.getMenuCount();i++) {
                array.add(writeComponent(menubar.getMenu(i), w, false));
            }
        }

        return array;
    }
    
    private JsonNode writePopupLayer(Window w) throws IOException {
        Component[] popupLayerArray = new Component[] {};
        if (w instanceof JFrame) {
            popupLayerArray = ((JFrame)w).getLayeredPane().getComponentsInLayer(JLayeredPane.POPUP_LAYER);
        } else if (w instanceof JDialog) {
            popupLayerArray = ((JDialog)w).getLayeredPane().getComponentsInLayer(JLayeredPane.POPUP_LAYER);                                    
        }
        
        ArrayNode array = JsonNodeFactory.instance.arrayNode();

        for (int i=0;i<popupLayerArray.length;i++) {
            Component c = (Component) popupLayerArray[i];
            if (c.isShowing()) {
                array.add(writeComponent(c, w, false));    
            }
        }

        return array;
    }
        
    private JsonNode writeComponent(Component c, Component coordBase, boolean isRenderer) throws IOException {
        
    	ObjectNode objectNode = JsonNodeFactory.instance.objectNode();

        int componentId = componentArray.size();
        componentArray.add(c);
        
        objectNode.put("cid", (int) componentId);
        
        {
            if (isRenderer) { // getLocationOnScreen does not work on renderers.
            	objectNode.put("cloc", writeIntegerPair(0, 0));
            } else {
                Point loc = c.getLocationOnScreen();
            	objectNode.put("cloc", writeIntegerPair((int)loc.x, (int)loc.y));
            } 
        }
        
        objectNode.put("csize", writeIntegerPair(c.getHeight(),c.getWidth()));
        
        objectNode.put("cname", writePotentiallyNullString(c.getName()));

        String tooltipText = (c instanceof JComponent) ? ((JComponent)c).getToolTipText() : "";
        objectNode.put("tooltip", writePotentiallyNullString(tooltipText));
        
        if (c instanceof AbstractButton) {
        	objectNode.put("ctext", writePotentiallyNullString(((AbstractButton)c).getText()));
        } else if (c instanceof JLabel) {
        	objectNode.put("ctext", writePotentiallyNullString(((JLabel)c).getText()));
        } else if (c instanceof JTextComponent) {
        	objectNode.put("ctext", writePotentiallyNullString(((JTextComponent)c).getText()));
        } else {
        	objectNode.put("ctext", JsonNodeFactory.instance.nullNode());
        }

        objectNode.put("cenabled", c.isEnabled());
        
        objectNode.put("ctype",writeComponentType(c,coordBase));
        
        Component children[] = new Component[]{};
        if (c instanceof Container) {            
            children = ((Container)c).getComponents();
        }
                              
    	ArrayNode childrenNode = new ArrayNode(JsonNodeFactory.instance);
        for (int i=0;i<children.length;i++) {
            if (children[i].isShowing()||isRenderer) {                                
                childrenNode.add(writeComponent((Component)children[i],coordBase,isRenderer));
            }
        }

        objectNode.put("cchildren",childrenNode);

        return objectNode;
    }
    
    private JsonNode writeComponentType(Component c,Component coordBase) throws IOException 
    {
    	ObjectNode objectTypeNode = JsonNodeFactory.instance.objectNode();
    		
        if (c instanceof JPanel) {
        	objectTypeNode.put("JPanel", new ArrayNode(JsonNodeFactory.instance));

        } else if (c instanceof JToggleButton || c instanceof JCheckBoxMenuItem || c instanceof JRadioButtonMenuItem) {
        	objectTypeNode.put("JToggleButton", JsonNodeFactory.instance.booleanNode(((AbstractButton)c).isSelected()));

        } else if (c instanceof AbstractButton) { // normal button, not toggle button
        	objectTypeNode.put("Button", new ArrayNode(JsonNodeFactory.instance));

        } else if (c instanceof JTextField ) {
            JTextField textField = (JTextField) c;
        	objectTypeNode.put("JTextField", JsonNodeFactory.instance.booleanNode(textField.isEditable()));

        } else if (c instanceof JLabel) {
        	objectTypeNode.put("JLabel", new ArrayNode(JsonNodeFactory.instance));

        } else if (c instanceof JComboBox) {
            JComboBox comboBox = (JComboBox)c;
            ListCellRenderer renderer = comboBox.getRenderer();
            JList dummyJList = new JList();

            if (comboBox.getSelectedIndex()==-1) {
            	objectTypeNode.put("JComboBox", JsonNodeFactory.instance.nullNode());
            } else {
                Component cell = (Component)renderer.getListCellRendererComponent(dummyJList, 
                                comboBox.getModel().getElementAt(comboBox.getSelectedIndex()), 
                                comboBox.getSelectedIndex(), 
                                false, 
                                false
                            );
            	objectTypeNode.put("JComboBox", writeComponent(cell, coordBase,true));
            }                          
                       
        } else if (c instanceof JList) {
            JList list = (JList) c;            
            ListCellRenderer renderer = list.getCellRenderer();
            
            ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
            for (int rowid=0; rowid<list.getModel().getSize(); rowid++) {
                Point loc = list.getLocationOnScreen();
                Rectangle cellBounds = list.getCellBounds(rowid, rowid);
                arrayNode.add(
                        writeCell(  rowid, 
                                    0,
                                    loc.x + cellBounds.x,
                                    loc.y + cellBounds.y,                            
                                    (Component)renderer.getListCellRendererComponent(list, 
                                            list.getModel().getElementAt(rowid), 
                                            rowid,
                                            false, 
                                            false
                                        ), 
                                    coordBase,
                                    false
                                    )
                    );                                
            }

            objectTypeNode.put("JList", arrayNode);
            
        } else if (c instanceof JTable) {
            JTable table = (JTable) c;
            TableModel model = table.getModel();
            
            int rowcount = model.getRowCount();
            int columncount = model.getColumnCount();
            ArrayNode outerArrayNode = JsonNodeFactory.instance.arrayNode();
            for (int j=0;j<columncount;j++) {            
                ArrayNode innerArrayNode = JsonNodeFactory.instance.arrayNode();
                for (int i=0;i<rowcount;i++) {
                    Point loc = table.getLocationOnScreen();
                    Rectangle cellBounds = table.getCellRect(i, j, false);
                    
                    TableCellRenderer renderer = table.getCellRenderer(i, j);                    
                    innerArrayNode.add(
                            writeCell(  i, 
                                        j, 
                                        loc.x + cellBounds.x,
                                        loc.y + cellBounds.y,                              
                                        (Component)renderer.getTableCellRendererComponent(table,
                                                    model.getValueAt(i, j),  
                                                    false, 
                                                    false,
                                                    i,
                                                    j
                                        ), 
                                        coordBase,
                                        false 
                                    )
                     );                                                                        
                }
                outerArrayNode.add(innerArrayNode);
            }                        
            objectTypeNode.put("JTable", outerArrayNode);
            
        } else if (c instanceof JTree) {
            JTree tree = (JTree) c;
            TreeModel model = tree.getModel();
            TreeCellRenderer renderer = tree.getCellRenderer();
            
        	// http://programmers.stackexchange.com/questions/214227/reconstructing-a-tree-from-depth-information
            Deque<TreeNodeHelper> stack = new LinkedList<TreeNodeHelper>();
            stack.addFirst(new TreeNodeHelper(JsonNodeFactory.instance.nullNode()));

            int expectedpathcount = 1;
            for (int rowid=0;rowid<tree.getRowCount();rowid++) {
                TreePath path = tree.getPathForRow(rowid);
                if (path.getPathCount()<expectedpathcount) {
                	int delta = expectedpathcount - path.getPathCount();
                    for (int i=0; i < delta; i++) {
                    	TreeNodeHelper elem1 = stack.removeFirst();
                    	TreeNodeHelper elem2 = stack.removeFirst();
                    	elem2.add(elem1.convert2json());
                    	stack.addFirst(elem2);
                    }
                    expectedpathcount = path.getPathCount() + 1;
                }                
                Point loc = tree.getLocationOnScreen();
                Rectangle cellBounds = tree.getRowBounds(rowid);
                stack.addFirst(
                	new TreeNodeHelper(
                        writeCell(  
                                rowid, 
                                0, 
                                loc.x + cellBounds.x,
                                loc.y + cellBounds.y,                         
                                (Component)renderer.getTreeCellRendererComponent(
                                        tree,
                                        path.getLastPathComponent(),
                                        tree.isRowSelected(rowid),
                                        tree.isExpanded(rowid),
                                        model.isLeaf(path.getLastPathComponent()),
                                        rowid,
                                        true
                                    ), 
                                coordBase,
                                true
                                )
                        )
                	);                                                 
            }
            while (stack.size()>1) {
            	TreeNodeHelper elem1 = stack.removeFirst();
            	TreeNodeHelper elem2 = stack.removeFirst();
            	elem2.add(elem1.convert2json());
            	stack.addFirst(elem2);
            }
            ArrayNode topLevelNodes = stack.removeFirst().getChildren();
            objectTypeNode.put("JTree", topLevelNodes);
            
        } else if (c instanceof JMenuBar) {                    
        	objectTypeNode.put("JMenuBar", new ArrayNode(JsonNodeFactory.instance));

        } else if (c instanceof JPopupMenu) {                    
        	objectTypeNode.put("JPopupMenu", new ArrayNode(JsonNodeFactory.instance));
        
        } else if (c instanceof JTabbedPane) {
            JTabbedPane tpane = (JTabbedPane)c;
            ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
            for (int i=0; i<tpane.getTabCount();i++) {
                ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
                objectNode.put("tabId", (int)i);
                objectNode.put("tabText", tpane.getTitleAt(i));
                objectNode.put("tabToolTip", writePotentiallyNullString(tpane.getToolTipTextAt(i)));
                objectNode.put("isTabSelected", i==tpane.getSelectedIndex());
                arrayNode.add(objectNode);
            }
            objectTypeNode.put("JTabbedPane", arrayNode);
        } else {
        	objectTypeNode.put("JUnknown", JsonNodeFactory.instance.textNode(c.getClass().getName()));
        }

    	return objectTypeNode;
    }
    
    private JsonNode writeCell(
                int rowid, 
                int colid, 
                int xPos,
                int yPos,
                Component rendererc, 
                Component coordBase,
                boolean belongsToJTree 
            ) throws IOException 
    {
    	ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
    	objectNode.put("rowid", (int)rowid);
    	objectNode.put("colid", (int)colid);

    	objectNode.put("cellPos", writeIntegerPair((int)xPos,(int)yPos));
    	objectNode.put("cellDim", writeIntegerPair((int)rendererc.getHeight(),rendererc.getWidth()));
    	objectNode.put("renderer", writeComponent(rendererc, coordBase, true));
    	objectNode.put("isFromTree", (boolean)belongsToJTree);
        return objectNode;
    }
    
    //
    //
    //
    private static JsonNode writePotentiallyNullString(String s) throws IOException {
        if (s==null) {
        	return JsonNodeFactory.instance.nullNode();
        } else {
        	return JsonNodeFactory.instance.textNode(s);
        }
    }

   public void click(int componentid) {
        
        final Component c = (Component)componentArray.get(componentid);
        Point point = new Point(c.getWidth()/2,c.getHeight()/2);
        postMouseEvent(c, MouseEvent.MOUSE_ENTERED, 0, point, 0, false);
        pressedReleasedClicked1(c, new Rectangle(0, 0, c.getWidth(), c.getHeight()), 1);
    }
    
    public void doubleClick(int componentid) {
        
        final Component c = (Component)componentArray.get(componentid);
        Point point = new Point(c.getWidth()/2,c.getHeight()/2);
        postMouseEvent(c, MouseEvent.MOUSE_ENTERED, 0, point, 0, false);
        Rectangle rect =  new Rectangle(0, 0, c.getWidth(), c.getHeight());
        pressedReleasedClicked1(c, rect, 1);
        pressedReleasedClicked1(c, rect, 2);
    }
    
    public void rightClick(final int componentid) {
        // http://stackoverflow.com/questions/5736872/java-popup-trigger-in-linux
        final Component button = (Component)componentArray.get(componentid);
        
        Point point = new Point(button.getWidth()/2,button.getHeight()/2);

        postMouseEvent(button, MouseEvent.MOUSE_ENTERED, 0, point, 0, false);
        postMouseEvent(button, MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON3_MASK, point, 1, !releaseIsPopupTrigger);
        postMouseEvent(button, MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON3_MASK, point, 1, releaseIsPopupTrigger);
        postMouseEvent(button, MouseEvent.MOUSE_CLICKED, MouseEvent.BUTTON3_MASK, point, 1, false); 
    }    
    
    public void clickButton(int buttonId) {
        
        final AbstractButton button = (AbstractButton)componentArray.get(buttonId);
        Point point = new Point(button.getWidth()/2,button.getHeight()/2);
        postMouseEvent(button, MouseEvent.MOUSE_ENTERED, 0, point, 0, false);
        pressedReleasedClicked1(button, new Rectangle(0, 0, button.getWidth(), button.getHeight()), 1);
    }
    
    public void toggle(final int buttonId, final boolean targetState) {

        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                final AbstractButton button = (AbstractButton)componentArray.get(buttonId);
                
                if (button.isSelected() != targetState) {
                    clickButton(buttonId);
                }                 
            }
        });

    }        

    public void clickCombo(final int buttonId) {
        
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                final JComboBox button = (JComboBox)componentArray.get(buttonId);
                button.showPopup();
            }
        });                 
    }    
    
    public void setTextField(final int componentid, final String text) {
        
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                final JTextField textField = (JTextField)componentArray.get(componentid);
                textField.setText(text);
            }
        });                 
    }
    
    public void clickCell(final int componentid, final int rowid, final int columnid) {

        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {        
                    final Component component = componentArray.get(componentid);
                    Rectangle bounds = new Rectangle(0,0,0,0);
                    if (component instanceof JList) {
                        JList list = (JList) component;
                        bounds = list.getCellBounds(rowid, rowid);
                        list.ensureIndexIsVisible(rowid);
                    } else if (component instanceof JTable) {
                        JTable table = (JTable) component;            
                        bounds = table.getCellRect(rowid, columnid, false);
                        table.scrollRectToVisible(bounds);
                    } else if (component instanceof JTree) {
                        JTree tree = (JTree) component;
                        bounds = tree.getRowBounds(rowid);
                        tree.scrollRowToVisible(rowid);            
                    } else {
                        throw new RuntimeException("can't handle component");
                    }
                    pressedReleasedClicked1(component, bounds, 1);
            }
        });                 
                    
    }
    
    public void doubleClickCell(final int componentid, final int rowid, final int columnid) {

        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {   
                    final Component component = componentArray.get(componentid);
                    Rectangle bounds = new Rectangle(0,0,0,0);
                    if (component instanceof JList) {
                        JList list = (JList) component;
                        bounds = list.getCellBounds(rowid, rowid);
                        list.ensureIndexIsVisible(rowid);
                    } else if (component instanceof JTable) {
                        JTable table = (JTable) component;            
                        bounds = table.getCellRect(rowid, columnid, false);
                        table.scrollRectToVisible(bounds);
                    } else if (component instanceof JTree) {
                        JTree tree = (JTree) component;
                        bounds = tree.getRowBounds(rowid);
                        tree.scrollRowToVisible(rowid);                        
                    } else {
                        throw new RuntimeException("can't handle component");
                    }
                    pressedReleasedClicked1(component, bounds, 1);
                    pressedReleasedClicked1(component, bounds, 2);
            }
        });                         
    }
    
    public void rightClickCell(final int componentid, final int rowid, final int columnid) {

        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {   
                    final Component component = componentArray.get(componentid);
                    Rectangle bounds = new Rectangle(0,0,0,0);
                    if (component instanceof JList) {
                        JList list = (JList) component;
                        bounds = list.getCellBounds(rowid, rowid);
                        list.ensureIndexIsVisible(rowid);
                    } else if (component instanceof JTable) {
                        JTable table = (JTable) component;            
                        bounds = table.getCellRect(rowid, columnid, false);
                        table.scrollRectToVisible(bounds);
                    } else if (component instanceof JTree) {
                        JTree tree = (JTree) component;
                        bounds = tree.getRowBounds(rowid);
                        tree.scrollRowToVisible(rowid);                        
                    } else {
                        throw new RuntimeException("can't handle component");
                    }
                    
                    Point point = new Point(bounds.x + bounds.width/2,bounds.y + bounds.height/2);

                    postMouseEvent(component, MouseEvent.MOUSE_ENTERED, 0, point, 0, false);
                    postMouseEvent(component, MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON3_MASK, point, 1, !releaseIsPopupTrigger);
                    postMouseEvent(component, MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON3_MASK, point, 1, releaseIsPopupTrigger);
                    postMouseEvent(component, MouseEvent.MOUSE_CLICKED, MouseEvent.BUTTON3_MASK, point, 1, false); 
            }
        });                         
    }
    
    public void expandCollapseCell(final int componentid, final int rowid, final boolean expand) {
                       
       SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                final Component component = componentArray.get(componentid);
                
                if (component instanceof JTree) {
                    JTree tree = (JTree)component;
                    if (expand) {
                        tree.expandRow(rowid);
                    } else {
                        tree.collapseRow(rowid);
                    }
                }
            }
        });
    }
    
    public void selectTab(final int componentid, final int tabid) {
       SwingUtilities.invokeLater(new Runnable() {            
            @Override
            public void run() {
                final JTabbedPane tpane = (JTabbedPane) componentArray.get(componentid);
                tpane.setSelectedIndex(tabid);
            }
        });
    }
              
    public BufferedImage getWindowImage(final int windowId) {
       return windowArray.get(windowId).getImage();
    }

    public void closeWindow(final int windowId) {
        Window window = windowArray.get(windowId).getWindow();
        
        java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
                    new WindowEvent(window, WindowEvent.WINDOW_CLOSING) 
                );
    }
    
    public void toFront(final int windowId) {
        final Window window = windowArray.get(windowId).getWindow();
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                window.setAlwaysOnTop(true);
                window.toFront();
                window.requestFocus();
                window.setAlwaysOnTop(false);
            }            
        });
    }
        
    public void escape(final int windowid) {
        Window window = windowArray.get(windowid).getWindow();
        
        java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
                new KeyEvent( window, 
                            KeyEvent.KEY_PRESSED, 
                            System.currentTimeMillis(), 
                            0, 
                            KeyEvent.VK_ESCAPE,
                            (char)KeyEvent.VK_ESCAPE       
                        ));
    }    
    
    public void enter(final int windowid) {
        Window window = windowArray.get(windowid).getWindow();
        
        java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
                new KeyEvent( window, 
                            KeyEvent.KEY_PRESSED, 
                            System.currentTimeMillis(), 
                            0, 
                            KeyEvent.VK_ENTER,
                            (char)KeyEvent.VK_ENTER       
                        ));
    }     
        
/*    private ImageBin obtainImageBin() {
        return new ImageBin(windowImageMap.values());
    }*/
    
    private static void postMouseEvent(Component component, 
            int type, 
            int mask, 
            Point point,
            int clickCount,
            boolean popupTrigger) 
    {
        java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
                new MouseEvent( component, 
                            type, // event type 
                            0, 
                            mask, // modifiers 
                            point.x, // x 
                            point.y, // y
                            clickCount, 
                            popupTrigger                        
                        ));  
    }
    
    private static void pressedReleasedClicked1(Component component, Rectangle bounds, int clickCount) {
        Point point = new Point(bounds.x + bounds.width/2,bounds.y + bounds.height/2);
        
        postMouseEvent(component, MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1_MASK, point, clickCount, false);
        postMouseEvent(component, MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON1_MASK, point, clickCount, false);
        postMouseEvent(component, MouseEvent.MOUSE_CLICKED, MouseEvent.BUTTON1_MASK, point, clickCount, false);
    }
    
    private static class TreeNodeHelper {
    	private JsonNode node;
    	private ArrayNode children = JsonNodeFactory.instance.arrayNode();
    	
    	public TreeNodeHelper(JsonNode node) {
			super();
			this.node = node;
		}

    	void add(JsonNode child) {
    		children.add(child);
    	}

		JsonNode convert2json() {
    		ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
    		arrayNode.add(node);
    		arrayNode.add(children);
    		return arrayNode;
    	}

		public ArrayNode getChildren() {
			return children;
		}
		
    }
   
    public JsonNode getJson() {
		return json;
	}

	public void setJson(JsonNode json) {
		this.json = json;
	}

	public static class WindowWrapper {
		private Window window;
		private JsonNode json;
		private BufferedImage image;

		public WindowWrapper(Window window, JsonNode json, BufferedImage image) {
			super();
			this.window = window;
			this.json = json;
			this.image = image;
		}

		public Window getWindow() {
			return window;
		}

		public JsonNode getJson() {
			return json;
		}

		public BufferedImage getImage() {
			return image;
		}
		
	}
}
