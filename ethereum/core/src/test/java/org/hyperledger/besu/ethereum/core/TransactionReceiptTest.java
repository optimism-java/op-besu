/*
 * Copyright ConsenSys AG.
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
package org.hyperledger.besu.ethereum.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.TransactionType;
import org.hyperledger.besu.ethereum.rlp.RLP;
import org.hyperledger.besu.evm.log.Log;
import org.hyperledger.besu.evm.log.LogTopic;
import org.hyperledger.besu.evm.log.LogsBloomFilter;

import java.util.Arrays;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;

public class TransactionReceiptTest {

  @Test
  public void toFromRlp() {
    final BlockDataGenerator gen = new BlockDataGenerator();
    final TransactionReceipt receipt = gen.receipt();
    final TransactionReceipt copy =
        TransactionReceipt.readFrom(RLP.input(RLP.encode(receipt::writeToWithRevertReason)), false);
    assertThat(copy).isEqualTo(receipt);
  }

  @Test
  public void toFromRlpWithReason() {
    final BlockDataGenerator gen = new BlockDataGenerator();
    final TransactionReceipt receipt = gen.receipt(Bytes.fromHexString("0x1122334455667788"));
    final TransactionReceipt copy =
        TransactionReceipt.readFrom(RLP.input(RLP.encode(receipt::writeToWithRevertReason)));
    assertThat(copy).isEqualTo(receipt);
  }

  @Test
  public void toFromRlpEIP1559() {
    String receiptHex =
        "0x02f901c58001b9010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000f8bef85d940000000000000000000000000000000000000011f842a0000000000000000000000000000000000000000000000000000000000000deada0000000000000000000000000000000000000000000000000000000000000beef830100fff85d940000000000000000000000000000000000000111f842a0000000000000000000000000000000000000000000000000000000000000deada0000000000000000000000000000000000000000000000000000000000000beef830100ff";

    final TransactionReceipt receipt =
        new TransactionReceipt(
            TransactionType.EIP1559,
            null,
            0,
            1,
            Arrays.asList(
                new Log(
                    Address.fromHexString("0x0000000000000000000000000000000000000011"),
                    Bytes.of(0x01, 0x00, 0xff),
                    Arrays.asList(
                        LogTopic.fromHexString(
                            "0x000000000000000000000000000000000000000000000000000000000000dead"),
                        LogTopic.fromHexString(
                            "0x000000000000000000000000000000000000000000000000000000000000beef"))),
                new Log(
                    Address.fromHexString("0x0000000000000000000000000000000000000111"),
                    Bytes.of(0x01, 0x00, 0xff),
                    Arrays.asList(
                        LogTopic.fromHexString(
                            "0x000000000000000000000000000000000000000000000000000000000000dead"),
                        LogTopic.fromHexString(
                            "0x000000000000000000000000000000000000000000000000000000000000beef")))),
            LogsBloomFilter.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());
    Bytes encoded =
        RLP.encode(
            rlpoutput -> {
              receipt.writeToForReceiptTrie(rlpoutput, false);
            });
    assertThat(encoded.toHexString()).isEqualTo(receiptHex);

    final TransactionReceipt copy =
        TransactionReceipt.readFrom(RLP.input(RLP.encode(receipt::writeTo)), false);
    assertThat(copy).isEqualTo(receipt);
  }

  @Test
  public void toFromDepositWithNonceRlp() {
    String receiptHex =
        "0x7ef901c58001b9010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000f8bef85d940000000000000000000000000000000000000011f842a0000000000000000000000000000000000000000000000000000000000000deada0000000000000000000000000000000000000000000000000000000000000beef830100fff85d940000000000000000000000000000000000000111f842a0000000000000000000000000000000000000000000000000000000000000deada0000000000000000000000000000000000000000000000000000000000000beef830100ff";
    final TransactionReceipt receipt =
        new TransactionReceipt(
            TransactionType.OPTIMISM_DEPOSIT,
            null,
            0,
            1,
            Arrays.asList(
                new Log(
                    Address.fromHexString("0x0000000000000000000000000000000000000011"),
                    Bytes.of(0x01, 0x00, 0xff),
                    Arrays.asList(
                        LogTopic.fromHexString(
                            "0x000000000000000000000000000000000000000000000000000000000000dead"),
                        LogTopic.fromHexString(
                            "0x000000000000000000000000000000000000000000000000000000000000beef"))),
                new Log(
                    Address.fromHexString("0x0000000000000000000000000000000000000111"),
                    Bytes.of(0x01, 0x00, 0xff),
                    Arrays.asList(
                        LogTopic.fromHexString(
                            "0x000000000000000000000000000000000000000000000000000000000000dead"),
                        LogTopic.fromHexString(
                            "0x000000000000000000000000000000000000000000000000000000000000beef")))),
            LogsBloomFilter.empty(),
            Optional.empty(),
            Optional.of(1234L),
            Optional.empty());
    Bytes encoded =
        RLP.encode(
            rlpoutput -> {
              receipt.writeToForReceiptTrie(rlpoutput, false);
            });
    assertThat(encoded.toHexString()).isEqualTo(receiptHex);

    var parsed = TransactionReceipt.readFrom(RLP.input(RLP.encode(receipt::writeTo)), false);
    var reEncoded =
        RLP.encode(
            rlpoutput -> {
              parsed.writeToForReceiptTrie(rlpoutput, false);
            });
    assertThat(reEncoded.toHexString()).isEqualTo(receiptHex);
  }

  @Test
  public void toFromDepositNoNonceRlp() {
    String receiptHex =
        "0x7ef901c58001b9010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000f8bef85d940000000000000000000000000000000000000011f842a0000000000000000000000000000000000000000000000000000000000000deada0000000000000000000000000000000000000000000000000000000000000beef830100fff85d940000000000000000000000000000000000000111f842a0000000000000000000000000000000000000000000000000000000000000deada0000000000000000000000000000000000000000000000000000000000000beef830100ff";
    final TransactionReceipt receipt =
        new TransactionReceipt(
            TransactionType.OPTIMISM_DEPOSIT,
            null,
            0,
            1,
            Arrays.asList(
                new Log(
                    Address.fromHexString("0x0000000000000000000000000000000000000011"),
                    Bytes.of(0x01, 0x00, 0xff),
                    Arrays.asList(
                        LogTopic.fromHexString(
                            "0x000000000000000000000000000000000000000000000000000000000000dead"),
                        LogTopic.fromHexString(
                            "0x000000000000000000000000000000000000000000000000000000000000beef"))),
                new Log(
                    Address.fromHexString("0x0000000000000000000000000000000000000111"),
                    Bytes.of(0x01, 0x00, 0xff),
                    Arrays.asList(
                        LogTopic.fromHexString(
                            "0x000000000000000000000000000000000000000000000000000000000000dead"),
                        LogTopic.fromHexString(
                            "0x000000000000000000000000000000000000000000000000000000000000beef")))),
            LogsBloomFilter.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());
    Bytes encoded =
        RLP.encode(
            rlpoutput -> {
              receipt.writeToForReceiptTrie(rlpoutput, false);
            });
    assertThat(encoded.toHexString()).isEqualTo(receiptHex);

    final TransactionReceipt copy =
        TransactionReceipt.readFrom(RLP.input(RLP.encode(receipt::writeTo)), false);
    assertThat(copy).isEqualTo(receipt);
  }

  @Test
  public void toFromDepositWithNonceAndVersionRlp() {
    String receiptHex =
        "0x7ef901c98001b9010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000f8bef85d940000000000000000000000000000000000000011f842a0000000000000000000000000000000000000000000000000000000000000deada0000000000000000000000000000000000000000000000000000000000000beef830100fff85d940000000000000000000000000000000000000111f842a0000000000000000000000000000000000000000000000000000000000000deada0000000000000000000000000000000000000000000000000000000000000beef830100ff8204d201";
    final TransactionReceipt receipt =
        new TransactionReceipt(
            TransactionType.OPTIMISM_DEPOSIT,
            null,
            0,
            1,
            Arrays.asList(
                new Log(
                    Address.fromHexString("0x0000000000000000000000000000000000000011"),
                    Bytes.of(0x01, 0x00, 0xff),
                    Arrays.asList(
                        LogTopic.fromHexString(
                            "0x000000000000000000000000000000000000000000000000000000000000dead"),
                        LogTopic.fromHexString(
                            "0x000000000000000000000000000000000000000000000000000000000000beef"))),
                new Log(
                    Address.fromHexString("0x0000000000000000000000000000000000000111"),
                    Bytes.of(0x01, 0x00, 0xff),
                    Arrays.asList(
                        LogTopic.fromHexString(
                            "0x000000000000000000000000000000000000000000000000000000000000dead"),
                        LogTopic.fromHexString(
                            "0x000000000000000000000000000000000000000000000000000000000000beef")))),
            LogsBloomFilter.empty(),
            Optional.empty(),
            Optional.of(1234L),
            Optional.of(1L));
    Bytes encoded =
        RLP.encode(
            rlpoutput -> {
              receipt.writeToForReceiptTrie(rlpoutput, false);
            });
    assertThat(encoded.toHexString()).isEqualTo(receiptHex);

    final TransactionReceipt copy =
        TransactionReceipt.readFrom(RLP.input(RLP.encode(receipt::writeTo)), false);
    assertThat(copy).isEqualTo(receipt);
  }
}
