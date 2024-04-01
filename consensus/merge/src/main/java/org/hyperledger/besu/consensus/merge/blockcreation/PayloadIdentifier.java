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
package org.hyperledger.besu.consensus.merge.blockcreation;

import com.google.common.primitives.Longs;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.datatypes.Quantity;
import org.hyperledger.besu.ethereum.core.Transaction;
import org.hyperledger.besu.ethereum.core.Withdrawal;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt64;
import org.hyperledger.besu.ethereum.rlp.BytesValueRLPOutput;
import org.web3j.utils.Numeric;

/** The Payload identifier. */
public class PayloadIdentifier implements Quantity {

  private final UInt64 val;

  /**
   * Instantiates a new Payload identifier.
   *
   * @param payloadId the payload id
   */
  @JsonCreator
  public PayloadIdentifier(final String payloadId) {
    if (payloadId.startsWith("0x")) {
      this.val = UInt64.fromHexString(payloadId);
    } else {
      this.val = UInt64.valueOf(Math.abs(Long.parseLong(payloadId)));
    }
  }

  /**
   * Instantiates a new Payload identifier.
   *
   * @param payloadId the payload id
   */
  public PayloadIdentifier(final Long payloadId) {
    this.val = UInt64.valueOf(Math.abs(payloadId));
  }

  /**
   * Instantiates a new Payload identifier.
   *
   * @param payloadId the payload id
   */
  public PayloadIdentifier(final BigInteger payloadId) {
    this.val = UInt64.valueOf(payloadId);
  }

  /**
   * Create payload identifier for payload params. This is a deterministic hash of all payload
   * parameters that aims to avoid collisions
   *
   * @param parentHash the parent hash
   * @param timestamp the timestamp
   * @param prevRandao the prev randao
   * @param feeRecipient the fee recipient
   * @param withdrawals the withdrawals
   * @param parentBeaconBlockRoot the parent beacon block root
   * @return the payload identifier
   */
  public static PayloadIdentifier forPayloadParams(
      final Hash parentHash,
      final Long timestamp,
      final Bytes32 prevRandao,
      final Address feeRecipient,
      final Optional<List<Withdrawal>> withdrawals,
      final Optional<Bytes32> parentBeaconBlockRoot) {

    return new PayloadIdentifier(
        timestamp
            ^ ((long) parentHash.toHexString().hashCode()) << 8
            ^ ((long) prevRandao.toHexString().hashCode()) << 16
            ^ ((long) feeRecipient.toHexString().hashCode()) << 24
            ^ (long)
                withdrawals
                    .map(
                        ws ->
                            ws.stream()
                                .sorted(Comparator.comparing(Withdrawal::getIndex))
                                .map(Withdrawal::hashCode)
                                .reduce(1, (a, b) -> a ^ (b * 31)))
                    .orElse(0)
            ^ ((long) parentBeaconBlockRoot.hashCode()) << 40);
  }

  /**
   * Create payload identifier for payload params. This is a deterministic hash of all payload
   * parameters that aims to avoid collisions
   *
   * @param parentHash the parent hash
   * @param timestamp the timestamp
   * @param prevRandao the prev randao
   * @param feeRecipient the fee recipient
   * @param withdrawals the withdrawals
   * @param parentBeaconBlockRoot the parent beacon block root
   * @param noTxPool the no tx pool
   * @param transactions the transactions
   * @param gasLimit the gas limit
   * @return the payload identifier
   */
  public static PayloadIdentifier forPayloadParams(
          final Hash parentHash,
          final Long timestamp,
          final Bytes32 prevRandao,
          final Address feeRecipient,
          final Optional<List<Withdrawal>> withdrawals,
          final Optional<Bytes32> parentBeaconBlockRoot,
          final Optional<Boolean> noTxPool,
          final Optional<List<Transaction>> transactions,
          final Optional<Long> gasLimit) {
    // Optimism: gasLimit will exist
    if (gasLimit.isPresent()) {
      SHA256.Digest digest = new SHA256.Digest();
      digest.update(parentHash.toArrayUnsafe());
      digest.update(Longs.toByteArray(timestamp));
      digest.update(prevRandao.toArrayUnsafe());
      digest.update(feeRecipient.toArrayUnsafe());
      final BytesValueRLPOutput out = new BytesValueRLPOutput();
      if (withdrawals.isPresent()) {
        out.writeList(withdrawals.get(), Withdrawal::writeTo);
      } else {
        out.writeEmptyList();
      }
      digest.update(out.encoded().toArrayUnsafe());
      parentBeaconBlockRoot.ifPresent(
              parentBeaconBlockRootBytes -> digest.update(parentBeaconBlockRootBytes.toArrayUnsafe()));
      boolean noTxPoolFlag = noTxPool.orElse(false);
      List<Transaction> txList = transactions.orElse(Collections.emptyList());
      if (noTxPoolFlag || !txList.isEmpty()) {
        digest.update(new byte[] {(byte) (noTxPoolFlag ? 1 : 0)});
        digest.update(Longs.toByteArray(txList.size()));
        txList.forEach(tx -> digest.update(tx.getHash().toArrayUnsafe()));
      }
      digest.update(Longs.toByteArray(gasLimit.get()));

      return new PayloadIdentifier(Numeric.toBigInt(digest.digest(), 0, 8));
    }
    return forPayloadParams(parentHash, timestamp, prevRandao, feeRecipient, withdrawals, parentBeaconBlockRoot);
  }

  @Override
  public Number getValue() {
    return getAsBigInteger();
  }

  @Override
  public BigInteger getAsBigInteger() {
    return val.toBigInteger();
  }

  @Override
  public String toHexString() {
    return val.toHexString();
  }

  @Override
  public String toShortHexString() {
    var shortHex = val.toShortHexString();
    if (shortHex.length() % 2 != 0) {
      shortHex = "0x0" + shortHex.substring(2);
    }
    return shortHex;
  }

  /**
   * Serialize to hex string.
   *
   * @return the string
   */
  @JsonValue
  public String serialize() {
    return toShortHexString();
  }

  @Override
  public boolean equals(final Object o) {
    if (o instanceof PayloadIdentifier) {
      return getAsBigInteger().equals(((PayloadIdentifier) o).getAsBigInteger());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return val.hashCode();
  }

  @Override
  public String toString() {
    return toHexString();
  }
}
