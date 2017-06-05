import com.austinv11.persistence.PersistenceManager;

public class SampleNode {
	
	private final int port;
	private final PersistenceManager manager;
	
	public SampleNode(int port) {
		this.port = port;
		manager = new PersistenceManager().setServerPort(port).setConnectionSpy(new LoggingSpy());
	}
	
	public void connectTo(int port) {
		manager.connectTo("localhost", port);
	}
	
	public PersistenceManager getManager() {
		return manager;
	}
}
