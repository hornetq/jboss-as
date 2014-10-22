/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.operations.common.Util.getEmptyOperation;
import static org.jboss.as.controller.parsing.ParseUtils.readStringAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.messaging.CommonAttributes.HA_POLICY;
import static org.jboss.as.messaging.CommonAttributes.NONE;
import static org.jboss.as.messaging.CommonAttributes.REPLICATION_MASTER;
import static org.jboss.as.messaging.CommonAttributes.REPLICATION_SLAVE;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.messaging.ha.HAAttributes;
import org.jboss.as.messaging.ha.ScaleDownAttributes;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Messaging subsystem 3.0 XML parser.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a>
 *
 */
public class Messaging30SubsystemParser extends Messaging20SubsystemParser {

    private static final Messaging30SubsystemParser INSTANCE = new Messaging30SubsystemParser();

    protected Messaging30SubsystemParser() {
    }

    public static MessagingSubsystemParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected void handleUnknownConfigurationAttribute(XMLExtendedStreamReader reader, Element element, ModelNode operation) throws XMLStreamException {
        switch (element) {
            case OVERRIDE_IN_VM_SECURITY:
                handleElementText(reader, element, operation);
                break;
            default: {
                super.handleUnknownConfigurationAttribute(reader, element, operation);
            }
        }
    }

    @Override
    protected void handleUnknownAddressSetting(XMLExtendedStreamReader reader, Element element, ModelNode addressSettingsAdd) throws XMLStreamException {
        switch (element) {
            case MAX_REDELIVERY_DELAY:
            case REDELIVERY_MULTIPLIER:
                handleElementText(reader, element, addressSettingsAdd);
                break;
            default:
                super.handleUnknownAddressSetting(reader, element, addressSettingsAdd);
        }
    }

    private static final EnumSet<Element> HA_POLICY_ATTRIBUTES = EnumSet.noneOf(Element.class);

    static {
        for (AttributeDefinition attr : HAPolicyDefinition.ATTRIBUTES) {
            HA_POLICY_ATTRIBUTES.add(Element.forName(attr.getXmlName()));
        }
    }

    @Override
    protected void processHaPolicy(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> list) throws XMLStreamException {

        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            String localName = reader.getLocalName();
            final Element element = Element.forName(localName);

            switch (element) {
                case NONE:
                    procesHaPolicyNone(reader, address, list);
                    break;
                case REPLICATION:
                    procesHaPolicyReplication(reader, address, list);
                    break;
                default:
                throw ParseUtils.unexpectedElement(reader);
            }
        }
    }

    private void procesHaPolicyNone(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> list) throws XMLStreamException {
        ModelNode haPolicyAdd = getEmptyOperation(ADD, address.clone().add(HA_POLICY, NONE));

        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            String localName = reader.getLocalName();
            final Element element = Element.forName(localName);

            switch (element) {
                case SCALE_DOWN:
                    processScaleDown(reader, haPolicyAdd);
                    break;
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }

        list.add(haPolicyAdd);

    }

    private void procesHaPolicyReplication(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> list) throws XMLStreamException {
        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            String localName = reader.getLocalName();
            final Element element = Element.forName(localName);

            switch (element) {
                case MASTER:
                    procesHaPolicyReplicationMaster(reader, address, list);
                    break;
                case SLAVE:
                    procesHaPolicyReplicationSlave(reader, address, list);
                    break;
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }
    }

    private void procesHaPolicyReplicationMaster(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> list) throws XMLStreamException {
        ModelNode operation = getEmptyOperation(ADD, address.clone().add(HA_POLICY, REPLICATION_MASTER));

        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String attrValue = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case GROUP_NAME: {
                    HAAttributes.GROUP_NAME.parseAndSetParameter(attrValue, operation, reader);
                    break;
                }
                case CLUSTER_NAME: {
                    HAAttributes.CLUSTER_NAME.parseAndSetParameter(attrValue, operation, reader);
                    break;                }
                case CHECK_FOR_LIVE_SERVER: {
                    HAAttributes.CHECK_FOR_LIVE_SERVER.parseAndSetParameter(attrValue, operation, reader);
                    break;
                } default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        requireNoContent(reader);

        list.add(operation);
    }

    private void procesHaPolicyReplicationSlave(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> list) throws XMLStreamException {
        ModelNode operation = getEmptyOperation(ADD, address.clone().add(HA_POLICY, REPLICATION_SLAVE));

        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String attrValue = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case GROUP_NAME: {
                    HAAttributes.GROUP_NAME.parseAndSetParameter(attrValue, operation, reader);
                    break;
                }
                case CLUSTER_NAME: {
                    HAAttributes.CLUSTER_NAME.parseAndSetParameter(attrValue, operation, reader);
                    break;
                } default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            String localName = reader.getLocalName();
            final Element element = Element.forName(localName);

            switch (element) {
                case SCALE_DOWN:
                    processScaleDown(reader, operation);
                    break;
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }

        list.add(operation);
    }

    private void processScaleDown(XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String attrValue = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ENABLED: {
                    ScaleDownAttributes.SCALE_DOWN.parseAndSetParameter(attrValue, operation, reader);
                    break;
                }
                case GROUP_NAME: {
                    ScaleDownAttributes.SCALE_DOWN_GROUP_NAME.parseAndSetParameter(attrValue, operation, reader);
                    break;
                }
                case CLUSTER_NAME: {
                    ScaleDownAttributes.SCALE_DOWN_CLUSTER_NAME.parseAndSetParameter(attrValue, operation, reader);
                    break;
                } default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        while(reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            Set<Element> seen = EnumSet.noneOf(Element.class);

            String localName = reader.getLocalName();
            final Element element = Element.forName(localName);
            if (!seen.add(element)) {
                throw ParseUtils.duplicateNamedElement(reader, element.getLocalName());
            }

            switch (element) {
                case DISCOVERY_GROUP_REF: {
                    checkOtherElementIsNotAlreadyDefined(reader, seen, Element.DISCOVERY_GROUP_REF, Element.CONNECTORS);
                    final String attrValue = readStringAttributeElement(reader, ScaleDownAttributes.SCALE_DOWN_DISCOVERY_GROUP_NAME.getXmlName());
                    ScaleDownAttributes.SCALE_DOWN_DISCOVERY_GROUP_NAME.parseAndSetParameter(attrValue, operation, reader);
                    break;
                } case CONNECTORS: {
                    checkOtherElementIsNotAlreadyDefined(reader, seen, Element.CONNECTORS, Element.DISCOVERY_GROUP_REF);
                    operation.get(ScaleDownAttributes.SCALE_DOWN_CONNECTORS.getName()).set(processJmsConnectors(reader));
                    break;
                }
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }
    }
}
