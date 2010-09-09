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

package org.jboss.as.deployment.managedbean;

import org.jboss.as.deployment.DeploymentPhases;
import org.jboss.as.deployment.managedbean.config.InterceptorConfiguration;
import org.jboss.as.deployment.managedbean.config.ManagedBeanConfiguration;
import org.jboss.as.deployment.managedbean.container.FieldResourceInjection;
import org.jboss.as.deployment.managedbean.config.ManagedBeanConfigurations;
import org.jboss.as.deployment.managedbean.container.ManagedBeanContainer;
import org.jboss.as.deployment.managedbean.container.ManagedBeanInterceptor;
import org.jboss.as.deployment.managedbean.container.ManagedBeanObjectFactory;
import org.jboss.as.deployment.managedbean.container.MethodResourceInjection;
import org.jboss.as.deployment.managedbean.config.ResourceConfiguration;
import org.jboss.as.deployment.managedbean.container.ResourceInjection;
import org.jboss.as.deployment.naming.ContextService;
import org.jboss.as.deployment.naming.DuplicateBindingException;
import org.jboss.as.deployment.naming.JndiName;
import org.jboss.as.deployment.naming.ModuleContextConfig;
import org.jboss.as.deployment.naming.ResourceBinder;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.Values;

import javax.naming.Context;
import javax.naming.LinkRef;
import javax.naming.Reference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.jboss.as.deployment.naming.NamespaceBindings.getNamespaceBindings;

/**
 * Deployment unit processors responsible for adding deployment items for each managed bean configuration.
 *
 * @author John E. Bailey
 */
public class ManagedBeanDeploymentProcessor implements DeploymentUnitProcessor {
    public static final long PRIORITY = DeploymentPhases.INSTALL_SERVICES.plus(200L);

