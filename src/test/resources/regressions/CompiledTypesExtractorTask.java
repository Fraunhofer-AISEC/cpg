/**
 * OpenSpotLight - Open Source IT Governance Platform
 *
 * Copyright (c) 2009, CARAVELATECH CONSULTORIA E TECNOLOGIA EM INFORMATICA LTDA
 * or third-party contributors as indicated by the @author tags or express
 * copyright attribution statements applied by the authors.  All third-party
 * contributions are distributed under license by CARAVELATECH CONSULTORIA E
 * TECNOLOGIA EM INFORMATICA LTDA.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the GNU Lesser General Public License  for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 ***********************************************************************
 * OpenSpotLight - Plataforma de Governança de TI de Código Aberto
 *
 * Direitos Autorais Reservados (c) 2009, CARAVELATECH CONSULTORIA E TECNOLOGIA
 * EM INFORMATICA LTDA ou como contribuidores terceiros indicados pela etiqueta
 * @author ou por expressa atribuição de direito autoral declarada e atribuída pelo autor.
 * Todas as contribuições de terceiros estão distribuídas sob licença da
 * CARAVELATECH CONSULTORIA E TECNOLOGIA EM INFORMATICA LTDA.
 *
 * Este programa é software livre; você pode redistribuí-lo e/ou modificá-lo sob os
 * termos da Licença Pública Geral Menor do GNU conforme publicada pela Free Software
 * Foundation.
 *
 * Este programa é distribuído na expectativa de que seja útil, porém, SEM NENHUMA
 * GARANTIA; nem mesmo a garantia implícita de COMERCIABILIDADE OU ADEQUAÇÃO A UMA
 * FINALIDADE ESPECÍFICA. Consulte a Licença Pública Geral Menor do GNU para mais detalhes.
 *
 * Você deve ter recebido uma cópia da Licença Pública Geral Menor do GNU junto com este
 * programa; se não, escreva para:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.openspotlight.bundle.language.java.asm.tool;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.javabean.JavaBeanConverter;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.openspotlight.bundle.language.java.asm.CompiledTypesExtractor;
import org.openspotlight.bundle.language.java.asm.model.MethodDeclaration;
import org.openspotlight.bundle.language.java.asm.model.TypeDefinition;
import org.openspotlight.bundle.language.java.asm.model.TypeDefinitionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * This class is an {@link Task ant task} that reads compiled artifacts and generates an XML File with all declared java types.
 * 
 * @author porcelli
 */
public class CompiledTypesExtractorTask extends Task {

    /** The LOG. */
    private final Logger    LOG               = LoggerFactory.getLogger(CompiledTypesExtractorTask.class);

    /** The context name. Default value is "unknown". */
    private String          contextName       = "unknown";

    /** The context version. Default value is "unknown". */
    private String          contextVersion    = "unknown";

    /** The compiled artifacts. */
    private final Set<File> compiledArtifacts = new HashSet<File>();

    /** The xml output file name. */
    private String          xmlOutputFileName = "";

    /**
     * Adds a set of compiled artifacts.
     * 
     * @param artifactSet the artifact set
     */
    public void addCompiledArtifacts( final FileSet artifactSet ) {
        final DirectoryScanner scanner = artifactSet.getDirectoryScanner(getProject());
        for (final String activeFileName : scanner.getIncludedFiles()) {
            final File file = new File(artifactSet.getDir(getProject()), activeFileName);
            compiledArtifacts.add(file);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.tools.ant.Task#execute()
     */
    @Override
    public void execute() {
        if (isValid()) {
            try {
                final CompiledTypesExtractor typeExtractor = new CompiledTypesExtractor();
                final List<TypeDefinition> scannedTypes = typeExtractor.getJavaTypes(compiledArtifacts);
                final TypeDefinitionSet wrapper = new TypeDefinitionSet();
                wrapper.setTypes(scannedTypes);
                wrapper.setName(contextName);
                wrapper.setVersion(contextVersion);
                // XML Output Generation
                final XStream xstream = new XStream();
                xstream.aliasPackage("", "org.openspotlight.bundle.language.java.asm.model");
                xstream.alias("List", LinkedList.class);

                xstream.registerConverter(new JavaBeanConverter(xstream.getMapper()) {
                    @Override
                    @SuppressWarnings( "unchecked" )
                    public boolean canConvert( final Class type ) {
                        return type.getName() == MethodDeclaration.class.getName();
                    }
                });

                LOG.info("Starting XML Output generation.");
                final String dirName = xmlOutputFileName.substring(0, xmlOutputFileName.lastIndexOf("/"));
                new File(dirName).mkdirs();
                final OutputStream outputStream = new FileOutputStream(xmlOutputFileName);
                xstream.toXML(wrapper, outputStream);
                outputStream.flush();
                outputStream.close();
                LOG.info("Finished XML output file.");

            } catch (final IOException e) {
                e.printStackTrace();
            } finally {

            }
        } else {
            LOG.error("Invalid State: Missing XmlOutputFileName.");
        }
    }

    /**
     * Checks if the task state is valid.
     * 
     * @return true, if it is valid
     */
    private boolean isValid() {
        if (xmlOutputFileName == null || xmlOutputFileName.trim().length() == 0) {
            return false;
        }
        return true;
    }

    /**
     * Sets the context name.
     * 
     * @param contextName the new context name
     */
    public void setContextName( final String contextName ) {
        this.contextName = contextName;
    }

    /**
     * Sets the context version.
     * 
     * @param contextVersion the new context version
     */
    public void setContextVersion( final String contextVersion ) {
        this.contextVersion = contextVersion;
    }

    /**
     * Sets the xml output file name.
     * 
     * @param xmlOutputFileName the new xml output file name
     */
    public void setXmlOutputFileName( final String xmlOutputFileName ) {
        this.xmlOutputFileName = xmlOutputFileName;
    }
}
