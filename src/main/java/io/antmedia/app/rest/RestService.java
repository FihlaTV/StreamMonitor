package io.antmedia.app.rest;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

import io.antmedia.app.StreamMonitorApplication;
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

	public StreamMonitorManager getManager() {
		if(manager == null) {
			manager = (StreamMonitorManager) StreamMonitorApplication.scope.getContext().getApplicationContext().getBean(StreamMonitorManager.BEAN_NAME);
		}
		return manager;
	}
	
	/**
	 * Add a stream to be recorded.
	 * Get method should be used.
	 * 
	 * application/json
	 * 
	 * @param streamId
	 * @return JSON data
	 */
	@GET
	@Path("/addStreamRecording/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Result addStream(@PathParam("id") String streamId) {
		boolean result = true;
		getManager().recordStream(streamId);
		Result operationResult = new Result(result);
		return operationResult;
	}

	/**
	 * Remove a stream from recording list.
	 * Get method should be used.
	 * 
	 * application/json
	 * 
	 * @param streamId
	 * @return JSON data
	 */
	@GET
	@Path("/removeStreamRecording/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Result removeStream(@PathParam("id") String streamId) {
		boolean result = true;
		getManager().stopStreamRecording(streamId);
		Result operationResult = new Result(result);
		return operationResult;
	}
}
