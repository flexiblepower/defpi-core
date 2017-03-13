package org.flexiblepower.service;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

public class XSDMessageSerializer extends MessageSerializer<Object> {
	private Map<String, Class<?>> classes = new HashMap<String, Class<?>>();
	private JAXBContext ctx;
	private Marshaller marshaller;
	private Unmarshaller unmarshaller;
	
	public XSDMessageSerializer(Class<?>... classes) {
		type = DescriptorType.XSD;
		for(Class<?> cls : classes){
			this.classes.put(cls.getName(), cls);
		}
		try {
			ctx = JAXBContext.newInstance(classes);
			marshaller = ctx.createMarshaller();
			unmarshaller = ctx.createUnmarshaller();
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}

	public byte[] serialize(Object object) {
		try {
			Class<?> cls = classes.get(object.getClass().getName());
			return this.serialize(object, cls);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public <T> byte[] serialize(Object object, Class<T> cls) throws JAXBException{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		if (cls.isAnnotationPresent(XmlRootElement.class)) {
			marshaller.marshal(object, baos);
		} else {
			marshaller.marshal(
					new JAXBElement<T>(new QName("", cls.getAnnotation(XmlType.class).name()), cls, null, (T) object),
					baos);
		}
		return baos.toByteArray();
	}

	public Object deserialize(byte[] data) {
		for(Class<?> cls : classes.values()){
			try {
				JAXBElement<?> elem = (JAXBElement<?>) unmarshaller.unmarshal(new StreamSource(new ByteArrayInputStream(data)), cls);
				return elem.getValue();
			} catch (JAXBException e) {
				// Could not unmarshall
			}
		}
		return null;
	}

}
