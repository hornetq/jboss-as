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

import static org.jboss.as.messaging.CommonAttributes.HA_POLICY;
import static org.jboss.as.messaging.CommonAttributes.NONE;
import static org.jboss.as.messaging.ha.ScaleDownAttributes.CLUSTER_NAME;
import static org.jboss.as.messaging.ha.ScaleDownAttributes.DISCOVERY_GROUP_NAME;
import static org.jboss.as.messaging.ha.ScaleDownAttributes.GROUP_NAME;
import static org.jboss.as.messaging.ha.ScaleDownAttributes.SCALE_DOWN;

import java.util.ArrayList;
import java.util.List;

import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.ScaleDownConfiguration;
import org.hornetq.core.config.ha.LiveOnlyPolicyConfiguration;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class HAPolicyConfiguration {

    public static void addHAPolicyConfiguration(OperationContext context, Configuration configuration, ModelNode model) throws OperationFailedException {

        if (!model.hasDefined(HA_POLICY)) {
            return;
        }
        Property prop = model.get(HA_POLICY).asProperty();
        ModelNode haPolicy = prop.getValue();

        String type = prop.getName();
        switch (type) {
            case NONE:
                ScaleDownConfiguration scaleDownConfiguration = addScaleDownConfiguration(context, haPolicy);

                LiveOnlyPolicyConfiguration haPolicyConfiguration = new LiveOnlyPolicyConfiguration(scaleDownConfiguration);
                configuration.setHAPolicyConfiguration(haPolicyConfiguration);
        }
    }

    private static ScaleDownConfiguration addScaleDownConfiguration(OperationContext context, ModelNode model) throws OperationFailedException {
        ScaleDownConfiguration scaleDownConfiguration = new ScaleDownConfiguration();

        scaleDownConfiguration.setScaleDown(SCALE_DOWN.resolveModelAttribute(context, model).asBoolean());

        ModelNode clusterName = CLUSTER_NAME.resolveModelAttribute(context, model);
        if (clusterName.isDefined()) {
            scaleDownConfiguration.setClusterName(clusterName.asString());
        }
        ModelNode groupName = GROUP_NAME.resolveModelAttribute(context, model);
        if (groupName.isDefined()) {
            scaleDownConfiguration.setGroupName(groupName.asString());
        }
        ModelNode discoveryGroupName = DISCOVERY_GROUP_NAME.resolveModelAttribute(context, model);
        if (discoveryGroupName.isDefined()) {
            scaleDownConfiguration.setDiscoveryGroup(discoveryGroupName.asString());
        }
        ModelNode connectors = ScaleDownAttributes.CONNECTOR.resolveModelAttribute(context, model);
        if (connectors.isDefined()) {
            List<String> connectorNames = new ArrayList<String>(connectors.keys());
            scaleDownConfiguration.setConnectors(connectorNames);
        }
        return scaleDownConfiguration;
    }
}
