/*-
 * #%L
 * wsimport-maven-plugin
 * %%
 * Copyright (C) 2018 - 2023 Andreas Veithen
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
package com.github.veithen.maven.wsimport;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Resource;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(
        name = "generate-test-sources",
        defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES,
        threadSafe = true)
public class GenerateTestSourcesMojo extends AbstractGenerateMojo {
    @Parameter(
            required = true,
            defaultValue = "${project.build.directory}/generated-test-sources/wsimport")
    private File outputDirectory;

    @Override
    protected List<Resource> getResources(MavenProject project) {
        List<Resource> resources = new ArrayList<>();
        resources.addAll(project.getResources());
        resources.addAll(project.getTestResources());
        return resources;
    }

    @Override
    protected File getOutputDirectory() {
        return outputDirectory;
    }

    @Override
    protected void addSourceRoot(MavenProject project, String path) {
        project.addTestCompileSourceRoot(path);
    }
}
