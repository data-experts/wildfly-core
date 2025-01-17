/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, JBoss Inc., and individual contributors as indicated
 * by the @authors tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.controller;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author Tomaz Cerar (c) 2015 Red Hat Inc.
 */
public abstract class PersistentResourceXMLParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext>, UnaryOperator<PersistentResourceXMLDescription> {

    private final AtomicReference<PersistentResourceXMLDescription> cachedDescription = new AtomicReference<>();

    public abstract PersistentResourceXMLDescription getParserDescription();

    /** @deprecated Experimental; for internal use only. May be removed at any time. */
    @Deprecated
    public final void cacheXMLDescription() {
        this.cachedDescription.updateAndGet(this);
    }

    @Override
    public PersistentResourceXMLDescription apply(PersistentResourceXMLDescription description) {
        return (description != null) ? description : this.getParserDescription();
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> modelNodes) throws XMLStreamException {
        // To reduce memory footprint, we only cache for a single parsing run
        new PersistentResourceXMLDescriptionReader(this.apply(this.cachedDescription.getAndSet(null))).readElement(reader, modelNodes);
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        new PersistentResourceXMLDescriptionWriter(this.apply(this.cachedDescription.get())).writeContent(writer, context);
    }
}
