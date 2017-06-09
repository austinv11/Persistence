import com.austinv11.persistence.Store;
import com.austinv11.persistence.impl.EncryptedPreProcessor;

import java.math.BigInteger;
import java.util.concurrent.ThreadLocalRandom;

public class SampleMain {
	
	private static final int port1 = 3000, port2 = 3001;
	
	public static void main(String[] args) throws InterruptedException {
		SampleNode node1 = new SampleNode(port1), node2 = new SampleNode(port2); //Setup the nodes
		
		Store<SampleObjectImpl> node1Store = node1.getManager().storeFor(SampleObjectImpl.class),
				node2Store = node2.getManager().storeFor(SampleObjectImpl.class);
		
		//Enables end-to-end encryption using the passkey "password"
		node1.getManager().setPreProcessor(new EncryptedPreProcessor("password"));
		node2.getManager().setPreProcessor(new EncryptedPreProcessor("password"));
		
		ISampleObject sample1 = node1.getManager().persist(new SampleObjectImpl(randString()));
		ISampleObject sample2 = node2.getManager().persist(new SampleObjectImpl(randString()));
		
		System.out.printf("Node 1 has %d objects%nNode 2 has %d objects%n", node1Store.size(), node2Store.size()); //Original Nodes should each only have 1 element stored
		
		node1.connectTo(port2); //Now connect node 1 to node 2 
		
		while (node1Store.size() == 1) {} //We can block until an object is added
		
		System.out.printf("Node 1 has %d objects%nNode 2 has %d objects%n", node1Store.size(), node2Store.size()); //Original Nodes should each now have 2 elements stored
		
		System.out.printf("Node 1 object has value %s%nNode 2 object has value %s%n", node1Store.get(node1.getManager().generateHash(sample2)).getSampleField(), sample2.getSampleField()); //Should be the same
		
		sample2.setSampleField("Blah!"); //Changes the local version (node2), it should automatically be reflected in the remote (node1)
		long sample2Hash = node1.getManager().generateHash(sample2);
		while (!node1Store.containsHash(sample2Hash)) {} //Block until the field is changed which causes the hash to be updated to match
		
		System.out.printf("Node 1 object has value %s%nNode 2 object has value %s%n", node1Store.get(node1.getManager().generateHash(sample2)).getSampleField(), sample2.getSampleField()); //Should be the same again but now with the updated value!
		
		System.out.printf("Node 1 has %d objects%nNode 2 has %d objects%n", node1Store.size(), node2Store.size()); //Original Nodes should each have 2 elements stored
		
		node1Store.removeHash(node1.getManager().generateHash(sample1)); //Remove the object from the shared cache
		while (node2Store.size() != 1) {} //We can block until an object is removed
		
		System.out.printf("Node 1 has %d objects%nNode 2 has %d objects%n", node1Store.size(), node2Store.size()); //Original Nodes should each only have 1 element stored
	}
	
	private static String randString() {
		return new BigInteger(130, ThreadLocalRandom.current()).toString(32);
	}
}
