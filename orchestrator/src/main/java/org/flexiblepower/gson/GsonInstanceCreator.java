package org.flexiblepower.gson;

import java.lang.reflect.Type;

import com.google.gson.InstanceCreator;

public class GsonInstanceCreator<T> implements InstanceCreator<T>{
	Class<T> c;
	public GsonInstanceCreator(Class<T> c){
		this.c = c;
	}
	public T createInstance(Type type) {
		try {
			return c.newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}
}
