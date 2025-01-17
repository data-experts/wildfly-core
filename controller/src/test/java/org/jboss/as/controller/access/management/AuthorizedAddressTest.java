/*
 * Copyright (C) 2014 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package org.jboss.as.controller.access.management;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.Set;
import java.util.logging.Level;

import org.jboss.as.controller.CapabilityServiceTarget;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.AuthorizationResult;
import org.jboss.as.controller.access.Environment;
import org.jboss.as.controller.access.ResourceAuthorization;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.client.MessageSeverity;
import org.jboss.as.controller.notification.Notification;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.junit.Test;
import org.wildfly.security.auth.server.SecurityIdentity;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2014 Red Hat, inc.
 */
public class AuthorizedAddressTest {

    /**
     * Test of authorizeAddress method, of class AuthorizedAddress.
     */
    @Test
    public void testAccessAuthorizedAddress() {
        ModelNode address = PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT,"test.war"), PathElement.pathElement(SUBSYSTEM, "Undertow")).toModelNode();
        ModelNode authorizedAddress = address;
        OperationContext context = new AuthorizationOperationContext(authorizedAddress.asString());
        ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(OP_ADDR).set(address);
        AuthorizedAddress expResult = new AuthorizedAddress(authorizedAddress, false);
        AuthorizedAddress result = AuthorizedAddress.authorizeAddress(context, operation);
        assertEquals(expResult, result);
    }

    /**
     * Test of authorizeAddress method, of class AuthorizedAddress.
     */
    @Test
    public void testAccessUnauthorizedAddress() {
        ModelNode address = PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT,"test.war"), PathElement.pathElement(SUBSYSTEM, "Undertow")).toModelNode();
        ModelNode authorizedAddress = PathAddress.EMPTY_ADDRESS.toModelNode();
        OperationContext context = new AuthorizationOperationContext(authorizedAddress.asString());
        ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(OP_ADDR).set(address);
        AuthorizedAddress expResult = new AuthorizedAddress(PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT,"<hidden>")).toModelNode(), true);
        AuthorizedAddress result = AuthorizedAddress.authorizeAddress(context, operation);
        assertEquals(expResult, result);
    }

    /**
     * Test of authorizeAddress method, of class AuthorizedAddress.
     */
    @Test
    public void testAccessParialUnauthorizedAddress() {
        ModelNode address = PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT,"test.war"), PathElement.pathElement(SUBSYSTEM, "Undertow")).toModelNode();
        ModelNode authorizedAddress = PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT,"test.war")).toModelNode();
        OperationContext context = new AuthorizationOperationContext(authorizedAddress.asString());
        ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(OP_ADDR).set(address);
        AuthorizedAddress expResult = new AuthorizedAddress(PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT,"test.war"), PathElement.pathElement(SUBSYSTEM,"<hidden>")).toModelNode(), true);
        AuthorizedAddress result = AuthorizedAddress.authorizeAddress(context, operation);
        assertEquals(expResult, result);
    }


    private static class AuthorizationOperationContext implements OperationContext {
        private final String authorizedAddress;

        private AuthorizationOperationContext(String authorizedAddress) {
            this.authorizedAddress = authorizedAddress;
        }

        @Override
        public void addStep(OperationStepHandler step, Stage stage) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void addStep(OperationStepHandler step, Stage stage, boolean addFirst) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void addStep(ModelNode operation, OperationStepHandler step, Stage stage) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void addStep(ModelNode operation, OperationStepHandler step, Stage stage, boolean addFirst) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void addStep(ModelNode response, ModelNode operation, OperationStepHandler step, Stage stage) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void addStep(ModelNode response, ModelNode operation, OperationStepHandler step, Stage stage, boolean addFirst) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void addModelStep(OperationDefinition stepDefinition, OperationStepHandler stepHandler, boolean addFirst) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void addModelStep(ModelNode response, ModelNode operation, OperationDefinition stepDefinition, OperationStepHandler stepHandler, boolean addFirst) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public InputStream getAttachmentStream(int index) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getAttachmentStreamCount() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ModelNode getResult() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean hasResult() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String attachResultStream(String mimeType, InputStream stream) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void attachResultStream(String uuid, String mimeType, InputStream stream) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ModelNode getFailureDescription() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean hasFailureDescription() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ModelNode getServerResults() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ModelNode getResponseHeaders() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void completeStep(RollbackHandler rollbackHandler) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void completeStep(ResultHandler resultHandler) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ProcessType getProcessType() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public RunningMode getRunningMode() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isBooting() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isNormalServer() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isRollbackOnly() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setRollbackOnly() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isRollbackOnRuntimeFailure() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isResourceServiceRestartAllowed() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void reloadRequired() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void restartRequired() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void revertReloadRequired() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void revertRestartRequired() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void runtimeUpdateSkipped() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ImmutableManagementResourceRegistration getResourceRegistration() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ManagementResourceRegistration getResourceRegistrationForUpdate() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ImmutableManagementResourceRegistration getRootResourceRegistration() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ServiceRegistry getServiceRegistry(boolean modify) throws UnsupportedOperationException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ServiceController<?> removeService(ServiceName name) throws UnsupportedOperationException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void removeService(ServiceController<?> controller) throws UnsupportedOperationException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public CapabilityServiceTarget getCapabilityServiceTarget() throws UnsupportedOperationException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void acquireControllerLock() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Resource createResource(PathAddress address) throws UnsupportedOperationException {
            throw new UnsupportedOperationException("Not supported yet.");
        }


        @Override
        public void addResource(PathAddress address, Resource toAdd) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void addResource(PathAddress address, int index, Resource toAdd) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Resource readResource(PathAddress relativeAddress) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Resource readResource(PathAddress relativeAddress, boolean recursive) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Resource readResourceFromRoot(PathAddress address) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Resource readResourceFromRoot(PathAddress address, boolean recursive) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Resource readResourceForUpdate(PathAddress relativeAddress) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Resource removeResource(PathAddress relativeAddress) throws UnsupportedOperationException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Resource getOriginalRootResource() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isModelAffected() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isResourceRegistryAffected() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isRuntimeAffected() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Stage getCurrentStage() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void report(MessageSeverity severity, String message) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean markResourceRestarted(PathAddress resource, Object owner) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean revertResourceRestarted(PathAddress resource, Object owner) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ModelNode resolveExpressions(ModelNode node) throws OperationFailedException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public <T> T getAttachment(AttachmentKey<T> key) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public <T> T attach(AttachmentKey<T> key, T value) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public <T> T attachIfAbsent(AttachmentKey<T> key, T value) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public <T> T detach(AttachmentKey<T> key) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public PathAddress getCurrentAddress() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getCurrentAddressValue() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getCurrentOperationName() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ModelNode getCurrentOperationParameter(String name) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ModelNode getCurrentOperationParameter(String name, boolean nullable) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public AuthorizationResult authorize(ModelNode operation) {
            String address = operation.get(OP_ADDR).asString();
            if(authorizedAddress.contains(address)) {
                return AuthorizationResult.PERMITTED;
            }
            return new AuthorizationResult(AuthorizationResult.Decision.DENY);
        }

        @Override
        public AuthorizationResult authorize(ModelNode operation, Set<Action.ActionEffect> effects) {
            String address = operation.get(OP_ADDR).asString();
            if(authorizedAddress.contains(address)) {
                return AuthorizationResult.PERMITTED;
            }
            return new AuthorizationResult(AuthorizationResult.Decision.DENY);
        }

        @Override
        public ResourceAuthorization authorizeResource(boolean attributes, boolean isDefaultResource) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public AuthorizationResult authorize(ModelNode operation, String attribute, ModelNode currentValue) {
            String address = operation.get(OP_ADDR).asString();
            if(authorizedAddress.contains(address)) {
                return AuthorizationResult.PERMITTED;
            }
            return new AuthorizationResult(AuthorizationResult.Decision.DENY);
        }

        @Override
        public AuthorizationResult authorize(ModelNode operation, String attribute, ModelNode currentValue, Set<Action.ActionEffect> effects) {
            String address = operation.get(OP_ADDR).asString();
            if(authorizedAddress.contains(address)) {
                return AuthorizationResult.PERMITTED;
            }
            return new AuthorizationResult(AuthorizationResult.Decision.DENY);
        }

        @Override
        public AuthorizationResult authorizeOperation(ModelNode operation) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public SecurityIdentity getSecurityIdentity() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Environment getCallEnvironment() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void registerCapability(RuntimeCapability capability) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void registerAdditionalCapabilityRequirement(String required, String dependent, String attribute) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean hasOptionalCapability(String required, String dependent, String attribute) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void requireOptionalCapability(String required, String dependent, String attribute) throws OperationFailedException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void deregisterCapabilityRequirement(String required, String dependent) {
            deregisterCapabilityRequirement(required, dependent, null);
        }

        @Override
        public void deregisterCapabilityRequirement(String required, String dependent, String attribute) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void deregisterCapability(String capabilityName) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public <T> T getCapabilityRuntimeAPI(String capabilityName, Class<T> apiType) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public <T> T getCapabilityRuntimeAPI(String capabilityBaseName, String dynamicPart, Class<T> apiType) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ServiceName getCapabilityServiceName(String capabilityName, Class<?> serviceType) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ServiceName getCapabilityServiceName(String capabilityBaseName, String dynamicPart, Class<?> serviceType) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ServiceName getCapabilityServiceName(String capabilityBaseName, Class<?> serviceType, String... dynamicParts) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public CapabilityServiceSupport getCapabilityServiceSupport() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void emit(Notification notification) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isDefaultRequiresRuntime() {
            return false;
        }

        @Override
        public void addResponseWarning(Level level, String warning) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void addResponseWarning(Level level, ModelNode warning) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

}
