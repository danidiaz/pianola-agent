package info.danidiaz.pianola;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Path("/entrypoint")
public class FooResource {
	public FooResource() {
		System.out.println("is this a singleton, I wonder??????");
	} 
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
}
