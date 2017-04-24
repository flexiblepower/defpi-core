package org.flexiblepower.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.flexiblepower.service.proto.SessionProto.Session;
import org.reflections.Reflections;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractService {

    public final static byte[] SUCCESS = new byte[] {0};
    public final static byte[] FAILURE = new byte[] {1};

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
        AbstractService.log.info("Started service {}", this.getClass().getName());
        final Context ctx = ZMQ.context(1);
        final Socket socket = ctx.socket(ZMQ.REP);
        socket.bind("tcp://*:4999");
        while (true) {
            final byte[] data = socket.recv(0);
            try {
                final Session session = Session.parseFrom(data);
                AbstractService.log.info("Received session data: {}", session);
                if (session.getMode() == Session.ModeType.CREATE) {
                    socket.send(this.createSession(session));
                } else {
                    socket.send(this.removeSession(session));
                }
            } catch (final Exception e) {
                AbstractService.log.error("Exception handling message: {}", e.getMessage());
                AbstractService.log.trace("Exception handing message", e);
                socket.send(AbstractService.FAILURE);
            }
        }
    }

    /**
     * Create new session based on the given Session message
     *
     * @param session Session protobuf deserialized message
     * @return Whether or not the session is created
     */
    private byte[] createSession(final Session session) {
        AbstractService.log.info("Received session create message: \n" + "    id           : " + session.getId() + "\n"
                + "    port         : " + session.getPort() + "\n" + "    subscribeHash: " + session.getSubscribeHash()
                + "\n" + "    publishHash  : " + session.getPublishHash());
        final ConnectionFactory connectionFactory = this.getFactoryFromHash(session.getSubscribeHash(),
                session.getPublishHash());
        if (connectionFactory != null) {
            final ServiceSession serviceSession = new ServiceSession(session, connectionFactory);
            AbstractService.sessions.put(session.getId(), serviceSession);
            serviceSession.startConsuming();
            return AbstractService.SUCCESS;
        }
        return AbstractService.FAILURE;
    }

    /**
     * Remove session identified by the Session message
     *
     * @param session Session protobuf deserialized message
     * @return Whether or not the session is removed
     */
    private byte[] removeSession(final Session session) {
        AbstractService.log.info("Received session remove message: \n" + "    id: " + session.getId());
        final ServiceSession serviceSession = AbstractService.sessions.get(session.getId());
        if (serviceSession != null) {
            AbstractService.log.debug("Stopping session");
            serviceSession.stopSession();
            return AbstractService.SUCCESS;
        }
        AbstractService.sessions.remove(session.getId());
        return AbstractService.FAILURE;
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
