package org.flexiblepower.serializers;

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

import org.flexiblepower.exceptions.SerializationException;

public class XSDMessageSerializer implements MessageSerializer<Object> {

    private final Map<String, Class<?>> classes = new HashMap<>();
    private final DescriptorType type = DescriptorType.XSD;
    private JAXBContext ctx;
    private Marshaller marshaller;
    private Unmarshaller unmarshaller;

    @Override
    public void addMessageClass(final Class<?> cls) {
        this.classes.put(cls.getName(), cls);
    }

    @Override
    public byte[] serialize(final Object object) throws SerializationException {
        if (!this.classes.values().contains(object.getClass())) {
            throw new IllegalArgumentException(
                    "Message type " + object.getClass().getName() + " is not registered with this MessageSerializer");
        }
        try {
            final Class<?> cls = this.classes.get(object.getClass().getName());
            return this.serialize(object, cls);
        } catch (final JAXBException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public Object deserialize(final byte[] data) throws SerializationException {
        if (this.unmarshaller == null) {
            try {
                this.init();
            } catch (final JAXBException e) {
                throw new SerializationException(e);
            }
        }

		try {
			return this.unmarshaller.unmarshal(new ByteArrayInputStream(data));
		} catch (final JAXBException e) {
			throw new SerializationException("Was not able to deserialize: " + e.getMessage());
		}
	}

    @Override
    public DescriptorType getType() {
        return this.type;
    }

    private <T> byte[] serialize(final Object object, final Class<T> cls) throws JAXBException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (this.marshaller == null) {
            this.init();
        }

        if (cls.isAnnotationPresent(XmlRootElement.class)) {
            this.marshaller.marshal(object, baos);
        } else {
            this.marshaller.marshal(
                    new JAXBElement<>(new QName("", cls.getAnnotation(XmlType.class).name()), cls, null, (T) object),
                    baos);
        }
        return baos.toByteArray();
    }

    /**
     * Lazy initialization to avoid having the constructor throw an exception
     *
     * @throws JAXBException
     */
    private void init() throws JAXBException {
        final Class<?>[] array = new Class<?>[this.classes.size()];
        this.ctx = JAXBContext.newInstance(this.classes.values().toArray(array));
        this.unmarshaller = this.ctx.createUnmarshaller();
        this.marshaller = this.ctx.createMarshaller();
    }

}
