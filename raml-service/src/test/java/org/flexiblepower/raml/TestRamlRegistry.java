/*-
 * #%L
 * dEF-Pi REST Orchestrator
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
package org.flexiblepower.raml;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * TestRamlRegistry
 *
 * @version 0.1
 * @since Aug 10, 2019
 */
@SuppressWarnings({"static-method", "javadoc"})
public class TestRamlRegistry {

    @Test
    public void patternTest() {
        Pattern p = RamlResourceRegistry.MethodRegistry.getPattern("test");
        Assertions.assertEquals("test", p.pattern());
        p = RamlResourceRegistry.MethodRegistry.getPattern("test/{id}/nogiets/{version}/zoiets");
        Assertions.assertTrue(p.matcher("test/2-._0~/nogiets/hoihoi.apx#2/zoiets").matches());
    }
}
