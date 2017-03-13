package org.flexiblepower.service;
public abstract class MessageSerializer<T> {
	protected DescriptorType type = DescriptorType.NONE;

	public abstract <T2 extends T> T2 deserialize(byte[] data);
	public abstract byte[] serialize(T object);
	
	public enum DescriptorType {
		PROTOBUF, XSD, NONE
	}
}