    /**
     * Process the deployment and add a managed bean service for each managed bean configuration in the deployment.
     *
     * @param context the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        final ManagedBeanConfigurations managedBeanConfigurations = context.getAttachment(ManagedBeanConfigurations.ATTACHMENT_KEY);
        if(managedBeanConfigurations == null) {
            return; // Skip deployments with no managed beans.
        }
        final ModuleContextConfig moduleContext = context.getAttachment(ModuleContextConfig.ATTACHMENT_KEY);
        if(moduleContext == null) {
            throw new DeploymentUnitProcessingException("Unable to deploy managed beans without a module naming context");
        }

        final BatchBuilder batchBuilder = context.getBatchBuilder();

        for(ManagedBeanConfiguration managedBeanConfiguration : managedBeanConfigurations.getConfigurations().values()) {
            processManagedBean(context, moduleContext, managedBeanConfiguration, batchBuilder);
        }
    }

    private void processManagedBean(final DeploymentUnitContext deploymentContext, final ModuleContextConfig moduleContext, final ManagedBeanConfiguration managedBeanConfiguration, final BatchBuilder batchBuilder) throws DeploymentUnitProcessingException {
        final Class<Object> beanClass = (Class<Object>)managedBeanConfiguration.getType();
        final String managedBeanName = managedBeanConfiguration.getName();

        final List<ResourceInjection<?>> resourceInjections = new ArrayList<ResourceInjection<?>>();
        final List<ManagedBeanInterceptor> interceptors = new ArrayList<ManagedBeanInterceptor>();
        final ManagedBeanService<Object> managedBeanService = new ManagedBeanService<Object>(new ManagedBeanContainer<Object>(beanClass, managedBeanConfiguration.getPostConstructMethods(), managedBeanConfiguration.getPreDestroyMethods(), resourceInjections, interceptors));

        final ServiceName moduleContextServiceName = moduleContext.getContextServiceName();

        final ServiceName managedBeanServiceName = ManagedBeanService.SERVICE_NAME.append(deploymentContext.getName(), managedBeanName);
        final BatchServiceBuilder<?> serviceBuilder = batchBuilder.addService(managedBeanServiceName, managedBeanService);

        final ServiceName managedBeanContextServiceName = moduleContextServiceName.append(managedBeanName, "context");
        final JndiName managedBeanContextJndiName = moduleContext.getContextName().append(managedBeanName + "-context");

        // Process managed bean resources
        for (ResourceConfiguration resourceConfiguration : managedBeanConfiguration.getResourceConfigurations()) {
            final ResourceInjection<Object> resourceInjection = processResource(deploymentContext, moduleContext, resourceConfiguration, batchBuilder, serviceBuilder, managedBeanContextServiceName, managedBeanContextJndiName);
            if(resourceInjection != null) {
                resourceInjections.add(resourceInjection);
            }
        }

        // Process managed bean interceptors
        for (InterceptorConfiguration interceptorConfiguration : managedBeanConfiguration.getInterceptorConfigurations()) {
            interceptors.add(processInterceptor(deploymentContext, moduleContext, interceptorConfiguration, batchBuilder, serviceBuilder, managedBeanContextServiceName, managedBeanContextJndiName));
        }

        final ContextService actualBeanContext = new ContextService(managedBeanContextJndiName);
        batchBuilder.addService(managedBeanContextServiceName, actualBeanContext)
            .addDependency(moduleContextServiceName, Context.class, actualBeanContext.getParentContextInjector());

        // Add an object factory reference for this managed bean
        final Reference managedBeanFactoryReference = ManagedBeanObjectFactory.createReference(beanClass, managedBeanServiceName);
        final ResourceBinder<Reference> managedBeanFactoryBinder = new ResourceBinder<Reference>(moduleContext.getContextName().append(managedBeanName), Values.immediateValue(managedBeanFactoryReference));
        final ServiceName referenceBinderName = moduleContextServiceName.append(managedBeanName);
        batchBuilder.addService(referenceBinderName, managedBeanFactoryBinder)
            .addDependency(moduleContextServiceName, Context.class, managedBeanFactoryBinder.getContextInjector())
            .addDependency(managedBeanServiceName);
    }

    private ResourceInjection<Object> processResource(final DeploymentUnitContext deploymentContext, final ModuleContextConfig moduleContext, final ResourceConfiguration resourceConfiguration, final BatchBuilder batchBuilder, final BatchServiceBuilder serviceBuilder, final ServiceName beanContextServiceName, final JndiName managedBeanContextJndiName) throws DeploymentUnitProcessingException {
        final JndiName localContextName = managedBeanContextJndiName.append(resourceConfiguration.getLocalContextName());
        final String targetContextName = resourceConfiguration.getTargetContextName();

        final ResourceInjection<Object> resourceInjection = getResourceInjection(resourceConfiguration);

        // Now add a binder for the local context
        final ServiceName binderName = beanContextServiceName.append(localContextName.getLocalName());
        if(resourceInjection != null) {
            serviceBuilder.addDependency(binderName, resourceInjection.getValueInjector());
        }

        final LinkRef linkRef = new LinkRef(targetContextName.startsWith("java") ? targetContextName : moduleContext.getContextName().append(targetContextName).getAbsoluteName());
        final boolean shouldBind;
        try {
            shouldBind = getNamespaceBindings(deploymentContext).addBinding(localContextName, linkRef);
        } catch (DuplicateBindingException e) {
            throw new DeploymentUnitProcessingException("Unable to process managed bean resource.", e);
        }
        if(shouldBind) {
            final ResourceBinder<LinkRef> resourceBinder = new ResourceBinder<LinkRef>(localContextName, Values.immediateValue(linkRef));

            final BatchServiceBuilder<Object> binderServiceBuilder = batchBuilder.addService(binderName, resourceBinder);
            binderServiceBuilder.addDependency(beanContextServiceName, Context.class, resourceBinder.getContextInjector());

            if(targetContextName.startsWith("java:")) {
                binderServiceBuilder.addOptionalDependency(ResourceBinder.JAVA_BINDER.append(targetContextName));
            } else {
                binderServiceBuilder.addOptionalDependency(moduleContext.getContextServiceName().append(targetContextName));
            }
        }

        return resourceInjection;
    }

    private ResourceInjection<Object> getResourceInjection(final ResourceConfiguration resourceConfiguration) {
        switch(resourceConfiguration.getTargetType()) {
            case FIELD:
                return new FieldResourceInjection<Object>(Values.immediateValue(Field.class.cast(resourceConfiguration.getTarget())), resourceConfiguration.getInjectedType().isPrimitive());
            case METHOD:
                return new MethodResourceInjection<Object>(Values.immediateValue(Method.class.cast(resourceConfiguration.getTarget())), resourceConfiguration.getInjectedType().isPrimitive());
            default:
                return null;
        }
    }

    private ManagedBeanInterceptor processInterceptor(final DeploymentUnitContext deploymentContext, final ModuleContextConfig moduleContext, final InterceptorConfiguration interceptorConfiguration, final BatchBuilder batchBuilder, final BatchServiceBuilder<?> serviceBuilder, final ServiceName managedBeanContextName, final JndiName managedBeanContextJndiName) throws DeploymentUnitProcessingException {
        final List<ResourceInjection<?>> resourceInjections = new ArrayList<ResourceInjection<?>>();

        for (ResourceConfiguration resourceConfiguration : interceptorConfiguration.getResourceConfigurations()) {
            final ResourceInjection<Object> resourceInjection = processResource(deploymentContext, moduleContext, resourceConfiguration, batchBuilder, serviceBuilder, managedBeanContextName, managedBeanContextJndiName);
            if(resourceInjection != null) {
                resourceInjections.add(resourceInjection);
            }
        }
        return new ManagedBeanInterceptor(interceptorConfiguration.getInterceptorClass(), interceptorConfiguration.getAroundInvokeMethod(), resourceInjections);
    }
}
