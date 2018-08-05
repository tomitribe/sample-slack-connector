/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tomitribe.chatterbox.slack.adapter;

import allbegray.slack.SlackClientFactory;
import allbegray.slack.rtm.EventListener;
import allbegray.slack.rtm.SlackRealTimeMessagingClient;
import allbegray.slack.type.Authentication;
import allbegray.slack.type.Presence;
import allbegray.slack.webapi.SlackWebApiClient;
import allbegray.slack.webapi.method.chats.ChatPostMessageMethod;
import com.fasterxml.jackson.databind.JsonNode;
import org.tomitribe.chatterbox.slack.api.InboundListener;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ConfigProperty;
import javax.resource.spi.Connector;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@Connector(description = "Sample Resource Adapter", displayName = "Sample Resource Adapter", eisType = "Sample Resource Adapter", version = "1.0")
public class SlackResourceAdapter implements ResourceAdapter, EventListener {
    final Map<SlackActivationSpec, MessageEndpoint> targets = new ConcurrentHashMap<>();

    private final Logger log = Logger.getLogger(SlackResourceAdapter.class.getName());

    @ConfigProperty
    private String token;

    private SlackRealTimeMessagingClient slackRealTimeMessagingClient;
    private SlackWebApiClient webApiClient;
    private String user;
    private Method messageReceivedMethod;

    public SlackResourceAdapter() {
        try {
            messageReceivedMethod = InboundListener.class.getMethod("messageReceived", String.class, String.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Unable to lookup messageReceived method on InboundListener");
        }
    }

    public void start(final BootstrapContext bootstrapContext) throws ResourceAdapterInternalException {
        webApiClient = SlackClientFactory.createWebApiClient(token);
        slackRealTimeMessagingClient = SlackClientFactory.createSlackRealTimeMessagingClient(token);

        final Authentication authentication = webApiClient.auth();
        user = authentication.getUser();

        webApiClient.setPresenceUser(Presence.AUTO);

        slackRealTimeMessagingClient.connect();
    }

    public void stop() {
        webApiClient.setPresenceUser(Presence.AWAY);
    }

    public void endpointActivation(final MessageEndpointFactory messageEndpointFactory, final ActivationSpec activationSpec)
            throws ResourceException {
        final SlackActivationSpec slackActivationSpec = (SlackActivationSpec) activationSpec;

        final MessageEndpoint messageEndpoint = messageEndpointFactory.createEndpoint(null);
        targets.put(slackActivationSpec, messageEndpoint);
    }

    public void endpointDeactivation(final MessageEndpointFactory messageEndpointFactory, final ActivationSpec activationSpec) {
        final SlackActivationSpec telnetActivationSpec = (SlackActivationSpec) activationSpec;

        final MessageEndpoint endpoint = targets.get(telnetActivationSpec);
        if (endpoint == null) {
            throw new IllegalStateException("No Endpoint to undeploy for ActivationSpec " + activationSpec);
        }

        endpoint.release();
    }

    public XAResource[] getXAResources(final ActivationSpec[] activationSpecs) throws ResourceException {
        return new XAResource[0];
    }

    public void sendMessage(final String channel, final String message) {
        ChatPostMessageMethod postMessage = new ChatPostMessageMethod(channel, message);
        postMessage.setUsername(user);
        webApiClient.postMessage(postMessage);
    }

    @Override
    public void onMessage(final JsonNode jsonNode) {
        final String text = jsonNode.get("text").textValue();
        final String channel = jsonNode.get("channel").textValue();

        for (final MessageEndpoint endpoint : targets.values()) {
            boolean beforeDelivery = false;

            try {
                endpoint.beforeDelivery(messageReceivedMethod);
                beforeDelivery = true;

                ((InboundListener) endpoint).messageReceived(channel, text);
            } catch (Throwable t) {
                log.severe("Unable to deliver message to endpoint. Cause: " + t.getMessage());
            } finally {
                if (beforeDelivery) {
                    try {
                        endpoint.afterDelivery();
                    } catch (Throwable t) {
                        log.severe("Unable to call afterDelivery(). Cause: " + t.getMessage());
                    }
                }
            }
        }
    }

    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
    }
}
