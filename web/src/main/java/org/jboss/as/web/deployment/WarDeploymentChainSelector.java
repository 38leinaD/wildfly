/*
* JBoss, Home of Professional Open Source
* Copyright 2010, Red Hat Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
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
package org.jboss.as.web.deployment;

import org.jboss.as.deployment.chain.DeploymentChainProvider;
import org.jboss.vfs.VirtualFile;

/**
 * War deployment chain selector.
 * 
 * @author Emanuel Muckenhuber
 */
public class WarDeploymentChainSelector implements DeploymentChainProvider.Selector {

    private static final String ARCHIVE_EXTENSION = ".war";
    private static final String DESCRIPTOR_PATH = "WEB-INF";
    
    public boolean supports(VirtualFile virtualFile) {
        return virtualFile.getName().toLowerCase().endsWith(ARCHIVE_EXTENSION) || virtualFile.getChild(DESCRIPTOR_PATH).exists();
    }

}

