/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.model;

import org.jboss.msc.service.Location;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;
import java.util.Collection;
import java.util.Collections;

/**
 * A configuration element for a remote domain controller.
 *
 * @author John E. Bailey
 */
public class RemoteDomainControllerElement extends AbstractModelElement<RemoteDomainControllerElement> {
    private static final long serialVersionUID = -2704285433730705139L;

    private String host;
    private int port;

    public RemoteDomainControllerElement(Location location, final String host, final int port) {
        super(location);
        this.host = host;
        this.port = port;
    }

    public RemoteDomainControllerElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);

        // Handle attributes
        String host = null;
        Integer port = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case HOST: {
                        host = value;
                        break;
                    }
                    case PORT: {
                        port = Integer.valueOf(value);
                        break;
                    }
                    default: throw unexpectedAttribute(reader, i);
                }
            }
        }
        if(host == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.HOST.getLocalName()));
        }
        this.host = host;
        if(port == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.PORT.getLocalName()));
        }
        this.port = port.intValue();

        reader.discardRemainder();
    }

    @Override
    public long elementHash() {
        long cksum = host.hashCode() & 0xffffffffL;
        cksum = Long.rotateLeft(cksum, 1) ^ port & 0xffffffffL;
        return cksum;
    }

    @Override
    protected void appendDifference(Collection<AbstractModelUpdate<RemoteDomainControllerElement>> target, RemoteDomainControllerElement other) {

    }

    @Override
    protected Class<RemoteDomainControllerElement> getElementClass() {
        return RemoteDomainControllerElement.class;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.HOST.getLocalName(), host);
        streamWriter.writeAttribute(Attribute.PORT.getLocalName(), Integer.toString(port));
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
