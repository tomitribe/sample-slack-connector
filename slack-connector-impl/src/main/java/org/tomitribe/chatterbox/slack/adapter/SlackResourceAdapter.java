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
import allbegray.slack.rtm.SlackRealTimeMessagingClient;
import allbegray.slack.type.Authentication;
import allbegray.slack.type.Presence;
import allbegray.slack.webapi.SlackWebApiClient;
import allbegray.slack.webapi.method.chats.ChatPostMessageMethod;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ConfigProperty;
import javax.resource.spi.Connector;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

@Connector(description = "Sample Resource Adapter", displayName = "Sample Resource Adapter", eisType = "Sample Resource Adapter", version = "1.0")
public class SlackResourceAdapter implements ResourceAdapter {

    @ConfigProperty
    private String token;

    private SlackRealTimeMessagingClient slackRealTimeMessagingClient;
    private SlackWebApiClient webApiClient;
    private String user;

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
    }

    public void endpointDeactivation(final MessageEndpointFactory messageEndpointFactory, final ActivationSpec activationSpec) {
    }

    public XAResource[] getXAResources(final ActivationSpec[] activationSpecs) throws ResourceException {
        return new XAResource[0];
    }

    public void sendMessage(final String channel, final String message) {
        ChatPostMessageMethod postMessage = new ChatPostMessageMethod(channel, message);
        postMessage.setUsername(user);
        webApiClient.postMessage(postMessage);
    }

    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
    }
}
