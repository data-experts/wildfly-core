/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.controller.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NOTIFICATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NOTIFICATION_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROXIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_TYPES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESOURCE_ADDED_NOTIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESOURCE_REMOVED_NOTIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALIDATE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.test.TestUtils.createAttribute;
import static org.jboss.as.controller.test.TestUtils.createMetric;
import static org.jboss.as.controller.test.TestUtils.createOperationDefinition;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ManagementModel;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelControllerClientFactory;
import org.jboss.as.controller.ModelOnlyWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ResourceBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.TestModelControllerService;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.ValidateOperationHandler;
import org.jboss.as.controller.operations.global.GlobalNotifications;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.persistence.NullConfigurationPersister;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Base class for tests of invocations from a main controller to a remote proxied controller.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class AbstractProxyControllerTest {

    private ExecutorService executor;
    private ModelControllerClient mainControllerClient;
    private ModelControllerClient proxiedControllerClient;
    private ServiceContainer container;

    @Before
    public void setupController() throws Exception {
        executor = Executors.newCachedThreadPool();
        container = ServiceContainer.Factory.create("test");
        ServiceTarget target = container.subTarget();
        ControlledProcessState processState = new ControlledProcessState(true);

        ProxyModelControllerService proxyService = new ProxyModelControllerService(processState);
        target.addService(ServiceName.of("ProxyModelController")).setInstance(proxyService).install();

        ServiceBuilder<?> mainBuilder = target.addService(ServiceName.of("MainModelController"));
        Supplier<ModelController> proxy = mainBuilder.requires(ServiceName.of("ProxyModelController"));
        MainModelControllerService mainService = new MainModelControllerService(proxy, processState);
        mainBuilder.setInstance(mainService);
        mainBuilder.install();

        proxyService.awaitStartup(30, TimeUnit.SECONDS);
        mainService.awaitStartup(30, TimeUnit.SECONDS);
        // execute operation to initialize the controller model
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("setup");
        operation.get(OP_ADDR).setEmptyList();

        mainControllerClient = mainService.getModelControllerClientFactory().createSuperUserClient(executor);
        mainControllerClient.execute(operation);

        proxiedControllerClient = proxyService.getModelControllerClientFactory().createSuperUserClient(executor);
        proxiedControllerClient.execute(operation);
    }

    @After
    public void shutdownServiceContainer() {
        if (container != null) {
            container.shutdown();
            try {
                container.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                container = null;
            }
        }
        executor.shutdownNow();
        mainControllerClient = null;
        proxiedControllerClient = null;
    }

    @Test
    public void testRecursiveReadResourceDescriptionOperation() throws Exception {
        Operation operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION);
        operation.getOperation().get(PROXIES).set(true);
        operation.getOperation().get(RECURSIVE).set(true);

        ModelNode result = proxiedControllerClient.execute(operation);

        checkHostSubModelDescription(result.get(RESULT), false, false);

        result = mainControllerClient.execute(operation);
        checkRootSubModelDescription(result.get(RESULT), false, false);

        result = mainControllerClient.execute(operation);
        checkRootSubModelDescription(result.get(RESULT), false, false);
    }

    @Test
    public void testRecursiveReadResourceDescriptionOperationForAddressInOtherController() throws Exception {
        Operation operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, SERVER, "serverA");
        operation.getOperation().get(RECURSIVE).set(true);

        ModelNode result = mainControllerClient.execute(operation);

        checkHostSubModelDescription(result.get(RESULT), false, false);

        result = mainControllerClient.execute(operation);
        checkHostSubModelDescription(result.get(RESULT), false, false);
    }

    @Test
    public void testRecursiveReadResourceDescriptionWithOperations() throws Exception {
        Operation operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION);
        operation.getOperation().get(PROXIES).set(true);
        operation.getOperation().get(RECURSIVE).set(true);
        operation.getOperation().get(OPERATIONS).set(true);

        ModelNode result = mainControllerClient.execute(operation);
        checkRootSubModelDescription(result.get(RESULT), true, false);
    }

    @Test
    public void testRecursiveReadResourceDescriptionWitNotifications() throws Exception {
        Operation operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION);
        operation.getOperation().get(PROXIES).set(true);
        operation.getOperation().get(RECURSIVE).set(true);
        operation.getOperation().get(NOTIFICATIONS).set(true);

        ModelNode result = mainControllerClient.execute(operation);
        checkRootSubModelDescription(result.get(RESULT), false, true);
    }

    @Test
    public void testRecursiveReadSubModelOperation() throws Exception {
        Operation operation = createOperation(READ_RESOURCE_OPERATION);
        operation.getOperation().get(PROXIES).set(true);
        operation.getOperation().get(RECURSIVE).set(true);

        ModelNode result = proxiedControllerClient.execute(operation);
        checkHostNode(result.get(RESULT));

        result = mainControllerClient.execute(operation);
        checkRootNode(result.get(RESULT));
    }

    @Test
    public void testRecursiveReadSubModelOperationForAddressInOtherController() throws Exception {
        Operation operation = createOperation(READ_RESOURCE_OPERATION, SERVER, "serverA");
        operation.getOperation().get(RECURSIVE).set(true);

        ModelNode result = mainControllerClient.execute(operation);
        checkHostNode(result.get(RESULT));
    }

    @Test
    public void testWriteAttributeOperation() throws Exception {
        Operation write = createOperation(WRITE_ATTRIBUTE_OPERATION, "serverchild", "svrA", "child", "childA");
        write.getOperation().get(NAME).set("value");
        write.getOperation().get(VALUE).set("NewValue");
        proxiedControllerClient.execute(write);

        Operation read = createOperation(READ_RESOURCE_OPERATION, "serverchild", "svrA", "child", "childA");
        read.getOperation().get(RECURSIVE).set(true);
        ModelNode result = proxiedControllerClient.execute(read);
        assertEquals("NewValue", result.get(RESULT, "value").asString());

        read = createOperation(READ_RESOURCE_OPERATION, SERVER, "serverA", "serverchild", "svrA", "child", "childA");
        read.getOperation().get(RECURSIVE).set(true);
        result = mainControllerClient.execute(read);
        assertEquals("NewValue", result.get(RESULT, "value").asString());
    }

    @Test
    public void testWriteAttributeOperationInChildController() throws Exception {
        Operation write = createOperation(WRITE_ATTRIBUTE_OPERATION, SERVER, "serverA", "serverchild", "svrA", "child", "childA");
        write.getOperation().get(NAME).set("value");
        write.getOperation().get(VALUE).set("NewValue2");
        ModelNode result = mainControllerClient.execute(write);

        Operation read = createOperation(READ_RESOURCE_OPERATION, "serverchild", "svrA", "child", "childA");
        read.getOperation().get(RECURSIVE).set(true);
        result = proxiedControllerClient.execute(read);
        assertEquals("NewValue2", result.get(RESULT, "value").asString());

        read = createOperation(READ_RESOURCE_OPERATION, SERVER, "serverA", "serverchild", "svrA", "child", "childA");
        read.getOperation().get(RECURSIVE).set(true);
        result = mainControllerClient.execute(read);
        assertEquals("NewValue2", result.get(RESULT, "value").asString());
    }

    @Test
    public void testWriteAttributeOperationSanity() throws Exception {
        Operation write = createOperation(WRITE_ATTRIBUTE_OPERATION, "serverchild", "svrA", "child", "childA");
        write.getOperation().get(NAME).set("value");
        write.getOperation().get(VALUE).set("NewValue2");
        ModelNode result = proxiedControllerClient.execute(write);

        Operation read = createOperation(READ_RESOURCE_OPERATION, "serverchild", "svrA", "child", "childA");
        read.getOperation().get(RECURSIVE).set(true);
        result = proxiedControllerClient.execute(read);
        assertEquals("NewValue2", result.get(RESULT, "value").asString());
    }


    @Test
    public void testReadAttributeOperationInChildController() throws Exception {
        Operation read = createOperation(READ_ATTRIBUTE_OPERATION, "serverchild", "svrA", "child", "childA");
        read.getOperation().get(NAME).set("name");
        ModelNode result = proxiedControllerClient.execute(read);
        assertEquals("childName", result.get(RESULT).asString());

        read.getOperation().get(NAME).set("metric");
        result = proxiedControllerClient.execute(read);
        assertEquals(ModelType.INT, result.get(RESULT).getType());
    }

    @Test
    public void testReadOperationNames() throws Exception {
        Operation read = createOperation(READ_OPERATION_NAMES_OPERATION);
        ModelNode result = mainControllerClient.execute(read);
        checkOperationNames(result.get(RESULT), 22);

        read = createOperation(READ_OPERATION_NAMES_OPERATION, SERVER, "serverA");
        result = mainControllerClient.execute(read);
        checkOperationNames(result.get(RESULT), 23);

        read = createOperation(READ_OPERATION_NAMES_OPERATION, SERVER, "serverA", "serverchild", "svrA");
        result = mainControllerClient.execute(read);
        checkOperationNames(result.get(RESULT), 22);
    }

    @Test
    public void testReadOperationDescription() throws Exception {
        Operation read = createOperation(READ_OPERATION_DESCRIPTION_OPERATION);
        read.getOperation().get(NAME).set(READ_ATTRIBUTE_OPERATION);
        ModelNode result = mainControllerClient.execute(read);
        checkReadAttributeOperationDescription(result.get(RESULT));

        read = createOperation(READ_OPERATION_DESCRIPTION_OPERATION, SERVER, "serverA");
        read.getOperation().get(NAME).set(READ_ATTRIBUTE_OPERATION);
        result = mainControllerClient.execute(read);
        checkReadAttributeOperationDescription(result.get(RESULT));

        read = createOperation(READ_OPERATION_DESCRIPTION_OPERATION, SERVER, "serverA", "serverchild", "svrA");
        read.getOperation().get(NAME).set(READ_ATTRIBUTE_OPERATION);
        result = mainControllerClient.execute(read);
        checkReadAttributeOperationDescription(result.get(RESULT));
    }

    @Test
    public void testReadChildNames() throws Exception {
        Operation read = createOperation(READ_CHILDREN_NAMES_OPERATION);
        read.getOperation().get(CHILD_TYPE).set(SERVER);
        ModelNode result = mainControllerClient.execute(read).get(RESULT);
        assertNotNull(result);
        assertEquals(ModelType.LIST, result.getType());
        assertEquals(1, result.asList().size());
        assertEquals("serverA", result.get(0).asString());

        read = createOperation(READ_CHILDREN_NAMES_OPERATION, SERVER, "serverA");
        read.getOperation().get(CHILD_TYPE).set("serverchild");
        result = mainControllerClient.execute(read).get(RESULT);
        assertNotNull(result);
        assertEquals(ModelType.LIST, result.getType());
        assertEquals(1, result.asList().size());
        assertEquals("svrA", result.get(0).asString());

        read = createOperation(READ_CHILDREN_NAMES_OPERATION, SERVER, "serverA", "serverchild", "svrA");
        read.getOperation().get(CHILD_TYPE).set("child");
        result = mainControllerClient.execute(read).get(RESULT);
        assertNotNull(result);
        assertEquals(ModelType.LIST, result.getType());
        assertEquals(1, result.asList().size());
        assertEquals("childA", result.get(0).asString());

    }

    @Test
    public void testReadChildTypes() throws Exception {
        Operation read = createOperation(READ_CHILDREN_TYPES_OPERATION);
        ModelNode result = mainControllerClient.execute(read).get(RESULT);
        assertNotNull(result);
        assertEquals(ModelType.LIST, result.getType());
        List<ModelNode> nodes = result.asList();
        assertEquals(2, nodes.size());
        List<String> typeNames = Arrays.asList(nodes.get(0).asString(), nodes.get(1).asString());
        assertTrue(Arrays.asList(SERVER, "profile").containsAll(typeNames));

        read = createOperation(READ_CHILDREN_TYPES_OPERATION, SERVER, "serverA");
        assertNotNull(result);
        assertEquals(ModelType.LIST, result.getType());
        nodes = result.asList();
        assertEquals(2, nodes.size());
        typeNames = Arrays.asList(nodes.get(0).asString(), nodes.get(1).asString());
        assertTrue(Arrays.asList(SERVER, "profile").containsAll(typeNames));

        read = createOperation(READ_CHILDREN_TYPES_OPERATION, SERVER, "serverA", "serverchild", "svrA");
        result = mainControllerClient.execute(read).get(RESULT);
        assertNotNull(result);
        assertEquals(ModelType.LIST, result.getType());
        nodes = result.asList();
        assertEquals(1, nodes.size());
        assertEquals("child", nodes.get(0).asString());
    }

    @Test
    public void testValidateOperationOp() throws Exception {
        ModelNode candidate = createOperation(READ_CHILDREN_TYPES_OPERATION).getOperation();
        Operation validate = createOperation(VALIDATE_OPERATION);
        validate.getOperation().get(VALUE).set(candidate);
        ModelNode result = mainControllerClient.execute(validate);
        Assert.assertFalse(result.get(FAILURE_DESCRIPTION).asString(), result.hasDefined(FAILURE_DESCRIPTION));

        candidate = createOperation(READ_OPERATION_DESCRIPTION_OPERATION).getOperation();
        candidate.get(NAME).set("Does not matter");
        validate.getOperation().get(VALUE).set(candidate);
        result = mainControllerClient.execute(validate);
        Assert.assertFalse(result.get(FAILURE_DESCRIPTION).asString(), result.hasDefined(FAILURE_DESCRIPTION));

        candidate = createOperation(READ_OPERATION_DESCRIPTION_OPERATION).getOperation();
        candidate.get("Bad").set("Crap");
        validate.getOperation().get(VALUE).set(candidate);
        result = mainControllerClient.execute(validate);
        Assert.assertEquals(FAILED, result.get(OUTCOME).asString());

        candidate = createOperation(READ_CHILDREN_TYPES_OPERATION, SERVER, "serverA", "serverchild", "svrA").getOperation();
        validate.getOperation().get(VALUE).set(candidate);
        result = mainControllerClient.execute(validate);
        Assert.assertFalse(result.get(FAILURE_DESCRIPTION).asString(), result.hasDefined(FAILURE_DESCRIPTION));

        candidate = createOperation(READ_OPERATION_DESCRIPTION_OPERATION, SERVER, "serverA", "serverchild", "svrA").getOperation();
        candidate.get(NAME).set("Does not matter");
        validate.getOperation().get(VALUE).set(candidate);
        result = mainControllerClient.execute(validate);
        Assert.assertFalse(result.get(FAILURE_DESCRIPTION).asString(), result.hasDefined(FAILURE_DESCRIPTION));

        candidate = createOperation(READ_CHILDREN_TYPES_OPERATION, SERVER, "serverA", "serverchild", "svrA").getOperation();
        candidate.get("Bad").set("Crap");
        validate.getOperation().get(VALUE).set(candidate);
        result = mainControllerClient.execute(validate);
        Assert.assertEquals(FAILED, result.get(OUTCOME).asString());
    }

    private void checkReadAttributeOperationDescription(ModelNode result) {
        assertEquals(READ_ATTRIBUTE_OPERATION, result.get(OPERATION_NAME).asString());
        assertEquals(ModelType.STRING, result.require(REQUEST_PROPERTIES).require(NAME).require(TYPE).asType());
        assertEquals(ModelType.OBJECT, result.require(REPLY_PROPERTIES).require(TYPE).asType());
    }

    private void checkOperationNames(ModelNode operationNamesList, int size) {
        assertTrue(operationNamesList.isDefined());
        assertEquals(ModelType.LIST, operationNamesList.getType());
        assertEquals(size, operationNamesList.asList().size());
    }

    private void checkRootSubModelDescription(ModelNode result, boolean operations, boolean notifications) {
        assertEquals("description", result.get(DESCRIPTION).asString());
        assertEquals("server", result.get(CHILDREN, SERVER, DESCRIPTION).asString());
        assertEquals(1, result.get(CHILDREN, SERVER, MODEL_DESCRIPTION).keys().size());
        assertEquals("profile", result.get(CHILDREN, PROFILE, DESCRIPTION).asString());
        assertEquals(1, result.get(CHILDREN, PROFILE, MODEL_DESCRIPTION).keys().size());

        if (operations) {
            Set<String> ops = result.require(OPERATIONS).keys();
            assertTrue(ops.contains(READ_ATTRIBUTE_OPERATION));
            assertTrue(ops.contains(READ_CHILDREN_NAMES_OPERATION));
            assertTrue(ops.contains(READ_CHILDREN_TYPES_OPERATION));
            assertTrue(ops.contains(READ_OPERATION_DESCRIPTION_OPERATION));
            assertTrue(ops.contains(READ_OPERATION_NAMES_OPERATION));
            assertTrue(ops.contains(READ_RESOURCE_DESCRIPTION_OPERATION));
            assertTrue(ops.contains(READ_RESOURCE_OPERATION));
            assertTrue(ops.contains(WRITE_ATTRIBUTE_OPERATION));
            for (String op : ops) {
                assertEquals(op, result.require(OPERATIONS).require(op).require(OPERATION_NAME).asString());
            }
        }

        if (notifications) {
            Set<String> notifs = result.require(NOTIFICATIONS).keys();
            assertTrue(notifs.contains(RESOURCE_ADDED_NOTIFICATION));
            assertTrue(notifs.contains(RESOURCE_REMOVED_NOTIFICATION));
            assertTrue(notifs.contains(ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION));
            for (String notif : notifs) {
                assertEquals(notif, result.require(NOTIFICATIONS).require(notif).require(NOTIFICATION_TYPE).asString());
            }
        }

        ModelNode proxy = result.get(CHILDREN, SERVER, MODEL_DESCRIPTION, "serverA");

        checkHostSubModelDescription(proxy, operations, notifications);
    }

    private void checkHostSubModelDescription(ModelNode result, boolean operations, boolean notifications) {

        assertEquals(result.toString(), 6, result.keys().size());
        assertEquals("description", result.get(DESCRIPTION).asString());
        assertEquals("serverchild", result.get(CHILDREN, "serverchild", DESCRIPTION).asString());
        assertEquals(1, result.get(CHILDREN, "serverchild", MODEL_DESCRIPTION).keys().size());

        if (operations) {
            Set<String> ops = result.require(OPERATIONS).keys();
            assertTrue(ops.contains(READ_ATTRIBUTE_OPERATION));
            assertTrue(ops.contains(READ_CHILDREN_NAMES_OPERATION));
            assertTrue(ops.contains(READ_CHILDREN_TYPES_OPERATION));
            assertTrue(ops.contains(READ_OPERATION_DESCRIPTION_OPERATION));
            assertTrue(ops.contains(READ_OPERATION_NAMES_OPERATION));
            assertTrue(ops.contains(READ_RESOURCE_DESCRIPTION_OPERATION));
            assertTrue(ops.contains(READ_RESOURCE_OPERATION));
            assertTrue(ops.contains(WRITE_ATTRIBUTE_OPERATION));
            for (String op : ops) {
                assertEquals(op, result.require(OPERATIONS).require(op).require(OPERATION_NAME).asString());
            }
        }

        if (notifications) {
            Set<String> notifs = result.require(NOTIFICATIONS).keys();
            assertTrue(notifs.contains(RESOURCE_ADDED_NOTIFICATION));
            assertTrue(notifs.contains(RESOURCE_REMOVED_NOTIFICATION));
            assertTrue(notifs.contains(ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION));
            for (String notif : notifs) {
                assertEquals(notif, result.require(NOTIFICATIONS).require(notif).require(NOTIFICATION_TYPE).asString());
            }
        }

        checkHostChildSubModelDescription(result.get(CHILDREN, "serverchild", MODEL_DESCRIPTION, "*"), operations, notifications);
    }

    private void checkHostChildSubModelDescription(ModelNode result, boolean operations, boolean notifications) {
        assertEquals(5, result.keys().size());
        assertEquals(1, result.get(ATTRIBUTES).keys().size());
        assertEquals(ModelType.STRING, result.get(ATTRIBUTES, "name", TYPE).asType());
        assertEquals("name", result.get(ATTRIBUTES, "name", DESCRIPTION).asString());
        assertFalse(result.get(ATTRIBUTES, "name", NILLABLE).asBoolean());
        assertEquals(1, result.get(ATTRIBUTES, "name", MIN_LENGTH).asInt());

        assertEquals(1, result.get(CHILDREN).keys().size());
        assertEquals("child", result.get(CHILDREN, "child", DESCRIPTION).asString());
        //we don't fully support this atm
        //assertEquals(1, result.get(CHILDREN, "child", MIN_OCCURS).asInt());
        assertEquals(1, result.get(CHILDREN, "child", MODEL_DESCRIPTION).keys().size());

        if (operations) {
            Set<String> ops = result.require(OPERATIONS).keys();
            assertTrue(ops.contains(READ_ATTRIBUTE_OPERATION));
            assertTrue(ops.contains(READ_CHILDREN_NAMES_OPERATION));
            assertTrue(ops.contains(READ_CHILDREN_TYPES_OPERATION));
            assertTrue(ops.contains(READ_OPERATION_DESCRIPTION_OPERATION));
            assertTrue(ops.contains(READ_OPERATION_NAMES_OPERATION));
            assertTrue(ops.contains(READ_RESOURCE_DESCRIPTION_OPERATION));
            assertTrue(ops.contains(READ_RESOURCE_OPERATION));
            assertTrue(ops.contains(WRITE_ATTRIBUTE_OPERATION));
            for (String op : ops) {
                assertEquals(op, result.require(OPERATIONS).require(op).require(OPERATION_NAME).asString());
            }
        }

        if (notifications) {
            Set<String> notifs = result.require(NOTIFICATIONS).keys();
            for (String notif : notifs) {
                assertEquals(notif, result.require(NOTIFICATIONS).require(notif).require(NOTIFICATION_TYPE).asString());
            }
            assertTrue(notifs.contains(RESOURCE_ADDED_NOTIFICATION));
            assertTrue(notifs.contains(RESOURCE_REMOVED_NOTIFICATION));
            assertTrue(notifs.contains(ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION));
        }

        checkHostChildChildSubModelDescription(result.get(CHILDREN, "child", MODEL_DESCRIPTION, "*"), operations, notifications);
    }

    private void checkHostChildChildSubModelDescription(ModelNode result, boolean operations, boolean notifications) {
        assertEquals(5, result.keys().size());
        assertEquals(3, result.get(ATTRIBUTES).keys().size());
        assertEquals(ModelType.STRING, result.get(ATTRIBUTES, "name", TYPE).asType());
        assertEquals("name", result.get(ATTRIBUTES, "name", DESCRIPTION).asString());
        assertFalse(result.get(ATTRIBUTES, "name", NILLABLE).asBoolean());
        assertEquals(1, result.get(ATTRIBUTES, "name", MIN_LENGTH).asInt());
        assertEquals(ModelType.STRING, result.get(ATTRIBUTES, "value", TYPE).asType());
        assertEquals("value", result.get(ATTRIBUTES, "value", DESCRIPTION).asString());
        assertFalse(result.get(ATTRIBUTES, "value", NILLABLE).asBoolean());
        assertEquals(1, result.get(ATTRIBUTES, "value", MIN_LENGTH).asInt());

        if (!operations) {
            assertFalse(result.hasDefined(OPERATIONS));
        } else {
            Set<String> ops = result.require(OPERATIONS).keys();
            assertTrue(ops.contains("test-op"));
            for (String op : ops) {
                assertEquals(op, result.require(OPERATIONS).require(op).require(OPERATION_NAME).asString());
            }
        }

        if (!notifications) {
            assertFalse(result.hasDefined(NOTIFICATIONS));
        } else {
            Set<String> notifs = result.require(NOTIFICATIONS).keys();
            assertTrue(notifs.contains(RESOURCE_ADDED_NOTIFICATION));
            assertTrue(notifs.contains(RESOURCE_REMOVED_NOTIFICATION));
            assertTrue(notifs.contains(ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION));
            for (String notif : notifs) {
                assertEquals(notif, result.require(NOTIFICATIONS).require(notif).require(NOTIFICATION_TYPE).asString());
            }
        }
    }

    private void checkRootNode(ModelNode result) {
        assertEquals(2, result.keys().size());
        assertEquals(1, result.get(SERVER).keys().size());
        checkHostNode(result.get(SERVER, "serverA"));
        assertEquals(1, result.get("profile").keys().size());
        assertEquals("Profile A", result.get("profile", "profileA", NAME).asString());
    }

    private void checkHostNode(ModelNode result) {
        assertEquals(1, result.keys().size());
        assertEquals("serverA", result.require("serverchild").require("svrA").require("name").asString());
        assertEquals("childName", result.get("serverchild", "svrA", "child", "childA", "name").asString());
        assertEquals("childValue", result.get("serverchild", "svrA", "child", "childA", "value").asString());
    }

    private Operation createOperation(String operationName, String... address) {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(operationName);
        if (address.length > 0) {
            for (String addr : address) {
                operation.get(ADDRESS).add(addr);
            }
        } else {
            operation.get(ADDRESS).setEmptyList();
        }

        return new OperationBuilder(operation).build();
    }

    protected abstract ProxyController createProxyController(ModelController proxiedController, PathAddress proxyNodeAddress);

    public class MainModelControllerService extends TestModelControllerService {

        final Supplier<ModelController> proxy;

        MainModelControllerService(final Supplier<ModelController> proxy, ControlledProcessState processState) {
            super(ProcessType.EMBEDDED_SERVER, new NullConfigurationPersister(), processState,
                    ResourceBuilder.Factory.create(PathElement.pathElement("root"), NonResolvingResourceDescriptionResolver.INSTANCE).build());
            this.proxy = proxy;
        }

        protected void initModel(ManagementModel managementModel, Resource modelControllerResource) {
            ManagementResourceRegistration rootRegistration = managementModel.getRootResourceRegistration();
            GlobalOperationHandlers.registerGlobalOperations(rootRegistration, processType);
            GlobalNotifications.registerGlobalNotifications(rootRegistration, processType);
            rootRegistration.registerOperationHandler(ValidateOperationHandler.DEFINITION, ValidateOperationHandler.INSTANCE);

            rootRegistration.registerOperationHandler(TestUtils.SETUP_OPERATION_DEF, new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    ModelNode mainModel = new ModelNode();
                    mainModel.get(SERVER, "serverA");  //Create an empty node to be got from the proxied model
                    mainModel.get("profile", "profileA").get(NAME).set("Profile A");

                    AbstractControllerTestBase.createModel(context, mainModel);
                }
            });

            rootRegistration.registerSubModel(ResourceBuilder.Factory.create(PathElement.pathElement("profile", "*"), NonResolvingResourceDescriptionResolver.INSTANCE)
                    .addReadOnlyAttribute(TestUtils.createNillableAttribute("name",ModelType.STRING))
                    .build());

            PathElement serverAElement = PathElement.pathElement(SERVER, "serverA");
            rootRegistration.registerProxyController(serverAElement, createProxyController(proxy.get(), PathAddress.pathAddress(serverAElement)));
        }

    }

    public class ProxyModelControllerService extends TestModelControllerService {

        private volatile ModelControllerClientFactory clientFactory;

        ProxyModelControllerService(final ControlledProcessState processState) {
            super(ProcessType.EMBEDDED_SERVER, new NullConfigurationPersister(), processState,
                    ResourceBuilder.Factory.create(PathElement.pathElement("root"), NonResolvingResourceDescriptionResolver.INSTANCE).build());
        }

        @Override
        protected boolean isExposingClientServicesAllowed() {
            return false; // don't install or it will conflict with the one from MainModelControllerService
        }

        protected void initModel(ManagementModel managementModel, Resource modelControllerResource) {
            ManagementResourceRegistration rootRegistration = managementModel.getRootResourceRegistration();
            GlobalOperationHandlers.registerGlobalOperations(rootRegistration, processType);
            GlobalNotifications.registerGlobalNotifications(rootRegistration, processType);
            rootRegistration.registerOperationHandler(ValidateOperationHandler.DEFINITION, ValidateOperationHandler.INSTANCE);
            rootRegistration.registerOperationHandler(createOperationDefinition("Test"),
                    new OperationStepHandler() {
                        @Override
                        public void execute(OperationContext context, ModelNode operation) {
                            // no-op
                        }
                    },
                    true
            );


            rootRegistration.registerOperationHandler(TestUtils.SETUP_OPERATION_DEF, new OperationStepHandler() {
                        @Override
                        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                            ModelNode proxyModel = new ModelNode();
                            proxyModel.get("serverchild", "svrA", "name").set("serverA");
                            proxyModel.get("serverchild", "svrA", "child", "childA", "name").set("childName");
                            proxyModel.get("serverchild", "svrA", "child", "childA", "value").set("childValue");

                            AbstractControllerTestBase.createModel(context, proxyModel);
                        }
                    }
            );

            ManagementResourceRegistration serverReg = rootRegistration.registerSubModel(
                    new SimpleResourceDefinition(PathElement.pathElement("serverchild", "*"),NonResolvingResourceDescriptionResolver.INSTANCE));
            serverReg.registerReadOnlyAttribute(createAttribute("name", ModelType.STRING), null);
            /*ManagementResourceRegistration serverReg = rootRegistration.registerSubModel(PathElement.pathElement("serverchild", "*"), new DescriptionProvider() {

                @Override
                public ModelNode getModelDescription(Locale locale) {
                    ModelNode node = new ModelNode();
                    node.get(DESCRIPTION).set("A server child");
                    node.get(ATTRIBUTES, NAME, TYPE).set(ModelType.STRING);
                    node.get(ATTRIBUTES, NAME, DESCRIPTION).set("The name of the server child");
                    node.get(ATTRIBUTES, NAME, REQUIRED).set(true);
                    node.get(ATTRIBUTES, NAME, MIN_LENGTH).set(1);
                    node.get(CHILDREN, "child", DESCRIPTION).set("The children of the server child");
                    node.get(CHILDREN, "child", MIN_OCCURS).set(1);
                    node.get(CHILDREN, "child", MODEL_DESCRIPTION);
                    return node;
                }
            });*/


            ManagementResourceRegistration serverChildReg = serverReg.registerSubModel(
                    new SimpleResourceDefinition(PathElement.pathElement("child", "*"),NonResolvingResourceDescriptionResolver.INSTANCE));

            /*ManagementResourceRegistration serverChildReg = serverReg.registerSubModel(PathElement.pathElement("child", "*"), new DescriptionProvider() {

                @Override
                public ModelNode getModelDescription(Locale locale) {
                    ModelNode node = new ModelNode();
                    node.get(DESCRIPTION).set("A named set of children");
                    node.get(ATTRIBUTES, NAME, TYPE).set(ModelType.STRING);
                    node.get(ATTRIBUTES, NAME, DESCRIPTION).set("The name of the child");
                    node.get(ATTRIBUTES, NAME, REQUIRED).set(true);
                    node.get(ATTRIBUTES, NAME, MIN_LENGTH).set(1);
                    node.get(ATTRIBUTES, VALUE, TYPE).set(ModelType.STRING);
                    node.get(ATTRIBUTES, VALUE, DESCRIPTION).set("The value of the child");
                    node.get(ATTRIBUTES, VALUE, REQUIRED).set(true);
                    node.get(ATTRIBUTES, VALUE, MIN_LENGTH).set(1);
                    return node;
                }
            });*/
            serverChildReg.registerReadOnlyAttribute(createAttribute("name", ModelType.STRING), null);
            final AttributeDefinition value = createAttribute("value", ModelType.STRING);
            serverChildReg.registerReadWriteAttribute(value, null, new ModelOnlyWriteAttributeHandler(value));
            serverChildReg.registerMetric(createMetric("metric", ModelType.STRING), GlobalOperationsTestCase.TestMetricHandler.INSTANCE);

            serverChildReg.registerOperationHandler(createOperationDefinition("test-op"),
                    new OperationStepHandler() {
                        @Override
                        public void execute(OperationContext context, ModelNode operation) {

                        }
                    }
            );
        }
    }

}
