package org.flexiblepower.orchestrator;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
 
/**
 * @author Maarten Kollenstart
 * Make sure the REST API allows requests from other domains
 */
public class CORSResponseFilter
implements ContainerResponseFilter {
 
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException {
 
		MultivaluedMap<String, Object> headers = responseContext.getHeaders();
 
		headers.add("Access-Control-Allow-Origin", "*");
		headers.add("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT");			
		headers.add("Access-Control-Allow-Headers", "X-Requested-With, Content-Type");
	}
 
}