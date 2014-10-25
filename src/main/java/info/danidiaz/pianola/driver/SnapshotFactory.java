package info.danidiaz.pianola.driver;

import com.fasterxml.jackson.databind.JsonNode;

public interface SnapshotFactory extends Snapshot {
	public JsonNode snapshot() throws Exception;	
}
