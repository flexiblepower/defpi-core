/*-
 * #%L
 * dEF-Pi service creation maven plugin
 * %%
 * Copyright (C) 2017 - 2019 Flexible Power Alliance Network
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
package org.flexiblepower.plugin.servicegen.compiler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;

import org.flexiblepower.codegen.compiler.InterfaceCompiler;
import org.jsonschema2pojo.AnnotationStyle;
import org.raml.jaxrs.generator.Configuration;
import org.raml.jaxrs.generator.RamlScanner;

/**
 * JavaRamlCompiler
 *
 * @version 0.1
 * @since Jun 27, 2019
 */
public class JavaRamlCompiler implements InterfaceCompiler {

    private String basePackageName;

    /**
     * @param basePackageName the basePackageName to set
     */
    public void setBasePackageName(final String basePackageName) {
        this.basePackageName = basePackageName;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.codegen.compiler.InterfaceCompiler#compile(java.nio.file.Path, java.nio.file.Path)
     */
    @Override
    public void compile(final Path sourceFile, final Path targetPath) throws IOException {
        final Configuration config = new Configuration();

        final String[] generateTypesWith = new String[0];
        // generateTypesWith[0] = "jackson2";

        config.setResourcePackage(this.basePackageName);
        config.setModelPackage(this.basePackageName + ".model");
        config.setSupportPackage(this.basePackageName + ".support");

        config.setOutputDirectory(targetPath.toFile());
        config.setJsonMapper(AnnotationStyle.NONE);

        config.setJsonMapperConfiguration(Collections.emptyMap());
        config.setTypeConfiguration(generateTypesWith);

        final RamlScanner scanner = new RamlScanner(config);
        scanner.handle(sourceFile.toFile());
    }

}
