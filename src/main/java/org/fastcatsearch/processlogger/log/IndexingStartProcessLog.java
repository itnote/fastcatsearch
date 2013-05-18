package org.fastcatsearch.processlogger.log;

import java.io.IOException;

import org.fastcatsearch.common.io.StreamInput;
import org.fastcatsearch.common.io.StreamOutput;

public class IndexingStartProcessLog implements ProcessLog {

	private String collection;
	private String indexingType;
	private long startTime;
	private boolean isScheduled;

	public IndexingStartProcessLog(){ }
	
	public IndexingStartProcessLog(String collection, String indexingType, long startTime, boolean isScheduled) {
		this.collection = collection;
		this.indexingType = indexingType;
		this.startTime = startTime;
		this.isScheduled = isScheduled;
	}

	public String getCollection() {
		return collection;
	}

	public String getIndexingType() {
		return indexingType;
	}

	public long getStartTime() {
		return startTime;
	}

	public boolean isScheduled() {
		return isScheduled;
	}

	@Override
	public void readFrom(StreamInput input) throws IOException {
		collection = input.readString();
		indexingType = input.readString();
		startTime = input.readLong();
		isScheduled = input.readBoolean();
	}

	@Override
	public void writeTo(StreamOutput output) throws IOException {
		output.writeString(collection);
		output.writeString(indexingType);
		output.writeLong(startTime);
		output.writeBoolean(isScheduled);
	}

}
