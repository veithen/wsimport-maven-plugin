/*-
 * #%L
 * wsimport-maven-plugin
 * %%
 * Copyright (C) 2018 Andreas Veithen
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
import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.xml.sax.SAXParseException;

import com.sun.codemodel.CodeWriter;
import com.sun.codemodel.writer.FileCodeWriter;
import com.sun.tools.ws.processor.generator.SeiGenerator;
import com.sun.tools.ws.processor.generator.ServiceGenerator;
import com.sun.tools.ws.processor.model.Model;
import com.sun.tools.ws.processor.modeler.wsdl.WSDLModeler;
import com.sun.tools.ws.wscompile.ErrorReceiver;
import com.sun.tools.ws.wscompile.WsimportOptions;
import com.sun.tools.ws.wsdl.parser.MetadataFinder;
import com.sun.tools.ws.wsdl.parser.WSDLInternalizationLogic;

public abstract class AbstractGenerateMojo extends AbstractMojo {
    @Parameter(property="project", readonly=true, required=true)
    private MavenProject project;

    @Parameter(required=true)
    private File[] wsdlFiles;

    /**
     * Corresponds to the {@code -p} option.
     */
    @Parameter
    private String packageName;

    @Parameter(required=true, defaultValue="${project.build.sourceEncoding}")
    private String outputEncoding;

    @Parameter
    private boolean generateService;

    @Parameter
    private boolean extension;

    public final void execute() throws MojoExecutionException, MojoFailureException {
        final Log log = getLog();
        File outputDirectory = getOutputDirectory();
        WsimportOptions options = new WsimportOptions();
        options.defaultPackage = packageName;
        for (File wsdlFile : wsdlFiles) {
            options.addWSDL(wsdlFile);
        }
        if (generateService) {
            File wsdlFile = wsdlFiles[0];
            String wsdlLocation = null;
            outer: for (Resource resource : getResources(project)) {
                File resourceDir = new File(resource.getDirectory());
                Deque<String> resourceComponents = new LinkedList<>();
                File file = wsdlFile;
                while (true) {
                    resourceComponents.addFirst(file.getName());
                    file = file.getParentFile();
                    if (file == null) {
                        break;
                    }
                    if (file.equals(resourceDir)) {
                        wsdlLocation = "/" + String.join("/", resourceComponents);
                        break outer;
                    }
                }
            }
            if (wsdlLocation == null) {
                // TODO: this is only suitable for test sources
                wsdlLocation = wsdlFiles[0].toURI().toString();
            }
            log.info("Using wsdlLocation = " + wsdlLocation);
            options.wsdlLocation = wsdlLocation;
        }
        options.sourceDir = outputDirectory;
        if (extension) {
            options.compatibilityMode = WsimportOptions.EXTENSION;
        }
        ErrorReceiver errorReceiver = new ErrorReceiver() {
            @Override
            public void fatalError(SAXParseException exception) {
                log.error(exception.getMessage());
                log.debug(exception);
            }
            
            @Override
            public void error(SAXParseException exception) {
                log.error(exception.getMessage());
                log.debug(exception);
            }
            
            @Override
            public void warning(SAXParseException exception) {
                log.warn(exception.getMessage());
                log.debug(exception);
            }
            
            @Override
            public void info(SAXParseException exception) {
                log.info(exception.getMessage());
                log.debug(exception);
            }

            @Override
            public void debug(SAXParseException exception) {
                log.debug(exception);
            }
        };
        MetadataFinder forest = new MetadataFinder(new WSDLInternalizationLogic(), options, errorReceiver);
        forest.parseWSDL();
        WSDLModeler wsdlModeler = new WSDLModeler(options, errorReceiver, forest);
        Model wsdlModel = wsdlModeler.buildModel();
        if (wsdlModel == null) {
            throw new MojoExecutionException("Code generation failed");
        }
        SeiGenerator.generate(wsdlModel, options, errorReceiver);
        if (generateService) {
            ServiceGenerator.generate(wsdlModel, options, errorReceiver);
        }
        outputDirectory.mkdirs();
        try {
            CodeWriter codeWriter = new FileCodeWriter(outputDirectory, outputEncoding);
            options.getCodeModel().build(codeWriter);
        } catch (IOException ex) {
            throw new MojoFailureException("Failed to write code", ex);
        }
        addSourceRoot(project, outputDirectory.getAbsolutePath());
    }

    protected abstract List<Resource> getResources(MavenProject project);
    protected abstract File getOutputDirectory();
    protected abstract void addSourceRoot(MavenProject project, String path);
}
