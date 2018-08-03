/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.tomitribe.chatterbox.slack.adapter.out;

import org.tomitribe.chatterbox.slack.api.SlackConnection;

public class SlackConnectionImpl implements SlackConnection {
    private SlackManagedConnection mc;

    private SlackManagedConnectionFactory mcf;

    public SlackConnectionImpl(final SlackManagedConnection mc, final SlackManagedConnectionFactory mcf) {
        this.mc = mc;
        this.mcf = mcf;
    }

    public void sendMessage(final String channel, final String message) {
        mc.sendMessage(channel, message);
    }

    public void close() {
        mc.closeHandle(this);
    }
}
