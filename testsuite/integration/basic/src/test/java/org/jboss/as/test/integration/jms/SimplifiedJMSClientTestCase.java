/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jms;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.jboss.as.test.shared.TimeoutUtil.adjust;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.UUID;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSPasswordCredential;
import javax.jms.JMSProducer;
import javax.jms.JMSSessionMode;
import javax.jms.Queue;
import javax.jms.TemporaryQueue;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.jms.auxiliary.TransactedMessageProducer;
import org.jboss.as.test.jms.auxiliary.CreateQueueSetupTask;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
@RunWith(Arquillian.class)
@ServerSetup(CreateQueueSetupTask.class)
public class SimplifiedJMSClientTestCase {

    public static final String QUEUE_NAME = "/queue/myAwesomeQueue";

    @Inject
    @JMSConnectionFactory("/ConnectionFactory")
    @JMSPasswordCredential(userName="guest",password="guest")
    @JMSSessionMode(JMSContext.AUTO_ACKNOWLEDGE)
    private JMSContext context;

    @Resource(mappedName = "/ConnectionFactory")
    private ConnectionFactory factory;

    @Resource(mappedName = QUEUE_NAME)
    private Queue queue;

    @EJB
    private TransactedMessageProducer producerBean;

    @Deployment
    public static JavaArchive createTestArchive() {
        return ShrinkWrap.create(JavaArchive.class, "SimplifiedJMSClientTestCase.jar")
                .addPackage(JMSOperations.class.getPackage())
                .addClass(CreateQueueSetupTask.class)
                .addClass(TimeoutUtil.class)
                .addPackage(TransactedMessageProducer.class.getPackage())
                .addAsManifestResource(EmptyAsset.INSTANCE,
                        "beans.xml")
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client,org.jboss.dmr,org.jboss.as.cli\n"),
                        "MANIFEST.MF");
    }

    @Test
    public void testSendAndReceiveWithInjectedContext() {
        sendAndReceiveWithContext(context);
    }

    @Test
    public void testSendAndReceiveWithCreatedContext() {
        JMSContext ctx = factory.createContext();

        sendAndReceiveWithContext(ctx);

        ctx.close();
    }

    private void sendAndReceiveWithContext(JMSContext ctx) {
        String text = UUID.randomUUID().toString();

        TemporaryQueue tempQueue = ctx.createTemporaryQueue();

        ctx.createProducer()
                .send(tempQueue, text);

        String t = ctx.createConsumer(tempQueue)
                .receiveBody(String.class, adjust(2000));
        assertThat(t, is(text));
    }

    @Test
    public void testSendWith_REQUIRED_transaction() {
        sendWith_REQUIRED_transaction(false);
    }

    @Test
    public void testSendWith_REQUIRED_transactionAndRollback() {
        sendWith_REQUIRED_transaction(true);
    }

    private void sendWith_REQUIRED_transaction(boolean rollback) {
        String text = UUID.randomUUID().toString();

        TemporaryQueue tempQueue = context.createTemporaryQueue();

        producerBean.sendToDestination(tempQueue, text, rollback);

        String t = context.createConsumer(tempQueue)
                .receiveBody(String.class, adjust(500));
        if (rollback) {
            assertThat(t, is(nullValue()));
        } else {
            assertThat(t, is(text));
        }
    }

    @Test
    public void testSendAndReceiveFromMDB() {
        sendAndReceiveFromMDB(false);
    }

    @Test
    public void testSendAndReceiveFromMDBWithRollback() {
        sendAndReceiveFromMDB(true);
    }

    private void sendAndReceiveFromMDB(boolean rollback) {
        String text = UUID.randomUUID().toString();

        TemporaryQueue replyTo = context.createTemporaryQueue();

        context.createProducer()
                .setJMSReplyTo(replyTo)
                .setProperty("rollback", rollback)
                .send(queue, text);

        String t = context.createConsumer(replyTo)
                .receiveBody(String.class, adjust(2000));

        if (rollback) {
            assertThat(t, is(nullValue()));
        } else {
            assertThat(t, is(text));
        }
    }
}
