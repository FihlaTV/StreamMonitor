package io.antmedia.app.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.WebApplicationContext;

import com.google.gson.Gson;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.app.StreamMonitorApplication;
import io.antmedia.app.monitor.StreamCapturer;
import io.antmedia.app.monitor.StreamMonitorManager;
import io.antmedia.rest.model.Result;

@Component
@Path("/")
public class RestService {
	Gson gson = new Gson();

	protected static final Logger logger = LoggerFactory.getLogger(RestService.class);

	@Context 
	private ServletContext servletContext;
	
	StreamMonitorManager manager;

	private ApplicationContext appCtx;

	private AntMediaApplicationAdapter appInstance;

	public StreamMonitorManager getManager() {
		if(manager == null) {
			manager = (StreamMonitorManager) StreamMonitorApplication.scope.getContext().getApplicationContext().getBean(StreamMonitorManager.BEAN_NAME);
		}
		return manager;
	}
	
	/**
	 * Add a stream to be recorded.
	 * Post method should be used.
	 * 
	 * application/json
	 * 
	 * @param streamId
	 * @return JSON data
	 */
	@POST
	@Path("/addStreamRecording/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Result addStream(@PathParam("id") String streamId, @RequestParam("id") String origin) {
		boolean result = true;
		
		String message = getManager().recordStream(streamId, getApplication().getScope().getName());
		Result operationResult = new Result(result);
		operationResult.setMessage(message);
		return operationResult;
	}
	
	@Nullable
	protected ApplicationContext getAppContext() {
		if (servletContext != null) {
			appCtx = (ApplicationContext) servletContext
					.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		}
		return appCtx;
	}
	
	public AntMediaApplicationAdapter getApplication() {
		if (appInstance == null) {
			ApplicationContext appContext = getAppContext();
			if (appContext != null) {
				appInstance = (AntMediaApplicationAdapter) appContext.getBean(AntMediaApplicationAdapter.BEAN_NAME);
			}
		}
		return appInstance;
	}

	/**
	 * Remove a stream from recording list.
	 * Post method should be used.
	 * 
	 * application/json
	 * 
	 * @param streamId
	 * @return JSON data
	 */
	@POST
	@Path("/removeStreamRecording/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Result removeStream(@PathParam("id") String streamId) {
		boolean result = true;
		String message = getManager().stopStreamRecording(streamId);
		Result operationResult = new Result(result);
		operationResult.setMessage(message);
		return operationResult;
	}
	
	/**
	 * Get a stream recording status.
	 * Get method should be used.
	 * 
	 * application/json
	 * 
	 * @param streamId
	 * @return true if recording, false otherwise 
	 */
	@GET
	@Path("/getStreamRecordingStatus/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public RecordingStatus getStreamRecordingStatus(@PathParam("id") String streamId) {
		ConcurrentHashMap<String, StreamCapturer> activeStreams = getManager().getActiveStreams();
		RecordingStatus status = new RecordingStatus();
		status.setRecording(activeStreams.get(streamId) != null);
		return status;
	}
	
	/**
	 * Get recording streams list.
	 * Get method should be used.
	 * 
	 * application/json
	 * 
	 * @return list of recording stream ids
	 */
	@GET
	@Path("/getStreamList")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public List<String> getStreamList() {
		ConcurrentHashMap<String, StreamCapturer> activeStreams = getManager().getActiveStreams();
		return new ArrayList<String>(activeStreams.keySet());
	}
}
