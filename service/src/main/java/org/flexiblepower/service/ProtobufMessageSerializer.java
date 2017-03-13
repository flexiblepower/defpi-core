package org.flexiblepower.service;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Parser;

public class ProtobufMessageSerializer extends MessageSerializer<GeneratedMessage> {
	private List<Parser<? extends GeneratedMessage>> parsers = new ArrayList<Parser<? extends GeneratedMessage>>();
	
	public ProtobufMessageSerializer(Class<? extends GeneratedMessage>... classes) {
		type = DescriptorType.PROTOBUF;
		try {
			for(Class<? extends GeneratedMessage> cls : classes){
				parsers.add((Parser<? extends GeneratedMessage>) cls.getMethod("parser").invoke(null));
			}
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			e.printStackTrace();
		}
	}
	
	public byte[] serialize(GeneratedMessage object){
		if(object != null)
			return object.toByteArray();
		return null;
	}
	
	public <T extends GeneratedMessage> T deserialize(byte[] data){
		for(Parser<? extends GeneratedMessage> parser : parsers){
			try {
				return (T) parser.parseFrom(data);
			} catch (InvalidProtocolBufferException e) {
				// Incorrect parser for this message
			}
		}
		return null;
	}

}
