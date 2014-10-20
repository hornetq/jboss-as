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

import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.ScaleDownConfiguration;
import org.hornetq.core.config.ha.LiveOnlyPolicyConfiguration;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class HAPolicyConfiguration {

    public static void addHAPolicyConfig(OperationContext context, Configuration configuration, ModelNode model) throws OperationFailedException {

        if (!model.hasDefined(CommonAttributes.HA_POLICY)) {
            return;
        }
        Property prop = model.get(CommonAttributes.HA_POLICY).asProperty();

        String type = prop.getName();
        switch (type) {
            case CommonAttributes.NONE:
                ScaleDownConfiguration scaleDownConfiguration = new ScaleDownConfiguration();
                scaleDownConfiguration.setScaleDown(ScaleDownAttributes.SCALE_DOWN.resolveModelAttribute(context, model).asBoolean());
                LiveOnlyPolicyConfiguration haPolicyConfiguration = new LiveOnlyPolicyConfiguration(scaleDownConfiguration);
                configuration.setHAPolicyConfiguration(haPolicyConfiguration);
        }
    }
}
