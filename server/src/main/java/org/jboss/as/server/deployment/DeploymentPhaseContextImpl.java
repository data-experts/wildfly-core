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

package org.jboss.as.server.deployment;

import java.util.List;

import org.jboss.as.controller.RequirementServiceTarget;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class DeploymentPhaseContextImpl extends SimpleAttachable implements DeploymentPhaseContext {
    private final RequirementServiceTarget serviceTarget;
    private final ServiceRegistry serviceRegistry;
    private final List<DeploymentUnitPhaseDependency> dependencies;
    private final DeploymentUnit deploymentUnitContext;
    private final Phase phase;

    DeploymentPhaseContextImpl(final RequirementServiceTarget serviceTarget, final ServiceRegistry serviceRegistry, final List<DeploymentUnitPhaseDependency> dependencies, final DeploymentUnit deploymentUnitContext, final Phase phase) {
        this.serviceTarget = serviceTarget;
        this.serviceRegistry = serviceRegistry;
        this.dependencies = dependencies;
        this.deploymentUnitContext = deploymentUnitContext;
        this.phase = phase;
    }

    @Override
    public ServiceName getPhaseServiceName() {
        return deploymentUnitContext.getServiceName().append(phase.name());
    }

    @Override
    public RequirementServiceTarget getRequirementServiceTarget() {
        return serviceTarget;
    }

    @Override
    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    @Override
    public DeploymentUnit getDeploymentUnit() {
        return deploymentUnitContext;
    }

    @Override
    public Phase getPhase() {
        return phase;
    }

    @Override
    public <T> void addDependency(ServiceName serviceName, AttachmentKey<T> attachmentKey) {
        addToAttachmentList(Attachments.NEXT_PHASE_ATTACHABLE_DEPS, new AttachableDependency(attachmentKey, serviceName, false));
    }

    @Override
    public <T> void requires(final ServiceName serviceName, final DelegatingSupplier<T> supplier) {
        this.dependencies.add(new SupplierDeploymentPhaseDependency<>(serviceName, supplier));
    }

    @Override
    public <T> void addDeploymentDependency(ServiceName serviceName, AttachmentKey<T> attachmentKey) {
        addToAttachmentList(Attachments.NEXT_PHASE_ATTACHABLE_DEPS, new AttachableDependency(attachmentKey, serviceName, true));
    }

    private static final class SupplierDeploymentPhaseDependency<T> implements DeploymentUnitPhaseDependency {
        private final ServiceName name;
        private final DelegatingSupplier<T> supplier;

        private SupplierDeploymentPhaseDependency(final ServiceName name, final DelegatingSupplier<T> supplier) {
            this.name = name;
            this.supplier = supplier;
        }

        @Override
        public void register(final ServiceBuilder<?> builder) {
            supplier.set(builder.requires(name));
        }
    }
}
