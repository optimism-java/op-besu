/*
 * Copyright optimism-java.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.datatypes;

import org.apache.tuweni.bytes.Bytes;

/** Optimism roll up data records. */
public class RollupGasData {

  private static final RollupGasData empty = new RollupGasData(0L, 0L);

  private final long zeroes;

  private final long ones;

  RollupGasData(final long zeroes, final long ones) {
    this.zeroes = zeroes;
    this.ones = ones;
  }

  public long getZeroes() {
    return zeroes;
  }

  public long getOnes() {
    return ones;
  }

  public static RollupGasData fromPayload(final Bytes payload) {
    if (payload == null) {
      return empty;
    }
    final int length = payload.size();
    int zeroes = 0;
    int ones = 0;
    for (int i = 0; i < length; i++) {
      byte b = payload.get(i);
      if (b == 0) {
        zeroes++;
      } else {
        ones++;
      }
    }
    return new RollupGasData(zeroes, ones);
  }
}
