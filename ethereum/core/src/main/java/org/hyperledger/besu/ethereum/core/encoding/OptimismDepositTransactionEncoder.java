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
package org.hyperledger.besu.ethereum.core.encoding;

import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.ethereum.core.Transaction;
import org.hyperledger.besu.ethereum.rlp.RLPOutput;

import java.math.BigInteger;

import org.apache.tuweni.bytes.Bytes;

public class OptimismDepositTransactionEncoder {

  public static void encode(final Transaction transaction, final RLPOutput out) {
    out.startList();
    out.writeBytes(transaction.getSourceHash().orElseThrow());
    out.writeBytes(transaction.getSender());
    out.writeBytes(transaction.getTo().map(Bytes::copy).orElse(Bytes.EMPTY));
    out.writeUInt256Scalar(transaction.getMint().orElse(Wei.ZERO));
    out.writeUInt256Scalar(transaction.getValue() != null ? transaction.getValue() : Wei.ZERO);
    out.writeLongScalar(transaction.getGasLimit());
    out.writeBigIntegerScalar(
        transaction.getIsSystemTx().map(b -> b ? BigInteger.ONE : BigInteger.ZERO).orElseThrow());
    out.writeBytes(transaction.getPayload() != null ? transaction.getPayload() : Bytes.EMPTY);
    out.endList();
  }
}
