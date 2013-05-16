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

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSPasswordCredential;
import javax.jms.JMSSessionMode;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

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
            return context;
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                }
            }
        }
    }
}
