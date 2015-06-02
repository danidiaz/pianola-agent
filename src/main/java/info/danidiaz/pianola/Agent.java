package info.danidiaz.pianola;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;

//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.node.JsonNodeFactory;
//import com.fasterxml.jackson.databind.node.ObjectNode;

public class Agent 
{
	  private final static int DEFAULT_PORT = 26060;	
	
	
    // http://docs.oracle.com/javase/6/docs/api/java/lang/instrument/package-summary.html
    public static void premain(String agentArgs) {
        // https://nikolaygrozev.wordpress.com/2014/10/16/rest-with-embedded-jetty-and-jersey-in-a-single-jar-step-by-step/
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        Server jettyServer = new Server(DEFAULT_PORT);
        jettyServer.setHandler(context);

        // https://nikolaygrozev.wordpress.com/2014/10/16/rest-with-embedded-jetty-and-jersey-in-a-single-jar-step-by-step/
        // https://jersey.java.net/documentation/latest/appendix-properties.html
        // 
        ResourceConfig rc = new ResourceConfig();
        rc.registerInstances(new Resource(0));
        ServletContainer sc = new ServletContainer(rc);
        ServletHolder holder = new ServletHolder(sc);
        context.addServlet(holder, "/*");
        //ServletHolder holder = context.addServlet(org.glassfish.jersey.servlet.ServletContainer.class, "/*");
        //holder.setInitOrder(0);

        // Tells the Jersey Servlet which REST service/class to load.
        //holder.setInitParameter(
        //   ServerProperties.PROVIDER_PACKAGES,
        //   Agent.class.getPackage().getName());
 
        try {
            jettyServer.start();
            // jettyServer.join();
        } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
            // jettyServer.destroy();
        }
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

}
