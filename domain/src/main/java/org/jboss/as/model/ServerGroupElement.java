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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.socket.SocketBindingGroupRefElement;
import org.jboss.msc.service.Location;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A server group within a {@link Domain}.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServerGroupElement extends AbstractModelElement<ServerGroupElement> {

    private static final long serialVersionUID = 3780369374145922407L;

    private final String name;
    private final String profile;
    private final Map<DeploymentUnitKey, ServerGroupDeploymentElement> deploymentMappings = new TreeMap<DeploymentUnitKey, ServerGroupDeploymentElement>();
    private SocketBindingGroupRefElement bindingGroup;
    private JvmElement jvm;
    private PropertiesElement systemProperties;
    
    /**
     * Construct a new instance.
     *
     * @param location the declaration location of this element
     * @param name the name of the server group
     */
    public ServerGroupElement(final Location location, final String name, final String profile) {
        super(location);
        this.name = name;
        this.profile = profile;
    }
    
    public ServerGroupElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
        // Handle attributes
        String name = null;
        String profile = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        name = value;
                        break;
                    }
                    case PROFILE: {
                        profile = value;
                        break;
                    }
                    default: throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }
        if (profile == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.PROFILE));
        }
        this.name = name;
        this.profile = profile;
        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case JVM: {
                            if (jvm != null) {
                                throw new XMLStreamException(element.getLocalName() + " already defined", reader.getLocation());
                            }
                            jvm = new JvmElement(reader);
                            break;
                        }
                        case SOCKET_BINDING_GROUP: {
                            if (bindingGroup != null) {
                                throw new XMLStreamException(element.getLocalName() + " already defined", reader.getLocation());
                            }
                            bindingGroup = new SocketBindingGroupRefElement(reader);
                            break;
                        }
                        case DEPLOYMENTS: {
                            parseDeployments(reader);
                            break;
                        }
                        case SYSTEM_PROPERTIES: {
                            if (systemProperties != null) {
                                throw new XMLStreamException(element.getLocalName() + " already declared", reader.getLocation());
                            }
                            this.systemProperties = new PropertiesElement(reader);
                            break;
                        }
                        default: throw unexpectedElement(reader);
                    }
                    break;
                }
                default: throw unexpectedElement(reader);
            }
        }
    }

    /**
     * Gets the name of the server group.
     * 
     * @return the name. Will not be <code>null</code>
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the name of the profile that servers in the server group will run.
     * 
     * @return the profile name. Will not be <code>null</code>
     */
    public String getProfileName() {
        return profile;
    }
    
    /**
     * Gets the default jvm configuration for servers in this group. Which jvm to
     * use can be overridden at the {@link ServerElement#getJvm() server level}.
     * The details of the configuration of this jvm can be overridden at the
     * @{link {@link Host#getJvm(String) host level} or at the 
     * {@link ServerElement#getJvm() server level}.
     *     
     * @return the jvm configuration, or <code>null</code> if there is none
     */
    public JvmElement getJvm() {
        return jvm;
    }
    
    /**
     * Sets the default jvm configuration for servers in this group.
     *     
     * param jvm the jvm configuration. May be <code>null</code>
     */
    void setJvm(JvmElement jvm) {
        this.jvm = jvm;
    }
    
    /**
     * Gets the default
     * {@link Domain#getSocketBindingGroup(String) domain-level socket binding group}
     * assignment for this server group.
     * 
     * @return the socket binding group reference, or <code>null</code>
     */
    public SocketBindingGroupRefElement getSocketBindingGroup() {
        return bindingGroup;
    }
    
    /**
     * Sets the default
     * {@link Domain#getSocketBindingGroup(String) domain-level socket binding group}
     * assignment for this server group.
     * 
     * param ref the socket binding group reference, or <code>null</code>
     */
    void setSocketBindingGroupRefElement(SocketBindingGroupRefElement ref) {
        this.bindingGroup = ref;
    }
    
    /**
     * Gets the deployments mapped to this server group.
     * 
     * @return the deployments. May be empty but will not be <code>null</code>
     */
    public Set<ServerGroupDeploymentElement> getDeployments() {
        Set<ServerGroupDeploymentElement> deps = new LinkedHashSet<ServerGroupDeploymentElement>();
        for (Map.Entry<DeploymentUnitKey, ServerGroupDeploymentElement> entry : deploymentMappings.entrySet()) {
            deps.add(entry.getValue());
        }
        return Collections.unmodifiableSet(deps);
    }
    
    /**
     * Gets any system properties defined at the server group level for this 
     * server group. These properties can extend and override any properties
     * declared at the {@link Domain#getSystemProperties() domain level} and
     * may in turn be extended or overridden by any properties declared at the
     * {@link Host#getSystemProperties() host level} or the 
     * {@link ServerElement#getSystemProperties() server level}.
     * 
     * @return the system properties, or <code>null</code> if there are none
     */
    public PropertiesElement getSystemProperties() {
        return systemProperties;
    }

    /** {@inheritDoc} */
    public long elementHash() {
        long cksum = name.hashCode() & 0xffffffffL;
        cksum = Long.rotateLeft(cksum, 1) ^ profile.hashCode() & 0xffffffffL;
        cksum = calculateElementHashOf(deploymentMappings.values(), cksum);
        if (bindingGroup != null) cksum = Long.rotateLeft(cksum, 1) ^ bindingGroup.elementHash();
        if (systemProperties != null) cksum = Long.rotateLeft(cksum, 1) ^ systemProperties.elementHash();
        if (jvm != null) cksum = Long.rotateLeft(cksum, 1) ^ jvm.elementHash();
        return cksum;
    }

    /** {@inheritDoc} */
    protected void appendDifference(final Collection<AbstractModelUpdate<ServerGroupElement>> target, final ServerGroupElement other) {
        // FIXME implement appendDifference
        throw new UnsupportedOperationException("implement me");
    }

    /** {@inheritDoc} */
    protected Class<ServerGroupElement> getElementClass() {
        return ServerGroupElement.class;
    }

    /** {@inheritDoc} */
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.NAME.getLocalName(), name);
        streamWriter.writeAttribute(Attribute.PROFILE.getLocalName(), profile);

        
        if (jvm != null) {
            streamWriter.writeStartElement(Element.JVM.getLocalName());
            jvm.writeContent(streamWriter);
        }
        
        if (bindingGroup != null) {
            streamWriter.writeStartElement(Element.SOCKET_BINDING_GROUP.getLocalName());
            bindingGroup.writeContent(streamWriter);
        }
        
        if (! deploymentMappings.isEmpty()) {
            streamWriter.writeStartElement(Element.DEPLOYMENTS.getLocalName());
            for (ServerGroupDeploymentElement element : deploymentMappings.values()) {
                streamWriter.writeStartElement(Element.DEPLOYMENT.getLocalName());
                element.writeContent(streamWriter);
            }
            streamWriter.writeEndElement();
        }       
        
        if (systemProperties != null && systemProperties.size() > 0) {
            streamWriter.writeStartElement(Element.SYSTEM_PROPERTIES.getLocalName());
            systemProperties.writeContent(streamWriter);
        }

        streamWriter.writeEndElement();
    }
    
    private void parseDeployments(XMLExtendedStreamReader reader) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case DEPLOYMENT: {
                            final ServerGroupDeploymentElement deployment = new ServerGroupDeploymentElement(reader);
                            if (deploymentMappings.containsKey(deployment.getKey())) {
                                throw new XMLStreamException("Deployment " + deployment.getName() + 
                                        " with sha1 hash " + bytesToHexString(deployment.getSha1Hash()) + 
                                        " already declared", reader.getLocation());
                            }
                            deploymentMappings.put(deployment.getKey(), deployment);
                            break;
                        }
                        default: throw unexpectedElement(reader);
                    }
                    break;
                }
                default: throw unexpectedElement(reader);
            }
        }        
    }
}
