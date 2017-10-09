/**
 * File TestJeroMq.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator;

import java.time.Duration;

import org.junit.Test;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

/**
 * TestJeroMq
 *
 * @author coenvl
 * @version 0.1
 * @since Aug 22, 2017
 */
@SuppressWarnings("static-method")
// @Ignore // This is not (yet) a real test
public class JeroMQTest {

    // @Test
    public void reqRepTest() throws InterruptedException {

        final Thread reqThread = new Thread(() -> {

            final Context ctx = ZMQ.context(1);
            final Socket s = ctx.socket(ZMQ.REQ);
            s.setReceiveTimeOut(10000);
            s.setSendTimeOut(1000);
            s.setImmediate(false); // Wait until the client has opened port

            System.out.println("REQ: Connecting...");
            s.connect("tcp://localhost:1234");
            System.out.println("REQ: Connected");

            while (true) {
                System.out.println("REQ: sending");
                final boolean sent = s.send("Zomaar wat data");
                System.out.println("REQ: sent: " + sent + ", receiving");
                final byte[] ans = s.recv();
                System.out.println("REQ: received reply: " + (ans == null ? "(null)" : new String(ans)));
                try {
                    Thread.sleep(500);
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
            }

        });

        final Thread repThread = new Thread(() -> {
            final Context ctx = ZMQ.context(1);
            final Socket s = ctx.socket(ZMQ.REP);

            s.setReceiveTimeOut(10000);
            s.setSendTimeOut(10000);

            System.out.println("REP: Connecting...");
            s.bind("tcp://0.0.0.0:1234");
            System.out.println("REP: Connected");

            while (true) {
                System.out.println("REP: Receiving");
                final byte[] ans = s.recv();
                System.out.println(
                        "REP: Received request: " + (ans == null ? "(null)" : new String(ans)) + ", sending...");
                final boolean sent = s.send("Zomaar een antwoord");
                System.out.println("REP: Sent: " + sent);
                try {
                    Thread.sleep(1000);
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        reqThread.start();
        Thread.sleep(Duration.ofSeconds(1).toMillis());
        repThread.start();

        Thread.sleep(Duration.ofMinutes(1).toMillis());
    }

    // @Test
    public void subSubTest() throws Exception {
        final Thread pubThread = new Thread(() -> {
            final Context ctx = ZMQ.context(1);
            final Socket s = ctx.socket(ZMQ.PUB);
            s.setSendTimeOut(1000);
            s.setImmediate(false); // Wait until the client has opened port

            System.out.println("PUB: Connecting...");
            s.connect("tcp://localhost:1234");
            System.out.println("PUB: Connected");

            while (true) {
                System.out.println("PUB: sending");
                final boolean sent = s.send("Zomaar_wat_data");
                System.out.println("PUB: sent: " + sent);
                try {
                    Thread.sleep(500);
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        final Thread subThread = new Thread(() -> {
            final Context ctx = ZMQ.context(1);
            final Socket s = ctx.socket(ZMQ.SUB);
            s.subscribe("");

            s.setReceiveTimeOut(1000);

            System.out.println("SUB: Connecting...");
            s.bind("tcp://0.0.0.0:1234");
            System.out.println("SUB: Connected");

            while (true) {
                System.out.println("SUB: Receiving");
                final byte[] ans = s.recv();
                System.out.println("SUB: Received message: " + (ans == null ? "(null)" : new String(ans)));
            }
        });

        pubThread.start();
        Thread.sleep(Duration.ofSeconds(3).toMillis());
        subThread.start();

        Thread.sleep(Duration.ofMinutes(1).toMillis());

    }

    @Test
    public void pushPullTest() throws Exception {
        final Thread pubThread = new Thread(() -> {
            final Context ctx = ZMQ.context(1);
            final Socket s = ctx.socket(ZMQ.PUSH);
            s.setSendTimeOut(1000);
            s.setImmediate(false); // Wait until the client has opened port

            System.out.println("PUSH: Connecting...");
            s.connect("tcp://localhost:1234");
            System.out.println("PUSH: Connected");

            while (true) {
                System.out.println("PUSH: sending");
                final boolean sent = s.send("Zomaar_wat_data");
                System.out.println("PUSH: sent: " + sent);
                try {
                    Thread.sleep(500);
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        final Thread subThread = new Thread(() -> {
            final Context ctx = ZMQ.context(1);
            final Socket s = ctx.socket(ZMQ.PULL);

            s.setReceiveTimeOut(1000);

            System.out.println("PULL: Connecting...");
            s.bind("tcp://0.0.0.0:1234");
            System.out.println("PULL: Connected");

            while (true) {
                System.out.println("PULL: Receiving");
                final byte[] ans = s.recv();
                System.out.println("PULL: Received message: " + (ans == null ? "(null)" : new String(ans)));
            }
        });

        pubThread.start();
        Thread.sleep(Duration.ofSeconds(3).toMillis());
        subThread.start();

        Thread.sleep(Duration.ofMinutes(1).toMillis());

    }
}
