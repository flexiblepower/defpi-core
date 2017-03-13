package org.flexiblepower.rest;

import java.io.IOException;
import java.io.InputStreamReader;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

@Path("/")
public class UI {
	final static Logger logger = LoggerFactory.getLogger(UI.class);
	@GET
	@Produces(MediaType.TEXT_HTML)
	public String getIndex() throws IOException{
		return CharStreams.toString(new InputStreamReader(this.getClass().getResourceAsStream("/index.html"), Charsets.UTF_8));
	}	

	@GET
	@Path("script.js")
	@Produces("text/javascript")
	public String getScript() throws IOException{
		return CharStreams.toString(new InputStreamReader(this.getClass().getResourceAsStream("/script.js"), Charsets.UTF_8));
	}
	
	@GET
	@Path("style.css")
	@Produces("text/css")
	public String getStyle() throws IOException{
		return CharStreams.toString(new InputStreamReader(this.getClass().getResourceAsStream("/style.css"), Charsets.UTF_8));
	}
	
	@GET
	@Path("jsPlumb.js")
	@Produces("text/javascript")
	public String getJSPlumb() throws IOException{
		return CharStreams.toString(new InputStreamReader(this.getClass().getResourceAsStream("/jsPlumb.js"), Charsets.UTF_8));
	}
	
	@GET
	@Path("jsPlumbHandlers.js")
	@Produces("text/javascript")
	public String getJSPlumbHandlers() throws IOException{
		return CharStreams.toString(new InputStreamReader(this.getClass().getResourceAsStream("/jsPlumbHandlers.js"), Charsets.UTF_8));
	}
	
	@GET
	@Path("dagre.min.js")
	@Produces("text/javascript")
	public String getDagre() throws IOException{
		return CharStreams.toString(new InputStreamReader(this.getClass().getResourceAsStream("/dagre.min.js"), Charsets.UTF_8));
	}
	
	@GET
	@Path("api.js")
	@Produces("text/javascript")
	public String getApi() throws IOException{
		return CharStreams.toString(new InputStreamReader(this.getClass().getResourceAsStream("/api.js"), Charsets.UTF_8));
	}
	
	@GET
	@Path("model.js")
	@Produces("text/javascript")
	public String getModel() throws IOException{
		return CharStreams.toString(new InputStreamReader(this.getClass().getResourceAsStream("/model.js"), Charsets.UTF_8));
	}

}
