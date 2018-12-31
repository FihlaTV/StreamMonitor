package io.antmedia.app.datastore;

public class Stream {
	private String streamId;
	private String origin;
	private boolean started = false;
	private boolean record = false;
	
	public String getStreamId() {
		return streamId;
	}
	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}
	public boolean isStarted() {
		return started;
	}
	public void setStarted(boolean started) {
		this.started = started;
	}
	public boolean isRecord() {
		return record;
	}
	public void setRecord(boolean record) {
		this.record = record;
	}
	public String getOrigin() {
		return origin;
	}
	public void setOrigin(String origin) {
		this.origin = origin;
	}
	
}
