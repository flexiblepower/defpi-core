/**
 * File Main.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator.pendingchange;

import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import com.mongodb.MongoClient;

/**
 * Main
 *
 * @author wilco
 * @version 0.1
 * @since Aug 7, 2017
 */
public class Main {

    @Entity("Test")
    public abstract static class A {

        @Id
        ObjectId id;

        boolean inUse = false;
    }

    @Entity("Test")
    public static class B extends A {

    }

    @Entity("Test")
    public static class C extends A {

    }

    public static void main(final String[] args) {
        final MongoClient mongoClient = new MongoClient("localhost");
        final Morphia morphia = new Morphia();
        morphia.map(A.class, B.class, C.class);
        final Datastore ds = morphia.createDatastore(mongoClient, "test");

        // ds.save(new B());
        // ds.save(new C());

        final A o = ds.findAndModify(ds.createQuery(A.class).filter("inUse", false).disableValidation(),
                ds.createUpdateOperations(A.class).set("inUse", true));
        // final Query<A> rq = ds.createQuery(A.class).disableValidation();
        // final List<A> returnsOnly = rq.asList();

        System.out.println(o);
    }

}
