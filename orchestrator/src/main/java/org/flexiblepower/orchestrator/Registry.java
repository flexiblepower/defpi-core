package org.flexiblepower.orchestrator;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.bson.Document;
import org.flexiblepower.gson.InitGson;
import org.flexiblepower.gson.Interface;
import org.flexiblepower.gson.RegistryCatalog;
import org.flexiblepower.gson.RegistryTags;
import org.flexiblepower.gson.ServiceLabels;
import org.flexiblepower.rest.ServicesRest;
import org.json.JSONObject;

import com.google.gson.reflect.TypeToken;

public class Registry {
	public static final String registryLink = "hub.servicelab.org/";
	public static final String registryPrefix = "dsefpi/";
	public static final String registryApiLink = "https://" + registryLink + "v2/";

	public static RegistryCatalog getRepositories() {

		Client client = ClientBuilder.newClient();
		WebTarget target = client.target(registryApiLink + "_catalog");
		Response response = target.request().get();
		return InitGson.create().fromJson(response.readEntity(String.class), RegistryCatalog.class);
	}

	public static void deleteTag(String repository, String tag){
		try{
			Client client = ClientBuilder.newClient();
			WebTarget target = client.target(registryApiLink + URLEncoder.encode(registryPrefix+repository, "UTF-8") + "/manifests/"+URLEncoder.encode(tag, "UTF-8"));
			Response response = target.request().accept("application/vnd.docker.distribution.manifest.v2+json").head();
			String digest = response.getHeaderString("DockerConnector-Content-Digest");
			ServicesRest.logger.info("Tag digest: "+digest);
			WebTarget target2 = client.target(registryApiLink + URLEncoder.encode(registryPrefix+repository, "UTF-8") + "/manifests/"+digest);
			Response response2 = target2.request().delete();
			ServicesRest.logger.info("Delete response: "+response2.getStatus()+"  -  "+response2.getStatusInfo().getReasonPhrase());
			
		} catch (UnsupportedEncodingException e){
			e.printStackTrace();
		}
	}
	
	public static RegistryTags getTags(String repository) {
		try {
	
			Client client = ClientBuilder.newClient();
			WebTarget target = client.target(registryApiLink + URLEncoder.encode(registryPrefix+repository, "UTF-8") + "/tags/list");

			Response response = target.request().get();
			return InitGson.create().fromJson(response.readEntity(String.class), RegistryTags.class);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static ServiceLabels getLabels(String repository, String tag){
		try{
			
			Client client = ClientBuilder.newClient();
			WebTarget target = client.target(registryApiLink + URLEncoder.encode(registryPrefix+repository, "UTF-8") + "/manifests/"+URLEncoder.encode(tag, "UTF-8"));
			Response response = target.request().get();
			String responseStr = response.readEntity(String.class);
			JSONObject v1Compatibility = new JSONObject(new JSONObject(responseStr).getJSONArray("history")
					.getJSONObject(0).getString("v1Compatibility"));
			String created = v1Compatibility.getString("created");
			JSONObject labels = v1Compatibility.getJSONObject("config").getJSONObject("Labels");
			List<Interface> interfaces = InitGson.create().fromJson(labels.getString("org.flexiblepower.interfaces"), new TypeToken<List<Interface>>(){}.getType());
			List<String> mappings = InitGson.create().fromJson(labels.getString("org.flexiblepower.mappings"), new TypeToken<List<String>>(){}.getType());
			List<String> ports = InitGson.create().fromJson(labels.getString("org.flexiblepower.ports"), new TypeToken<List<String>>(){}.getType());
			ServiceLabels serviceLabels = new ServiceLabels();
			serviceLabels.setName(labels.getString("org.flexiblepower.serviceName"));
			serviceLabels.setInterfaces(interfaces);
			serviceLabels.setCreated(created);
			serviceLabels.setMappings(mappings);
			serviceLabels.setPorts(ports);
			return serviceLabels;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
	public static Document getInterfaceDocument(String image, String tag){
		ServiceLabels serviceLabels = getLabels(image.replace(registryLink, "").replace(registryPrefix, ""), tag);

		return Document.parse("{\"interfaces\": "+InitGson.create().toJson(serviceLabels.getInterfaces())+"}");
	}
}
