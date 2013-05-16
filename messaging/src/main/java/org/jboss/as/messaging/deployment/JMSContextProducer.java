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

package org.jboss.as.messaging.deployment;

import static javax.jms.JMSContext.AUTO_ACKNOWLEDGE;
import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

import java.io.Serializable;

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.jms.BytesMessage;
import javax.jms.ConnectionFactory;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.IllegalStateRuntimeException;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSPasswordCredential;
import javax.jms.JMSProducer;
import javax.jms.JMSSessionMode;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.as.messaging.MessagingMessages;

/**
 * Producer factory for JMSContext resources.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class JMSContextProducer {

    public static final String DEFAULT_JMS_CONNECTION_FACTORY_LOCATION = "java:/comp/DefaultJMSConnectionFactory";

    @Produces
    public JMSContext getJMSContext(InjectionPoint injectionPoint) throws NamingException {
        String connectionFactoryLookup = DEFAULT_JMS_CONNECTION_FACTORY_LOCATION;
        String userName = null;
        String password = null;
        int ackMode = AUTO_ACKNOWLEDGE;

        if (injectionPoint != null) {
            // Check for @JMSConnectionFactory annotation
            if (injectionPoint.getAnnotated().isAnnotationPresent(JMSConnectionFactory.class)) {
                JMSConnectionFactory cf = injectionPoint.getAnnotated().getAnnotation(JMSConnectionFactory.class);
                connectionFactoryLookup = cf.value();
            }

            // Check for JMSConnectionFactory annotation
            if (injectionPoint.getAnnotated().isAnnotationPresent(JMSPasswordCredential.class)) {
                JMSPasswordCredential credential = injectionPoint.getAnnotated().getAnnotation(JMSPasswordCredential.class);
                userName = credential.userName();
                password = credential.password();
            }

            // Check for JMSConnectionFactory annotation
            if (injectionPoint.getAnnotated().isAnnotationPresent(JMSSessionMode.class)) {
                JMSSessionMode sessionMode = injectionPoint.getAnnotated().getAnnotation(JMSSessionMode.class);
                ackMode = sessionMode.value();
            }
        }

        return create(connectionFactoryLookup, userName, password, ackMode);
    }

    public void closeJMSContext(@Disposes JMSContext context) {
        System.out.println("###### JMSContextProducer.closeJMSContext");
        System.out.println("context = [" + context + "]");
        context.close();
    }

    // FIXME wrap the JMSContext returned by HornetQ to handle restrictions on injected JMS Context (JMS 2.0 spec, §12.4.5)
    private JMSContext create(String cfName, String user, String password, int ackMode) throws NamingException {
        Context ctx = null;
        try {
            ctx = new InitialContext();
            ConnectionFactory cf = (ConnectionFactory) ctx.lookup(cfName);
            // this call will fail until HornetQ implements JMS 2.0
            JMSContext context = cf.createContext(user, password, ackMode);
            return new JMSContextWrapper(context);
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                }
            }
        }
    }

    /**
     * Wrapper to restrict use of methods for injected JMSContext (JMS 2.0 spec, §12.4.5)
     */
    private class JMSContextWrapper implements JMSContext {


        private final JMSContext delegate;

        public JMSContextWrapper(JMSContext context) {
            this.delegate = context;
        }

        @Override
        public JMSContext createContext(int sessionMode) {
            return delegate.createContext(sessionMode);
        }

        @Override
        public JMSProducer createProducer() {
            return delegate.createProducer();
        }

        @Override
        public String getClientID() {
            return delegate.getClientID();
        }

        @Override
        public void setClientID(String clientID) {
            throw MESSAGES.callNotPermittedOnInjectedJMSContext();
        }

        @Override
        public ConnectionMetaData getMetaData() {
            return delegate.getMetaData();
        }

        @Override
        public ExceptionListener getExceptionListener() {
            return delegate.getExceptionListener();
        }

        @Override
        public void setExceptionListener(ExceptionListener listener) {
            throw MESSAGES.callNotPermittedOnInjectedJMSContext();
        }

        @Override
        public void start() {
            throw MESSAGES.callNotPermittedOnInjectedJMSContext();
        }

        @Override

        public void stop() {
            throw MESSAGES.callNotPermittedOnInjectedJMSContext();
        }

        @Override
        public void setAutoStart(boolean autoStart) {
            throw MESSAGES.callNotPermittedOnInjectedJMSContext();
        }

        @Override
        public boolean getAutoStart() {
            return delegate.getAutoStart();
        }

        @Override
        public void close() {
            // FIXME should be able to close it when disposing the injected resource
            throw MESSAGES.callNotPermittedOnInjectedJMSContext();
        }

        @Override
        public BytesMessage createBytesMessage() {
            return delegate.createBytesMessage();
        }

        @Override
        public MapMessage createMapMessage() {
            return delegate.createMapMessage();
        }

        @Override
        public Message createMessage() {
            return delegate.createMessage();
        }

        @Override
        public ObjectMessage createObjectMessage() {
            return delegate.createObjectMessage();
        }

        @Override
        public ObjectMessage createObjectMessage(Serializable object) {
            return delegate.createObjectMessage(object);
        }

        @Override
        public StreamMessage createStreamMessage() {
            return delegate.createStreamMessage();
        }

        @Override
        public TextMessage createTextMessage() {
            return delegate.createTextMessage();
        }

        @Override
        public TextMessage createTextMessage(String text) {
            return delegate.createTextMessage(text);
        }

        @Override
        public boolean getTransacted() {
            return delegate.getTransacted();
        }

        @Override
        public int getSessionMode() {
            return delegate.getSessionMode();
        }

        @Override
        public void commit() {
            throw MESSAGES.callNotPermittedOnInjectedJMSContext();
        }

        @Override
        public void rollback() {
            throw MESSAGES.callNotPermittedOnInjectedJMSContext();
        }

        @Override
        public void recover() {
            throw MESSAGES.callNotPermittedOnInjectedJMSContext();
        }

        @Override
        public JMSConsumer createConsumer(Destination destination) {
            return delegate.createConsumer(destination);
        }

        @Override
        public JMSConsumer createConsumer(Destination destination, String messageSelector) {
            return delegate.createConsumer(destination, messageSelector);
        }

        @Override
        public JMSConsumer createConsumer(Destination destination, String messageSelector, boolean noLocal) {
            return delegate.createConsumer(destination, messageSelector, noLocal);
        }

        @Override
        public Queue createQueue(String queueName) {
            return delegate.createQueue(queueName);
        }

        @Override
        public Topic createTopic(String topicName) {
            return delegate.createTopic(topicName);
        }

        @Override
        public JMSConsumer createDurableConsumer(Topic topic, String name) {
            return delegate.createDurableConsumer(topic, name);
        }

        @Override
        public JMSConsumer createDurableConsumer(Topic topic, String name, String messageSelector, boolean noLocal) {
            return delegate.createDurableConsumer(topic, name, messageSelector, noLocal);
        }

        @Override
        public JMSConsumer createSharedDurableConsumer(Topic topic, String name) {
            return delegate.createSharedDurableConsumer(topic, name);
        }

        @Override
        public JMSConsumer createSharedDurableConsumer(Topic topic, String name, String messageSelector) {
            return delegate.createSharedDurableConsumer(topic, name, messageSelector);
        }

        @Override
        public JMSConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName) {
            return delegate.createSharedConsumer(topic, sharedSubscriptionName);
        }

        @Override
        public JMSConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName, String messageSelector) {
            return delegate.createSharedConsumer(topic, sharedSubscriptionName, messageSelector);
        }

        @Override
        public QueueBrowser createBrowser(Queue queue) {
            return delegate.createBrowser(queue);
        }

        @Override
        public QueueBrowser createBrowser(Queue queue, String messageSelector) {
            return delegate.createBrowser(queue, messageSelector);
        }

        @Override
        public TemporaryQueue createTemporaryQueue() {
            return delegate.createTemporaryQueue();
        }

        @Override
        public TemporaryTopic createTemporaryTopic() {
            return delegate.createTemporaryTopic();
        }

        @Override
        public void unsubscribe(String name) {
            delegate.unsubscribe(name);
        }

        @Override
        public void acknowledge() {
            throw MESSAGES.callNotPermittedOnInjectedJMSContext();
        }
    }
}
