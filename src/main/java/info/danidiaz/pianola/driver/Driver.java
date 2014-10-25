package info.danidiaz.pianola.driver;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.googlecode.jsonrpc4j.StreamServer;

import org.msgpack.MessagePack;
import org.msgpack.MessageTypeException;
import org.msgpack.packer.MessagePackPacker;
import org.msgpack.packer.Packer;
import org.msgpack.unpacker.MessagePackUnpacker;
import org.msgpack.unpacker.Unpacker;

public class Driver implements SnapshotFactory
{
    
    // http://www.iana.org/assignments/service-names-port-numbers/service-names-port-numbers.xml
    private final static int DEFAULT_PORT = 26060;
    private final static int MAX_THREADS = 1;
    
    boolean releaseIsPopupTrigger;
    
    private int lastSnapshotId = 0;
    private SnapshotImpl lastSnapshot = null; 
    
    private ByteArrayOutputStream imageBuffer = new ByteArrayOutputStream();
    
    // http://docs.oracle.com/javase/6/docs/api/java/lang/instrument/package-summary.html
    public static void premain(String agentArgs) {
        agentArgs = agentArgs == null ? "" : agentArgs;
        
        System.out.println( "Hi, I'm the agent, started with options: " + agentArgs );
                
        try {
            int port = DEFAULT_PORT;
            boolean releaseIsPopupTrigger = true;            
            String [] splittedArgs = agentArgs.split(",",0);
            for (int i=0;i<splittedArgs.length;i++) {
                String arg = splittedArgs[i];
                if (arg.startsWith("port")) {
                    port = Integer.decode(arg.substring(arg.indexOf('/')+1));
                } else if (arg.startsWith("popupTrigger")) {
                    releaseIsPopupTrigger =
                            arg.substring(arg.indexOf('/')+1).equals("release");
                }
            }                        
            
            MessagePack messagePack = new MessagePack(); 
        	JsonRpcServer jsonRpcServer = new JsonRpcServer(
        			new Driver(releaseIsPopupTrigger),
        			SnapshotFactory.class
        	);

            ServerSocket serverSocket = new ServerSocket(DEFAULT_PORT);
            StreamServer streamServer = new StreamServer(jsonRpcServer, MAX_THREADS, serverSocket);
            streamServer.start();

        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }        	
    }

