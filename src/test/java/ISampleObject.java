import com.austinv11.persistence.Getter;
import com.austinv11.persistence.Setter;

public interface ISampleObject {
	
	@Getter
	String getSampleField();
	
	@Setter
	void setSampleField(String str);
}
