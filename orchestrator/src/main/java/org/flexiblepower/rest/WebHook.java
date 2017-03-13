package org.flexiblepower.rest;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("registryHook")
public class WebHook {
	final static Logger logger = LoggerFactory.getLogger(WebHook.class);
	static volatile Map<String, Integer> pendigUpdates = new HashMap<String, Integer>();
	@POST
	@Consumes("application/vnd.docker.distribution.events.v1+json")
	public Response receiveWebHook(String json){
		/*//logger.info("Received webhook");
		JSONArray events = new JSONObject(json).getJSONArray("events");
		for (Iterator<?> iterator = events.iterator(); iterator.hasNext();) {
			JSONObject type = (JSONObject) iterator.next();
		//	logger.info("["+type.getString("action")+"]: "+type.getString("id")+"  -  "+type.getJSONObject("target").getString("repository")+"  -  "+type.getJSONObject("target").getInt("size")+"/"+type.getJSONObject("target").getInt("length"));
			if(type.getString("action").equals("push")){
				final String registry = type.getJSONObject("target").getString("repository");
				final Integer random = new Random().nextInt();
		//		logger.info("  push registry: "+registry);
				pendigUpdates.put(registry, random);
				(new Thread(){ 
					public void run(){
						try {
							Thread.sleep(60000);
							if(pendigUpdates.get(registry) == random){
		//						logger.info("  UPDATING CONTAINERS");
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}).start();
			}
		}
		return Response.ok().build();*/
		return Response.ok().build();
	}
}