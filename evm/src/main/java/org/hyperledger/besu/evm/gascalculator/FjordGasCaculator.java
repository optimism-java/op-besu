/*
 * Copyright contributors to Hyperledger Besu.
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
package org.hyperledger.besu.evm.gascalculator;

import org.hyperledger.besu.datatypes.Address;

import static org.hyperledger.besu.datatypes.Address.KZG_POINT_EVAL;
import static org.hyperledger.besu.datatypes.Address.P256_VERIFY;

/**
 * Gas Calculator for Fjord
 *
 * <UL>
 *   <LI>Gas costs for TSTORE/TLOAD
 *   <LI>Blob gas for EIP-4844
 * </UL>
 */
public class FjordGasCaculator extends CancunGasCalculator {

  private int maxPrecompile;

  /** Instantiates a new Cancun Gas Calculator. */
  public FjordGasCaculator() {
    this(256);
    this.maxPrecompile = 256;
  }

  /**
   * Instantiates a new Cancun Gas Calculator
   *
   * @param maxPrecompile the max precompile
   */
  protected FjordGasCaculator(final int maxPrecompile) {
    super(maxPrecompile);
    this.maxPrecompile = maxPrecompile;
  }

  @Override
  public boolean isPrecompile(final Address address) {
    final byte[] addressBytes = address.toArrayUnsafe();
    for (int i = 0; i < 18; i++) {
      if (addressBytes[i] != 0) {
        return false;
      }
    }
    final int lastBytes = ((addressBytes[18] & 0xff) << 8) | (addressBytes[19] & 0xff);
    return lastBytes <= maxPrecompile && lastBytes != 0;
  }
}