    public Driver(boolean releaseIsPopupTrigger) {
        super();
        this.releaseIsPopupTrigger = releaseIsPopupTrigger;
    }
    
/*    private enum Action {
        CLICK("click") {
            @Override
            public void unpackInvoke(Unpacker unpacker, SnapshotImpl snapshot) throws IOException  {
                int cId = unpacker.readInt();
                snapshot.click(cId);                
            }  },
        DOUBLECLICK("doubleClick") {
            @Override
            public void unpackInvoke(Unpacker unpacker, SnapshotImpl snapshot) throws IOException {
                int cId = unpacker.readInt();
                snapshot.doubleClick(cId);
            }  },
        RIGHTCLICK("rightClick") {
            @Override
            public void unpackInvoke(Unpacker unpacker, SnapshotImpl snapshot) throws IOException {
                int cId = unpacker.readInt();
                snapshot.rightClick(cId);               
            }  },
        CLICKBUTTON("clickButton") {
            @Override
            public void unpackInvoke(Unpacker unpacker, SnapshotImpl snapshot) throws IOException {
                int buttonId = unpacker.readInt();
                snapshot.clickButton(buttonId);
            }  },
        TOGGLE("toggle") {
            @Override
            public void unpackInvoke(Unpacker unpacker, SnapshotImpl snapshot) throws IOException {
                int buttonId = unpacker.readInt();
                boolean targetState = unpacker.readBoolean();
                snapshot.toggle(buttonId,targetState);                
            }  },
        CLICKCOMBO("clickCombo") {
            @Override
            public void unpackInvoke(Unpacker unpacker, SnapshotImpl snapshot) throws IOException {
                int buttonId = unpacker.readInt();
                snapshot.clickCombo(buttonId);                
            }  },
        SETTEXTFIELD("setTextField") {
            @Override
            public void unpackInvoke(Unpacker unpacker, SnapshotImpl snapshot) throws IOException {
                int buttonId = unpacker.readInt();
                String text = unpacker.readString();
                snapshot.setTextField(buttonId,text);                
            }  },
        CLICKCELL("clickCell") {
            @Override
            public void unpackInvoke(Unpacker unpacker, SnapshotImpl snapshot) throws IOException {
                int componentId = unpacker.readInt();
                int rowId = unpacker.readInt();
                int columnId = unpacker.readInt();
                snapshot.clickCell(componentId,rowId,columnId);               
            }  },
        DOUBLECLICKCELL("doubleClickCell") {
            @Override
            public void unpackInvoke(Unpacker unpacker, SnapshotImpl snapshot) throws IOException {
                int componentId = unpacker.readInt();
                int rowId = unpacker.readInt();
                int columnId = unpacker.readInt();
                snapshot.doubleClickCell(componentId,rowId,columnId);                
            }  },
        RIGHTCLICKCEll("rightClickCell") {
            @Override
            public void unpackInvoke(Unpacker unpacker, SnapshotImpl snapshot) throws IOException {
                int componentId = unpacker.readInt();
                int rowId = unpacker.readInt();
                int columnId = unpacker.readInt();
                snapshot.rightClickCell(componentId,rowId,columnId);                
            }  },
        EXPANDCOLLAPSECELL("expandCollapseCell") {
            @Override
            public void unpackInvoke(Unpacker unpacker, SnapshotImpl snapshot) throws IOException  {
                int componentId = unpacker.readInt();
                int rowId = unpacker.readInt();
                int columnId = unpacker.readInt(); // not actually used
                boolean expand = unpacker.readBoolean();
                snapshot.expandCollapseCell(componentId,rowId,expand);                
            }  },
        SELECTTAB("selectTab") {
            @Override
            public void unpackInvoke(Unpacker unpacker, SnapshotImpl snapshot) throws IOException  {
                int componentId = unpacker.readInt();
                int tabid = unpacker.readInt();
                snapshot.selectTab(componentId,tabid);               
            }  },
        GETWINDOWIMAGE("getWindowImage") {
            @Override
            public void unpackInvokePack(Unpacker unpacker, SnapshotImpl snapshot,
                    ByteArrayOutputStream imageBuffer, Packer packer)
                    throws Exception {
                int windowId = unpacker.readInt();
                BufferedImage image = snapshot.getWindowImage(windowId);
                imageBuffer.reset();
                ImageIO.write(image, "png", imageBuffer);
                packer.write((int)0);
                packer.write(imageBuffer.toByteArray());
            } },
        CLOSEWINDOW("closeWindow") {
            @Override
            public void unpackInvoke(Unpacker unpacker, SnapshotImpl snapshot) throws IOException {
                int windowId = unpacker.readInt();
                snapshot.closeWindow(windowId);                
            }  },
        TOFRONT("toFront") {
            @Override
            public void unpackInvoke(Unpacker unpacker, SnapshotImpl snapshot) throws IOException {
                int windowId = unpacker.readInt();
                snapshot.toFront(windowId);                
            }  },
        ESCAPE("escape") {
            @Override
            public void unpackInvoke(Unpacker unpacker, SnapshotImpl snapshot) throws IOException {
                int windowId = unpacker.readInt();
                snapshot.escape(windowId);                
            }  },
        ENTER("enter") {
            @Override
            public void unpackInvoke(Unpacker unpacker, SnapshotImpl snapshot) throws IOException {
                int windowId = unpacker.readInt();
                snapshot.enter(windowId);                
            }  };
        
        private final String name;

        private Action(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
        
        public void unpackInvokePack(
                Unpacker unpacker,
                SnapshotImpl snapshot,
                ByteArrayOutputStream imageBuffer,
                Packer packer
            ) 
        throws Exception {
            try {
                unpackInvoke(unpacker, snapshot);
            } catch (Exception e) {
                e.printStackTrace();
                
                packer.write((int)1); // An error happened.
                packer.write((int)2); // Internal server error.
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                packer.write(sw.toString());
                return;
            }
            packer.write((int)0); // No error happened.
            packer.writeNil();
        }
        
        // For those requests which usually respond null
        public void unpackInvoke(Unpacker unpacker,SnapshotImpl snapshot) throws Exception { }
    }
    
    @Override
    public void run() {
        try {
            Map<String,Action> actionMap = new HashMap<String,Action>();
            for (Action a: Action.values()) {
                actionMap.put(a.getName(),a);
            }
                
            while (true) {
                Socket  clientSocket = serverSocket.accept();
                
                InputStream sistream =  new BufferedInputStream(clientSocket.getInputStream());

                // http://jackson.codehaus.org/1.9.4/javadoc/index.html
                JsonFactory jsonFactory = new JsonFactory();
                JsonParser jp = jsonFactory.createParser(sistream);

                Unpacker unpacker = new MessagePackUnpacker(messagePack,sistream);
                
                OutputStream sostream =  new BufferedOutputStream(clientSocket.getOutputStream());
                JsonGenerator jg = jsonFactory.createGenerator(sostream, JsonEncoding.UTF8);
                // jg.flush();
                
                Packer packer = new MessagePackPacker(messagePack,sostream);
               
                try {
                    String methodName = unpacker.readString();                
                    if (methodName.equals("snapshot")) {
                        lastSnapshotId++;
                        SnapshotImpl pianola = new SnapshotImpl(lastSnapshot,releaseIsPopupTrigger);
                        packer.write((int)0); // No error happened.
                        packer.write((int)lastSnapshotId);
                        pianola.buildAndWrite(packer);
                        lastSnapshot = pianola;     
                    } else {
                        int snapshotId = unpacker.readInt();
                        if (snapshotId == lastSnapshotId) {
                            if (actionMap.containsKey(methodName)) {
                                actionMap.get(methodName).unpackInvokePack(unpacker,
                                        lastSnapshot,
                                        imageBuffer,
                                        packer
                                    );
                            } else {
                                packer.write((int)1); // An error happened. 
                                packer.write((int)2); // Server error. 
                                packer.write("Unsupported method: " + methodName);
                            }
                        } else {
                            packer.write((int)1); // An error happened. 
                            packer.write((int)1); // Snapshot mismatch error. 
                            packer.write((int)snapshotId);
                            packer.write((int)lastSnapshotId); 
                        }
                    }
                    sostream.flush();
                } catch (IOException ioe) {
                    ioe.printStackTrace();    
                } catch (MessageTypeException msgte) {                
                    msgte.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    sistream.close();
                    sostream.close();
                    clientSocket.close();
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();    
        }  
    }*/

	@Override
	public ObjectNode snapshot() throws Exception {
		lastSnapshotId++;

        SnapshotImpl pianola = new SnapshotImpl(lastSnapshot,releaseIsPopupTrigger);
		JsonNode windows = pianola.buildAndWrite();

		JsonNodeFactory factory = JsonNodeFactory.instance;
		ObjectNode snapshotNode = factory.objectNode();
		snapshotNode.put("snapshotId",lastSnapshotId);
		snapshotNode.put("windows",windows);

        lastSnapshot = pianola;    

        return snapshotNode;
	} 
}
