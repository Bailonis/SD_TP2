package tp1.api;

public class DomainInfo {
	private String uri;
	private long time;

	public DomainInfo(String uri) {

		this.uri = uri;
		this.time = System.currentTimeMillis();

	}

	public long getTime() {
		return this.time;
	}

	public void setTime() {
		this.time = System.currentTimeMillis();
	}

	public String getUri() {
		return this.uri;
	}



	@Override
	public String toString() {
		return String.format("URI: %s Received at: %s", this.uri, this.time);
	}
}
