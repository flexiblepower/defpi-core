/*-
 * #%L
 * dEF-Pi API
 * %%
 * Copyright (C) 2017 - 2018 Flexible Power Alliance Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.flexiblepower.raml.server;

import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import org.flexiblepower.proto.RamlProto.RamlRequest;
import org.flexiblepower.proto.RamlProto.RamlResponse;
import org.flexiblepower.raml.Example;
import org.flexiblepower.raml.example.Humans;
import org.flexiblepower.raml.example.model.Arm;
import org.flexiblepower.raml.example.model.ArmImpl;
import org.flexiblepower.raml.example.model.Gender;
import org.flexiblepower.raml.example.model.Human;
import org.flexiblepower.raml.example.model.Leg;
import org.flexiblepower.raml.example.model.LegImpl;
import org.flexiblepower.raml.example.model.Person;
import org.flexiblepower.raml.example.model.PersonImpl;
import org.flexiblepower.serializers.ProtobufMessageSerializer;
import org.flexiblepower.service.Connection;
import org.flexiblepower.service.ConnectionHandler;
import org.flexiblepower.service.InterfaceInfo;

/**
 * TestServerConnectionHandler
 *
 * @version 0.1
 * @since Aug 20, 2019
 */

@SuppressWarnings({"javadoc", "static-method"})
@InterfaceInfo(name = "Test server connection handler",
               version = "server",
               receivesHash = "12345",
               sendsHash = "54321",
               serializer = ProtobufMessageSerializer.class,
               receiveTypes = {RamlRequest.class},
               sendTypes = {RamlResponse.class})
public class TestServerConnectionHandler implements ConnectionHandler {

    /**
     * @param c The connection this handler handles
     */
    public TestServerConnectionHandler(final Connection c) {
        // Do whatever
    }

    public Humans getHumans() {
        return new Humans() {

            @Override
            public List<Human> getHumans(final String type) {
                return Collections.singletonList(this.getCustomPerson(type));
            }

            @Override
            public List<Human> getHumansAll() {
                return Collections.singletonList(this.getExampleHuman());
            }

            @Override
            public Human getHumansById(final String id, final String userType) {
                return this.getExampleHuman();
            }

            @Override
            public void putHumansById(final String id, final Human entity) {
                System.out.println(Double.parseDouble(((Person) entity).getName()));
            }

            @Override
            public Person getHumansPersonById(final String id, final String type) {
                return this.getExampleHuman();
            }

            private Person getCustomPerson(final String name) {
                final Person piet = new PersonImpl();
                piet.setName(name);
                piet.setWeight(80);
                final GregorianCalendar dob = new GregorianCalendar(1985, 4, 17);
                piet.setDateOfBirth(dob.getTime());
                piet.setActualGender(Gender.MALE);

                final Leg leg = new LegImpl();
                leg.setToes(5);
                final Arm arm = new ArmImpl();
                arm.setFingers(5);

                piet.setLimbs(Arrays.asList(leg, leg, arm, arm));
                return piet;
            }

            private Person getExampleHuman() {
                return this.getCustomPerson("Piet");
            }

        };
    }

    public Example getExample() {
        return new Example() {

            @Override
            public String getExampleText() {
                return this.getPersonalText("world");
            }

            @Override
            public String getPersonalText(final String name) {
                return "Hello " + name + "!";
            }

            @Override
            public String getPersonalText(final int reps) {
                String ret = "";
                for (int i = 0; i < reps; i++) {
                    ret += this.getExampleText() + "\n";
                }
                return ret;
            }

            @Override
            public float setStuff(final int id, final String q, final double test, final Map<String, String> body) {
                return (float) (id + Double.parseDouble(body.get(q)) + Math.sqrt(test));
            }

        };

    }

    // This would normally be implemented as a default implementation in the generated code
    public void handleRamlRequest(final RamlRequest msg) {
        RamlRequestHandler.handle(this, msg);
    }

    @Override
    public void onSuspend() {
        // TODO Auto-generated method stub
    }

    @Override
    public void resumeAfterSuspend() {
        // TODO Auto-generated method stub
    }

    @Override
    public void onInterrupt() {
        // TODO Auto-generated method stub
    }

    @Override
    public void resumeAfterInterrupt() {
        // TODO Auto-generated method stub
    }

    @Override
    public void terminated() {
        // TODO Auto-generated method stub
    }

}
