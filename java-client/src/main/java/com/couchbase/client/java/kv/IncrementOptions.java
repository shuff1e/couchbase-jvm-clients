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

package com.couchbase.client.java.kv;

import com.couchbase.client.core.annotation.Stability;
import com.couchbase.client.core.error.CasMismatchException;
import com.couchbase.client.core.error.InvalidArgumentException;

import java.time.Duration;
import java.util.Optional;

/**
 * Modifies properties of the increment operation.
 */
public class IncrementOptions extends CommonDurabilityOptions<IncrementOptions> {

  /**
   * Stores the delta for the operation.
   */
  private long delta = 1;

  /**
   * If present, holds the initial value.
   */
  private Optional<Long> initial = Optional.empty();

  /**
   * If set, holds the expiration for this operation.
   */
  private Duration expiry = Duration.ZERO;

  /**
   * If set, holds the CAS value for this operation.
   */
  private long cas = 0;

  /**
   * Creates a new {@link IncrementOptions}.
   *
   * @return the created options.
   */
  public static IncrementOptions incrementOptions() {
    return new IncrementOptions();
  }

  private IncrementOptions() { }

  /**
   * The amount of which the document value should be incremented.
   *
   * @param delta the amount to increment.
   * @return this options class for chaining purposes.
   */
  public IncrementOptions delta(long delta) {
    if (delta < 0) {
      throw InvalidArgumentException.fromMessage("The delta cannot be less than 0");
    }
    this.delta = delta;
    return this;
  }

  /**
   * The initial value that should be used if the document has not been created yet.
   *
   * @param initial the initial value to use.
   * @return this options class for chaining purposes.
   */
  public IncrementOptions initial(long initial) {
    this.initial = Optional.of(initial);
    return this;
  }

  /**
   * Set a custom expiration time for the document (by default no expiry is set).
   *
   * @param expiry the custom expiry value of the document.
   * @return this options class for chaining purposes.
   */
  public IncrementOptions expiry(final Duration expiry) {
    this.expiry = expiry;
    return this;
  }

  /**
   * Specifies a CAS value that will be taken into account on the server side for optimistic concurrency.
   * <p>
   * The CAS value is an opaque identifier which is associated with a specific state of the document on the server. The
   * CAS value is received on read operations (or after mutations) and can be used during a subsequent mutation to
   * make sure that the document has not been modified in the meantime.
   * <p>
   * If document on the server has been modified in the meantime the SDK will raise a {@link CasMismatchException}. In
   * this case the caller is expected to re-do the whole "fetch-modify-update" cycle again. Please refer to the
   * SDK documentation for more information on CAS mismatches and subsequent retries.
   *
   * @param cas the opaque CAS identifier to use for this operation.
   * @return the {@link IncrementOptions} for chaining purposes.
   */
  public IncrementOptions cas(long cas) {
    this.cas = cas;
    return this;
  }

  @Stability.Internal
  public Built build() {
    return new Built();
  }

  public class Built extends BuiltCommonDurabilityOptions {

    Built() { }

    public Duration expiry() {
      return expiry;
    }

    public Optional<Long> initial() {
      return initial;
    }

    public long delta() {
      return delta;
    }

    public long cas() { return cas; }

  }

}
