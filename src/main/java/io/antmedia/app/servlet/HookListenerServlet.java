package io.antmedia.app.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;

import io.antmedia.app.StreamMonitorApplication;
import io.antmedia.app.monitor.StreamMonitorManager;

public class HookListenerServlet extends HttpServlet{
	StreamMonitorManager manager;

	public StreamMonitorManager getManager() {
		if(manager == null) {
			manager = (StreamMonitorManager) StreamMonitorApplication.scope.getContext().getApplicationContext().getBean(StreamMonitorManager.BEAN_NAME);
		}
		return manager;
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String origin = req.getRemoteAddr();
		String streamId = req.getParameter("id");
		String action = req.getParameter("action");
		String streamName = req.getParameter("streamName");
		String category = req.getParameter("category");

		super.doPost(req, resp);
		
		getManager().newHookMessage(origin, streamId, action, streamName, category);
	}
}
