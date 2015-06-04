package info.danidiaz.pianola;

import info.danidiaz.pianola.Snapshot.WindowWrapper;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Path("/")
public class Resource {

    private static final int NEXT_ID_WRAPARAOUND = 30000;

	private int nextId;
	private Map<Integer,Snapshot> snapsotMap;
	private ImageBin imageBin;

    public Resource(int nextId) {
    	this(nextId, Collections.<Integer,Snapshot>emptyMap());
    }

    public Resource(int nextId, Map<Integer, Snapshot> snapsots) {
		super();
		this.nextId = nextId;
		this.snapsotMap = snapsots;
		this.imageBin = new ImageBin();
	}

	@POST
    @Path("snapshots")
    @Produces(MediaType.APPLICATION_JSON)
    public Response takeSnapshot() {
		try {
			Snapshot snapshot = Snapshot.build(imageBin, false);
			this.snapsotMap = Collections.singletonMap(this.nextId, snapshot); 
			URI createdUri = URI.create("/snapshots/" + this.nextId);
			return Response.created(createdUri).build();
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServerErrorException(500);
		} finally {
			this.nextId++;
			if (this.nextId == NEXT_ID_WRAPARAOUND) {
				this.nextId = 0;
			}
		}
    }

	@GET
    @Path("snapshots")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<Integer> listSnapshots() {
        return snapsotMap.keySet();
    }

	@GET
    @Path("snapshots/{snapshotId}")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonNode getSnapshot(@PathParam("snapshotId") Integer snapshotId) {
		if (snapsotMap.containsKey(snapshotId)) {
			return this.snapsotMap.get(snapshotId).getJson();
		} else {
			throw new ClientErrorException(404);
		}
    }


	@GET
    @Path("snapshots/{snapshotId}/windows")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Integer> getWindows( @PathParam("snapshotId") Integer snapshotId ) {
		if (snapsotMap.containsKey(snapshotId)) {
			List<WindowWrapper> windows = 
					this.snapsotMap.get(snapshotId).getWindowArray();
			List<Integer> windowIds = new ArrayList<>();
			for (int i = 0; i < windows.size(); i++) {
				windowIds.add(i);
			}
			return windowIds;
		} else {
			throw new ClientErrorException(404);
		}
    }

	/*
	@GET
    @Produces(MediaType.TEXT_PLAIN)
    public String test() {
        return "Test";
    }
    
    @GET
    @Path("jsonny")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonNode getUser() {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("user", "jDoe");
        return node;
    }
    */
}
