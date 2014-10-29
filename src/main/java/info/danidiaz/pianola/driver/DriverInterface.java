package info.danidiaz.pianola.driver;

import com.fasterxml.jackson.databind.JsonNode;

public interface DriverInterface extends SnapshotInterface {
	public JsonNode snapshot() throws Exception;	
}
