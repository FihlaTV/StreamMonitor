package io.antmedia.app.datastore;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class StreamMonitorStore{

	private DB db;
	private BTreeMap<String, String> map;
	private static final String MAP_NAME = "STREAM";

	private Gson gson;
	protected static Logger logger = LoggerFactory.getLogger(StreamMonitorStore.class);

	public StreamMonitorStore(String dbName) {

		db = DBMaker
				.fileDB(dbName)
				.fileMmapEnableIfSupported()
				.transactionEnable()
				.closeOnJvmShutdown()
				.make();

		map = db.treeMap(MAP_NAME).keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING).counterEnable()
				.createOrOpen();
		GsonBuilder builder = new GsonBuilder();
		gson = builder.create();
	}


	public Stream updateStreamRecord(String streamId, boolean record) {
		Stream stream = null;
		synchronized (this) {
			try {
				String jsonString = map.get(streamId);
				if (jsonString != null) {
					stream = gson.fromJson(jsonString, Stream.class);
					stream.setRecord(record);
					map.replace(streamId, gson.toJson(stream));
				}
				else {
					stream = new Stream();
					stream.setStreamId(streamId);
					stream.setRecord(record);
					map.put(streamId, gson.toJson(stream));
				}
				db.commit();
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}

		return stream;
	}

	public Stream updateStreamStarted(String streamId, String origin, boolean started) {
		Stream stream = null;
		synchronized (this) {
			try {
				String jsonString = map.get(streamId);
				if (jsonString != null) {
					stream = gson.fromJson(jsonString, Stream.class);
					stream.setStarted(started);
					stream.setOrigin(origin);
					map.replace(streamId, gson.toJson(stream));
				}
				else {
					stream = new Stream();
					stream.setStreamId(streamId);
					stream.setStarted(started);
					stream.setOrigin(origin);
					map.put(streamId, gson.toJson(stream));
				}
				db.commit();
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}

		return stream;
	}
	
	public Stream getStream(String streamId) {
		return gson.fromJson(map.get(streamId), Stream.class);
	}

	public void deleteStream(String streamId) {
		map.remove(streamId);
		db.commit();
	}
}