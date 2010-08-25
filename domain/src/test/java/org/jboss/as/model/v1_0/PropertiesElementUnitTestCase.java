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

package org.jboss.as.model.v1_0;

import org.jboss.as.model.Namespace;
import org.jboss.as.model.base.PropertiesElementTestBase;

/**
 * Test PropertiesElement with {@link Namespace#DOMAIN_1_0}.
 * 
 * @author Brian Stansberry
 */
public class PropertiesElementUnitTestCase extends PropertiesElementTestBase {

    /**
     * @param name
     */
    public PropertiesElementUnitTestCase(String name) {
        super(name);
        // TODO Auto-generated constructor stub
    }

    /* (non-Javadoc)
     * @see org.jboss.as.model.base.DomainModelElementTestBase#getTargetNamespace()
     */
    @Override
    protected String getTargetNamespace() {
        return Namespace.DOMAIN_1_0.getUriString();
    }

    /* (non-Javadoc)
     * @see org.jboss.as.model.base.DomainModelElementTestBase#getTargetNamespaceLocation()
     */
    @Override
    protected String getTargetNamespaceLocation() {
        return "jboss_7_0.xsd";
    }

}
