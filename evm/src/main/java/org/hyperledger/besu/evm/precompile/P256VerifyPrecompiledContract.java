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
package org.hyperledger.besu.evm.precompile;

import org.hyperledger.besu.crypto.SECP256R1;
import org.hyperledger.besu.crypto.SECPPublicKey;
import org.hyperledger.besu.crypto.SECPSignature;
import org.hyperledger.besu.evm.frame.MessageFrame;

import java.util.Arrays;

import org.apache.tuweni.bytes.Bytes;

public class P256VerifyPrecompiledContract implements PrecompiledContract {

  private static final long GAS_COST = 3450;

  /** The constant TRUE. */
  private static final Bytes TRUE =
      Bytes.fromHexString("0x0000000000000000000000000000000000000000000000000000000000000001");

  private final SECP256R1 secp256R1;

  public P256VerifyPrecompiledContract() {
    this.secp256R1 = new SECP256R1();
    this.secp256R1.disableNative();
  }

  @Override
  public String getName() {
    return "P256Verify";
  }

  @Override
  public long gasRequirement(final Bytes input) {
    return GAS_COST;
  }

  @Override
  public PrecompileContractResult computePrecompile(
      final Bytes input, final MessageFrame messageFrame) {
    if (input.size() != 160) {
      return PrecompileContractResult.success(Bytes.EMPTY);
    }
    final Bytes hash = extractParameterBytes(input, 0, 32);
    final Bytes rs = extractParameterBytes(input, 32, 64);
    final Bytes encodedpublicKey = extractParameterBytes(input, 96, 64);

    try {
      SECPPublicKey publicKey = this.secp256R1.createPublicKey(encodedpublicKey);
      SECPSignature signature = this.secp256R1.decodeSignature(Bytes.wrap(rs, Bytes.of(0)));
      if (this.secp256R1.verify(hash, signature, publicKey)) {
        return PrecompileContractResult.success(TRUE);
      }
    } catch (IllegalArgumentException e) {
      return PrecompileContractResult.success(Bytes.EMPTY);
    }
    return PrecompileContractResult.success(Bytes.EMPTY);
  }

  private static Bytes extractParameterBytes(
      final Bytes input, final int offset, final int length) {
    if (offset > input.size() || length == 0) {
      return Bytes.EMPTY;
    }
    final byte[] raw = Arrays.copyOfRange(input.toArray(), offset, offset + length);
    return Bytes.wrap(raw);
  }
}
