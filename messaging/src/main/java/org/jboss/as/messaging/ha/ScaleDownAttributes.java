/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging.ha;

import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.STRING;

import org.hornetq.api.config.HornetQDefaultConfiguration;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ParameterCorrector;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
import org.jboss.as.messaging.AttributeMarshallers;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class ScaleDownAttributes {

    public static SimpleAttributeDefinition SCALE_DOWN = SimpleAttributeDefinitionBuilder.create(CommonAttributes.SCALE_DOWN, BOOLEAN)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode(HornetQDefaultConfiguration.isDefaultScaleDown()))
            .setAllowExpression(true)
            // scale-down attribute is represented with the "enabled" attribute of the "scale-down" XML element
            .setXmlName(CommonAttributes.ENABLED)
            .setRestartAllServices()
            .build();

    public static SimpleAttributeDefinition CLUSTER_NAME = SimpleAttributeDefinitionBuilder.create(CommonAttributes.CLUSTER_NAME, STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static SimpleAttributeDefinition GROUP_NAME = SimpleAttributeDefinitionBuilder.create(CommonAttributes.GROUP_NAME, STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static SimpleAttributeDefinition DISCOVERY_GROUP_NAME =  SimpleAttributeDefinitionBuilder.create(CommonAttributes.DISCOVERY_GROUP_NAME, STRING)
            .setAllowNull(true)
            .setAlternatives(CommonAttributes.CONNECTOR)
            .setAttributeMarshaller(AttributeMarshallers.DISCOVERY_GROUP_MARSHALLER)
            .setRestartAllServices()
            .build();

    public static AttributeDefinition CONNECTOR = new SimpleMapAttributeDefinition.Builder(CommonAttributes.CONNECTOR, true)
            .setAlternatives(DISCOVERY_GROUP_NAME.getName())
            .setAttributeMarshaller(AttributeMarshallers.CONNECTORS_MARSHALLER)
            .setCorrector(new ParameterCorrector() {
                /*
                 * https://issues.jboss.org/browse/WFLY-1796
                 *
                 * For backwards compatibility, the connector attribute must be a map where the key is a
                 * connector name and the value is not taken into account (in previous HornetQ versions, the value
                 * was the backup's server connector).
                 *
                 * This is a source of confusion when creating resources with connector: users expect to pass a
                 * list of connectors and this fails as they must pass a map with undefined values.
                 *
                 * This corrector will replace a list with the map expected to populate the model.
                 */
                @Override
                public ModelNode correct(ModelNode newValue, ModelNode currentValue) {
                    if (newValue.getType() != ModelType.LIST) {
                        return newValue;
                    } else {
                        ModelNode correctValue = new ModelNode();
                        for (ModelNode node : newValue.asList()) {
                            correctValue.get(node.asString());
                        }
                        return correctValue;
                    }
                }
            })
            .setRestartAllServices()
            .build();
}
