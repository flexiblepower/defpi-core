/*-
 * #%L
 * dEF-Pi service creation maven plugin
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
package org.flexiblepower.plugin.servicegen.compiler.raml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import org.raml.jaxrs.generator.Configuration;
import org.raml.jaxrs.generator.GenerationException;
import org.raml.jaxrs.generator.RamlScanner;
import org.raml.v2.api.RamlModelBuilder;
import org.raml.v2.api.RamlModelResult;

/**
 * DefpiRamlScanner
 *
 * @version 0.1
 * @since Aug 9, 2019
 */
public class DefpiRamlScanner extends RamlScanner {

    List<String> resources;

    /**
     * @param configuration
     */
    public DefpiRamlScanner(final Configuration configuration) {
        super(configuration);
    }

    public List<String> getResourceNames() {
        return this.resources;
    }

    @Override
    public void handle(final InputStream stream, final File ramlDirectory) throws GenerationException, IOException {
        final RamlModelResult result = new RamlModelBuilder().buildApi(new InputStreamReader(stream),
                ramlDirectory.getAbsolutePath() + "/");
        if (result.hasErrors()) {
            throw new GenerationException(result.getValidationResults());
        }

        this.resources = new LinkedList<>();
        if (result.isVersion08() && (result.getApiV08() != null)) {
            super.handleRamlFile(result.getApiV08(), ramlDirectory);
            for (final org.raml.v2.api.model.v08.resources.Resource r : result.getApiV08().resources()) {
                this.resources.add(r.displayName().substring(1));
            }
        } else if (result.isVersion10() && (result.getApiV10() != null)) {
            super.handleRamlFile(result.getApiV10(), ramlDirectory);
            for (final org.raml.v2.api.model.v10.resources.Resource r : result.getApiV10().resources()) {
                this.resources.add(r.displayName().value().substring(1));
            }
        } else {
            throw new GenerationException("RAML file is neither v10 nor v08 api file");
        }

    }
}
