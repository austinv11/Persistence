import com.austinv11.persistence.ConnectionSpy;
import com.austinv11.persistence.FailableValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public class LoggingSpy implements ConnectionSpy {
	
	@Nonnull
	@Override
	public FailableValue<Map<String, Object>> interceptConnectionRequest(@Nullable Integer v, long time, @Nullable Map<String, Object> data) {
		System.out.println("Connection request");
		return FailableValue.succeeded(null);
	}
	
	@Override
	public boolean interceptCompletedHandshake(@Nullable Integer v, long time, @Nullable Map<String, Object> data) {
		System.out.println("Completed handshake");
		return true;
	}
	
	@Override
	public void disconnected() {
		System.out.println("Disconnected");
	}
	
	@Override
	public void latencyCheck(long diff) {
		System.out.println(diff);
	}
}
