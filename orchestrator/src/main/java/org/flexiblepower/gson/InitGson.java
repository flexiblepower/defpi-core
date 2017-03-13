package org.flexiblepower.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class InitGson {
	public static Gson create(){
		GsonBuilder gsonBuilder = new GsonBuilder();

		gsonBuilder.registerTypeAdapter(Service.class, new GsonInstanceCreator<Service>(Service.class));
		gsonBuilder.registerTypeAdapter(ServiceLabels.class, new GsonInstanceCreator<ServiceLabels>(ServiceLabels.class));
		gsonBuilder.registerTypeAdapter(ContainerCreate.class, new GsonInstanceCreator<ContainerCreate>(ContainerCreate.class));
		gsonBuilder.registerTypeAdapter(ContainerDescription.class, new GsonInstanceCreator<ContainerDescription>(ContainerDescription.class));
		gsonBuilder.registerTypeAdapter(ContainerInfo.class, new GsonInstanceCreator<ContainerInfo>(ContainerInfo.class));
		gsonBuilder.registerTypeAdapter(RegistryCatalog.class, new GsonInstanceCreator<RegistryCatalog>(RegistryCatalog.class));
		gsonBuilder.registerTypeAdapter(RegistryTags.class, new GsonInstanceCreator<RegistryTags>(RegistryTags.class));
		gsonBuilder.registerTypeAdapter(Repository.class, new GsonInstanceCreator<Repository>(Repository.class));
		gsonBuilder.registerTypeAdapter(Interface.class, new GsonInstanceCreator<Interface>(Interface.class));
		gsonBuilder.registerTypeAdapter(Link.class, new GsonInstanceCreator<Link>(Link.class));
		gsonBuilder.registerTypeAdapter(Host.class, new GsonInstanceCreator<Host>(Host.class));
		gsonBuilder.registerTypeAdapter(HostList.class, new GsonInstanceCreator<HostList>(HostList.class));
		
		Gson gson = gsonBuilder.create();
		
		return gson;
	}
}
