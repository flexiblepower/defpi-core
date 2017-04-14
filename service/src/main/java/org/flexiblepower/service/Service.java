package org.flexiblepower.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.flexiblepower.service.proto.SessionProto.Session;
import org.reflections.Reflections;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

import com.google.protobuf.InvalidProtocolBufferException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class Service {

    /**
     * Active sessions of the current service
     */
    public static Map<String, ServiceSession> sessions = new HashMap<>();

    /**
     * Available handlers for the different interfaces
     */
    protected Map<String, InterfaceHandler> handlers = new HashMap<>();

    /**
     * Main loop to receive Session messages from the orchestrator
     */
    public void start() {
        Service.log.info("Started service {}", this.getClass().getName());
        final Context ctx = ZMQ.context(1);
        final Socket socket = ctx.socket(ZMQ.REP);
        socket.bind("tcp://*:4999");
        while (true) {
            final byte[] data = socket.recv(0);
            Service.log.debug("Received data");
            try {
                final Session session = Session.parseFrom(data);
                System.out.println(session);
                boolean result;
                if (session.getMode() == Session.ModeType.CREATE) {
                    result = this.createSession(session);
                } else {
                    result = this.removeSession(session);
                }
                socket.send(new byte[] {(byte) (result ? 0 : 1)});
            } catch (final InvalidProtocolBufferException e) {
                socket.send(new byte[] {(byte) 1});
                e.printStackTrace();
            }
        }
    }

    /**
     * Create new session based on the given Session message
     *
     * @param session Session protobuf deserialized message
     * @return Whether or not the session is created
     */
    private boolean createSession(final Session session) {
        Service.log.info("Received session create message: \n" + "    id           : " + session.getId() + "\n"
                + "    port         : " + session.getPort() + "\n" + "    subscribeHash: " + session.getSubscribeHash()
                + "\n" + "    publishHash  : " + session.getPublishHash());
        final ConnectionFactory connectionFactory = this.getFactoryFromHash(session.getSubscribeHash(),
                session.getPublishHash());
        if (connectionFactory != null) {
            final ServiceSession serviceSession = new ServiceSession(session, connectionFactory);
            Service.sessions.put(session.getId(), serviceSession);
            serviceSession.startConsuming();
            return true;
        }
        return false;
    }

    /**
     * Remove session identified by the Session message
     *
     * @param session Session protobuf deserialized message
     * @return Whether or not the session is removed
     */
    private boolean removeSession(final Session session) {
        Service.log.info("Received session remove message: \n" + "    id: " + session.getId());
        final ServiceSession serviceSession = Service.sessions.get(session.getId());
        if (serviceSession != null) {
            Service.log.debug("Stopping session");
            serviceSession.stopSession();
            return true;
        }
        Service.sessions.remove(session.getId());
        return false;
    }

    /**
     * Get the ConnectionFactory corresponding to a specific interface
     *
     * @param interfaceName The human readable name of the interface
     * @return The corresponding ConnectionFactory, will return null when no factory is found
     */
    public ConnectionFactory getFactory(final String interfaceName) {
        final InterfaceHandler handler = this.handlers.get(interfaceName);
        if (handler == null) {
            return null;
        }

        return handler.getConnectionFactory();
    }

    /**
     * Get the ConnectionFactory corresponding to a set of hashes
     *
     * @param subscribeHash The subscribe hash the factory has to support
     * @param publishHash The publish hash the factory has to support
     * @return The corresponding ConnectionFactory, will return null when no factory is found
     */
    public ConnectionFactory getFactoryFromHash(final String subscribeHash, final String publishHash) {
        for (final Map.Entry<String, InterfaceHandler> handler : this.handlers.entrySet()) {
            if (handler.getValue().getSubscribeHash().equals(subscribeHash)
                    && handler.getValue().getPublishHash().equals(publishHash)) {
                return handler.getValue().getConnectionFactory();
            }
        }
        return null;
    }

    /**
     * Find factory based on reflection and the Factory annotation. The prefix (i.e. the package)
     * is the location the code will look for factories and the interface name is the matcher for
     * the annotation.
     *
     * @param prefix The prefix (i.e. the package) is the location the code will look for factories
     * @param interfaceName The interface will be used for matching the annotation
     * @return A class that extends ConnectionFactory or else null
     */
    protected Class<? extends ConnectionFactory> getFactory(final String prefix, final String interfaceName) {
        final Reflections reflections = new Reflections(prefix);
        final Set<Class<?>> factories = reflections.getTypesAnnotatedWith(Factory.class);
        for (final Class<?> factory : factories) {
            final Factory f = factory.getAnnotation(Factory.class);
            if (f.interfaceName().equalsIgnoreCase(interfaceName)) {
                return (Class<? extends ConnectionFactory>) factory;
            }
        }
        return null;
    }
}
