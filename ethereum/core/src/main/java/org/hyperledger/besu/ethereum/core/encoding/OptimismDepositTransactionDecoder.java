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
package org.hyperledger.besu.ethereum.core.encoding;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.datatypes.TransactionType;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.ethereum.core.Transaction;
import org.hyperledger.besu.ethereum.rlp.RLPInput;

import java.math.BigInteger;

public class OptimismDepositTransactionDecoder {

  public static Transaction decode(final RLPInput input) {
    input.enterList();
    final Transaction.Builder builder =
        Transaction.builder()
            .type(TransactionType.OPTIMISM_DEPOSIT)
            .sourceHash(Hash.wrap(input.readBytes32()))
            .sender(Address.wrap(input.readBytes()))
            .to(input.readBytes(v -> v.isEmpty() ? null : Address.wrap(v)))
            .mint(Wei.of(input.readUInt256Scalar()))
            .value(Wei.of(input.readUInt256Scalar()))
            .gasLimit(input.readLongScalar())
            .isSystemTx(input.readBigIntegerScalar().compareTo(BigInteger.ONE) == 0)
            .payload(input.readBytes());
    input.leaveList();
    return builder.build();
  }
}
