/*
 * Copyright (c) 2018 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.couchbase.client.core.io;

import com.couchbase.client.core.CoreContext;

import java.net.SocketAddress;
import java.util.Map;

public class IoContext extends CoreContext {

  private final SocketAddress localSocket;

  private final SocketAddress remoteSocket;

  /**
   * Creates a new IO Context.
   *
   * @param ctx the core context as a parent.
   * @param localSocket the local io socket.
   * @param remoteSocket the remote io socket.
   */
  public IoContext(final CoreContext ctx, final SocketAddress localSocket,
                   final SocketAddress remoteSocket) {
    super(ctx.id(), ctx.env());
    this.localSocket = localSocket;
    this.remoteSocket = remoteSocket;
  }

  @Override
  protected void injectExportableParams(final Map<String, Object> input) {
    super.injectExportableParams(input);
    input.put("local", localSocket());
    input.put("remote", remoteSocket());
  }

  /**
   * Returns the local socket.
   *
   * @return the local socket address.
   */
  public SocketAddress localSocket() {
    return localSocket;
  }

  /**
   * Returns the remote socket.
   *
   * @return the remote socket address.
   */
  public SocketAddress remoteSocket() {
    return remoteSocket;
  }
}