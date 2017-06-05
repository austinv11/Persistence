public class SampleObjectImpl implements ISampleObject {
	
	private volatile String str;
	
	public SampleObjectImpl(String str) {
		this.str = str;
	}
	
	@Override
	public String getSampleField() {
		return str;
	}
	
	@Override
	public synchronized void setSampleField(String str) {
		this.str = str;
	}
	
	@Override
	public int hashCode() {
		return str.hashCode(); //It is expected that objects implement this!
	}
}
