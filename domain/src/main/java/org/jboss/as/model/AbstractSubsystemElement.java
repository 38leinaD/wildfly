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
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.util.Collection;

/**
 * The base class of all subsystem elements.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractSubsystemElement<E extends AbstractSubsystemElement<E>> extends AbstractModelRootElement<E> implements ServiceActivator {

    private static final long serialVersionUID = 899219830157478004L;

    /**
     * Construct a new instance.
     *
     * @param location the declaration location of this element
     * @param elementName the element name
     */
    protected AbstractSubsystemElement(final Location location, final QName elementName) {
        super(location, elementName);
    }

    /**
     * Construct a new instance.
     *
     * @param reader the reader from which the element content should be read
     */
    protected AbstractSubsystemElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
    }

    /**
     * Activate this container within a service container.
     *
     * @param context the service activation context
     */
    public abstract void activate(final ServiceActivatorContext context);
}
