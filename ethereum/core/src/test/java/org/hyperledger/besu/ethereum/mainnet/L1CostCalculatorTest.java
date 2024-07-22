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
package org.hyperledger.besu.ethereum.mainnet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hyperledger.besu.datatypes.RollupGasData;
import org.hyperledger.besu.datatypes.Wei;

import java.util.Arrays;
import java.util.List;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class L1CostCalculatorTest {
  private static UInt256 baseFee;
  private static UInt256 overhead;
  private static UInt256 scalar;

  private static UInt256 blobBaseFee;
  private static UInt256 baseFeeScalar;
  private static UInt256 blobBaseFeeScalar;

  private static Wei bedrockFee;
  private static Wei regolithFee;
  private static Wei ecotoneFee;
  private static RollupGasData emptyRollData;

  @BeforeAll
  static void setUp() {
    baseFee = UInt256.valueOf(1000 * 1000_000);
    overhead = UInt256.valueOf(50);
    scalar = UInt256.valueOf(7 * 1000_000);

    blobBaseFee = UInt256.valueOf(10 * 1000_000);
    baseFeeScalar = UInt256.valueOf(2);
    blobBaseFeeScalar = UInt256.valueOf(3);

    bedrockFee = Wei.of(UInt256.valueOf(11326000000000L));
    regolithFee = Wei.of(UInt256.valueOf(3710000000000L));
    ecotoneFee = Wei.of(UInt256.valueOf(960900));

    byte[] bytes = new byte[30];
    Arrays.fill(bytes, (byte) 1);
    emptyRollData = RollupGasData.fromPayload(Bytes.wrap(bytes));
  }

  @Test
  void l1CostInBedrockProcess() {
    Wei bedrockGasUsed =
        L1CostCalculator.l1CostInBedrockProcess(emptyRollData, baseFee, overhead, scalar, false);
    assertEquals(bedrockFee, bedrockGasUsed);

    Wei regolithGasUsed =
        L1CostCalculator.l1CostInBedrockProcess(emptyRollData, baseFee, overhead, scalar, true);
    assertEquals(regolithFee, regolithGasUsed);
  }

  @Test
  void l1CostInEcotone() {
    Wei ecotoneGasUsed =
        L1CostCalculator.l1CostInEcotone(
            emptyRollData, baseFee, blobBaseFee, baseFeeScalar, blobBaseFeeScalar);
    assertEquals(ecotoneFee, ecotoneGasUsed);
  }

  @Test
  void l1CostInFjord() {
    var funcBaseFee = UInt256.valueOf(1_000_000_000L);
    var funcBlobBaseFee = UInt256.valueOf(10_000_000L);
    var funcFeeScalar = UInt256.valueOf(2L);
    var funcBlobBaseFeeScalar = UInt256.valueOf(3L);

    List<Long> fastLzsize = List.of(100L, 150L, 170L);
    for (Long size : fastLzsize) {
      Wei wei =
          L1CostCalculator.l1CostFjord(
              new RollupGasData(0, 0, size),
              funcBaseFee,
              funcBlobBaseFee,
              funcFeeScalar,
              funcBlobBaseFeeScalar);
      assertEquals(Wei.of(UInt256.valueOf(3203000L)), wei);
    }

    fastLzsize = List.of(171L, 175L, 200L);
    for (Long size : fastLzsize) {
      Wei wei =
          L1CostCalculator.l1CostFjord(
              new RollupGasData(0, 0, size),
              funcBaseFee,
              funcBlobBaseFee,
              funcFeeScalar,
              funcBlobBaseFeeScalar);
      assertTrue(UInt256.valueOf(3203000L).compareTo(wei.toUInt256()) < 0);
    }
  }

  @Test
  void l1CostInFjord2() {
    var funcBaseFee = UInt256.valueOf(2000000L);
    var funcBlobBaseFee = UInt256.valueOf(3000000L);
    var funcFeeScalar = UInt256.valueOf(20L);
    var funcBlobBaseFeeScalar = UInt256.valueOf(15L);

    Wei wei =
        L1CostCalculator.l1CostFjord(
            new RollupGasData(0, 0, 235),
            funcBaseFee,
            funcBlobBaseFee,
            funcFeeScalar,
            funcBlobBaseFeeScalar);
    assertEquals(UInt256.valueOf(105484L), wei.toUInt256());
  }

  @Test
  void l1CostInFjord3() {
    var funcBaseFee = UInt256.valueOf(1000000000L);
    var funcBlobBaseFee = UInt256.valueOf(10000000L);
    var funcFeeScalar = UInt256.valueOf(2L);
    var funcBlobBaseFeeScalar = UInt256.valueOf(3L);

    Wei wei =
        L1CostCalculator.l1CostFjord(
            new RollupGasData(0, 30, 31),
            funcBaseFee,
            funcBlobBaseFee,
            funcFeeScalar,
            funcBlobBaseFeeScalar);
    assertEquals(UInt256.valueOf(3203000L), wei.toUInt256());
  }

  @Test
  void l1CostInFjord4() {
    var funcBaseFee = UInt256.valueOf(22232045986L);
    var funcBlobBaseFee = UInt256.valueOf(652134L);
    var funcFeeScalar = UInt256.valueOf(7600L);
    var funcBlobBaseFeeScalar = UInt256.valueOf(862000L);
    var rollUpGasData =
        RollupGasData.fromPayload(
            Bytes.fromHexString(
                "0x02f92ad783aa37dc8301393684068e778084069dbacc834a6815945ff137d4b0fdcd49dca30c7cf57e578a026d278980b92a641fad948c0000000000000000000000000000000000000000000000000000000000000040000000000000000000000000cfa706659d0bbe85ebb5c145a2d9f1c89c76b1c40000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000006400000000000000000000000000000000000000000000000000000000000000b8000000000000000000000000000000000000000000000000000000000000010c000000000000000000000000000000000000000000000000000000000000015c00000000000000000000000000000000000000000000000000000000000001aa00000000000000000000000000000000000000000000000000000000000001fe000000000000000000000000000000000000000000000000000000000000024c00000000000000000000000000ce101773c1fed47e0e11f028a168de6caba3bdf0000000000000000000000000000000000000000000000000000000000000333000000000000000000000000000000000000000000000000000000000000016000000000000000000000000000000000000000000000000000000000000001800000000000000000000000000000000000000000000000000000000000009e160000000000000000000000000000000000000000000000000000000000019bfd000000000000000000000000000000000000000000000000000000000000cb4000000000000000000000000000000000000000000000000000000000069dbacc00000000000000000000000000000000000000000000000000000000068e778000000000000000000000000000000000000000000000000000000000000002a00000000000000000000000000000000000000000000000000000000000000360000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000e40000189a0000000000000000000000005fd84259d66cd46123540766be93dfe6d43130d7000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000600000000000000000000000000000000000000000000000000000000000000044a9059cbb0000000000000000000000003e9c6ea0deca37fe5312a9045706e8946121b4d50000000000000000000000000000000000000000000000000000000000000282000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000958817340e0a3435e06254f2ed411e6418cd070d6f000000000000000000000000000000000000000000000000000000006657532e0000000000000000000000000000000000000000000000000000000000000000a1640cdcc227c9da337db6bc52110a4c7c965dbd6cbff1bee1a09d5fd14fc4bd6cd86cb687a2e1b0fa7a9325731136a2476063f57e66a13459d107fec37e96191c000000000000000000000000000000000000000000000000000000000000000000000000000000000001c000000000000000000000000000000000000000000000000000000000000000400000000000000000000000001965cd0bf68db7d007613e79d8386d48b9061ea6000000000000000000000000000000000000000000000000000000000000016000000000000000000000000000000000000000000000000000000000665753400000000000000000000000000000000000000000000000000000000066574e909d764665dddccfa399306a13a741583d275182129b5e9b18cc02959ec84753ea00000000000000000000000000000000000000000000000000000000000000a000000000000000000000000000000000000000000000000000000000000000e0000000000000000000000000000000000000000000000000000000000000000192443a8b54b6efbc27641ac942d12aba9bdd4e13b483e5c9ca7233d169eedee400000000000000000000000000000000000000000000000000000000000000414b9b57df8a561f7f588a6bc79d763d94fd276e5f4998f6e1412bc8307cc37af47cfd69bbe6aac67b7cd6380e281b837138158f1f6e9ff509bee312d5971035541c0000000000000000000000000000000000000000000000000000000000000000000000000000000000000017945d4c68e60ed9dc924a86f79e9fa17957d4c80000000000000000000000000000000000000000000000000000000000000338000000000000000000000000000000000000000000000000000000000000016000000000000000000000000000000000000000000000000000000000000001800000000000000000000000000000000000000000000000000000000000009e160000000000000000000000000000000000000000000000000000000000019bfd000000000000000000000000000000000000000000000000000000000000cb4000000000000000000000000000000000000000000000000000000000069dbacc00000000000000000000000000000000000000000000000000000000068e778000000000000000000000000000000000000000000000000000000000000002a00000000000000000000000000000000000000000000000000000000000000360000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000e40000189a0000000000000000000000005fd84259d66cd46123540766be93dfe6d43130d7000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000600000000000000000000000000000000000000000000000000000000000000044a9059cbb0000000000000000000000003e9c6ea0deca37fe5312a9045706e8946121b4d50000000000000000000000000000000000000000000000000000000000000282000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000958817340e0a3435e06254f2ed411e6418cd070d6f000000000000000000000000000000000000000000000000000000006657532e000000000000000000000000000000000000000000000000000000000000000078d31cbb6431eca16f545d61c07cf4c655073045d5191f4a235342d55721166b6077f2c473e62eb84d459ff6abcc51254345373cf25d59272873853a64dc3ece1b000000000000000000000000000000000000000000000000000000000000000000000000000000000001c000000000000000000000000000000000000000000000000000000000000000400000000000000000000000001965cd0bf68db7d007613e79d8386d48b9061ea6000000000000000000000000000000000000000000000000000000000000016000000000000000000000000000000000000000000000000000000000665753420000000000000000000000000000000000000000000000000000000066574e92dd485cdcb72d516c040b0c6a438174001a046f8321c74564c2212640106a7fc100000000000000000000000000000000000000000000000000000000000000a000000000000000000000000000000000000000000000000000000000000000e00000000000000000000000000000000000000000000000000000000000000001cd51e9aae80ded84d02fb61abffd3706a36d0a7a93c8b890191c75f3ee577e8000000000000000000000000000000000000000000000000000000000000000416be4e940ebf61d42383fb695722a340dcf31f837c6814fbc2f6b2f193fe69003339f80436ae68a6674607200f069377e6334eb132865779c6d9d8eb5dae7da791c000000000000000000000000000000000000000000000000000000000000000000000000000000000000003aecdf2db1e82cf17038daee4eeb307f3336cd730000000000000000000000000000000000000000000000000000000000000333000000000000000000000000000000000000000000000000000000000000016000000000000000000000000000000000000000000000000000000000000001800000000000000000000000000000000000000000000000000000000000009e160000000000000000000000000000000000000000000000000000000000019bfd000000000000000000000000000000000000000000000000000000000000cb4000000000000000000000000000000000000000000000000000000000069dbacc00000000000000000000000000000000000000000000000000000000068e778000000000000000000000000000000000000000000000000000000000000002a00000000000000000000000000000000000000000000000000000000000000360000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000e40000189a0000000000000000000000005fd84259d66cd46123540766be93dfe6d43130d7000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000600000000000000000000000000000000000000000000000000000000000000044a9059cbb0000000000000000000000003e9c6ea0deca37fe5312a9045706e8946121b4d50000000000000000000000000000000000000000000000000000000000000282000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000958817340e0a3435e06254f2ed411e6418cd070d6f000000000000000000000000000000000000000000000000000000006657532e00000000000000000000000000000000000000000000000000000000000000003b0e99b9cca9828193abb297a0c29ae601e6a8a361f2371e00c0dca72f192c126bbe8b7e4c95f5008f06a3bc0f88f68c48f331d305387a7576c733f49bcc52171b000000000000000000000000000000000000000000000000000000000000000000000000000000000001c000000000000000000000000000000000000000000000000000000000000000400000000000000000000000001965cd0bf68db7d007613e79d8386d48b9061ea6000000000000000000000000000000000000000000000000000000000000016000000000000000000000000000000000000000000000000000000000665753440000000000000000000000000000000000000000000000000000000066574e94889abe46cea3fd69752c52fc5315b4b21f1dc49de40471a0b1c8caeb5d11cd2200000000000000000000000000000000000000000000000000000000000000a000000000000000000000000000000000000000000000000000000000000000e0000000000000000000000000000000000000000000000000000000000000000188d816417f451d1cfc74da4b12509a292915be16ec4a9c926d6a68462f2faaa30000000000000000000000000000000000000000000000000000000000000041d32067a87ab7d0effbfb2ee5584005b6cef9fa1b08c845146fd6a78efe2f0d6d269a461173eae10d56ca941ed99a9d4b8749a2dd49796e29357c3d4b8fac14371c000000000000000000000000000000000000000000000000000000000000000000000000000000000000004c541cb68820a72029682f61a1421f58f62b905f0000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000000000000000000000000016000000000000000000000000000000000000000000000000000000000000001800000000000000000000000000000000000000000000000000000000000004cdb0000000000000000000000000000000000000000000000000000000000019b87000000000000000000000000000000000000000000000000000000000000c92400000000000000000000000000000000000000000000000000000000069dbacc00000000000000000000000000000000000000000000000000000000068e778000000000000000000000000000000000000000000000000000000000000002600000000000000000000000000000000000000000000000000000000000000320000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000a40000189a0000000000000000000000004bd0b3b6325676644b7e8091c1683df3092e14d9000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000600000000000000000000000000000000000000000000000000000000000000004183ff085000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000958817340e0a3435e06254f2ed411e6418cd070d6f000000000000000000000000000000000000000000000000000000006657533800000000000000000000000000000000000000000000000000000000000000007581619c4faf4a16190b3da8d6d60f401bdfc91da1b27d42810236c30bdab0bc548aea1c2fa45b6ec95a14ac6c26835be3809f51ec8c9ab1362b4462b3efe4611b000000000000000000000000000000000000000000000000000000000000000000000000000000000001c000000000000000000000000000000000000000000000000000000000000000400000000000000000000000001965cd0bf68db7d007613e79d8386d48b9061ea60000000000000000000000000000000000000000000000000000000000000160000000000000000000000000000000000000000000000000000000006657533e0000000000000000000000000000000000000000000000000000000066574e8e417f13ba0f65553fb36522711b73060438cfa569a0ac4aee7304ae4a766f9c3b00000000000000000000000000000000000000000000000000000000000000a000000000000000000000000000000000000000000000000000000000000000e00000000000000000000000000000000000000000000000000000000000000001e900b8541679f627ffb517bf976e5e9ac2b96f537748cd3ec809878564f16a2a0000000000000000000000000000000000000000000000000000000000000041c587b62e6f564726c82bc290c85cfc62ff766e0503789f897a17620516b10b8457774d59449afac9ac57942b9725fadb641c74b4f4282d50e848e78b34e7782c1b00000000000000000000000000000000000000000000000000000000000000000000000000000000000000546e42ceb74596b77dededf887960bba7680e97700000000000000000000000000000000000000000000000000000000000000350000000000000000000000000000000000000000000000000000000000000160000000000000000000000000000000000000000000000000000000000000018000000000000000000000000000000000000000000000000000000000000057370000000000000000000000000000000000000000000000000000000000019b4b000000000000000000000000000000000000000000000000000000000000c89400000000000000000000000000000000000000000000000000000000069dbacc00000000000000000000000000000000000000000000000000000000068e778000000000000000000000000000000000000000000000000000000000000002400000000000000000000000000000000000000000000000000000000000000300000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000840000189a000000000000000000000000a71b7c6d54aea61e56eb7ba91146e078701045e700000000000000000000000000000000000000000000000000000da475abf000000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000958817340e0a3435e06254f2ed411e6418cd070d6f0000000000000000000000000000000000000000000000000000000066575310000000000000000000000000000000000000000000000000000000000000000004fcd4f8107d05d209745ee34ab949f730ce7bd9a6ac9ada1069f6515af3fff53c9faa5ca5a807cc80c0d62d5d6b0b297c3b3563667368d458696f74bc0ca6411c000000000000000000000000000000000000000000000000000000000000000000000000000000000001c000000000000000000000000000000000000000000000000000000000000000400000000000000000000000001965cd0bf68db7d007613e79d8386d48b9061ea60000000000000000000000000000000000000000000000000000000000000160000000000000000000000000000000000000000000000000000000006657556c00000000000000000000000000000000000000000000000000000000665750bc309269900479e1f9ff4a2e8382e55bd87d2c37df5c83539cd604d9f67e1d28be00000000000000000000000000000000000000000000000000000000000000a000000000000000000000000000000000000000000000000000000000000000e00000000000000000000000000000000000000000000000000000000000000001f2870b5200d28c0946022b779a070e077a34e5e2c7c17c826d4dfaf37c3f9141000000000000000000000000000000000000000000000000000000000000004113afbbe1ce40ca4032529f677f4a6507e0d4b91231b466d5072a021b92fc08c874bb4e58608d7ef22b8a0fad605436e50f0962540cdcedf773e3acb2fc449e0b1b000000000000000000000000000000000000000000000000000000000000000000000000000000000000007a7542bfeac979a1e68e33131c5c4c21697fc5d20000000000000000000000000000000000000000000000000000000000000332000000000000000000000000000000000000000000000000000000000000016000000000000000000000000000000000000000000000000000000000000001800000000000000000000000000000000000000000000000000000000000009e160000000000000000000000000000000000000000000000000000000000019bfd000000000000000000000000000000000000000000000000000000000000cb4000000000000000000000000000000000000000000000000000000000069dbacc00000000000000000000000000000000000000000000000000000000068e778000000000000000000000000000000000000000000000000000000000000002a00000000000000000000000000000000000000000000000000000000000000360000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000e40000189a0000000000000000000000005fd84259d66cd46123540766be93dfe6d43130d7000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000600000000000000000000000000000000000000000000000000000000000000044a9059cbb0000000000000000000000003e9c6ea0deca37fe5312a9045706e8946121b4d50000000000000000000000000000000000000000000000000000000000000282000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000958817340e0a3435e06254f2ed411e6418cd070d6f000000000000000000000000000000000000000000000000000000006657532e000000000000000000000000000000000000000000000000000000000000000066046683044d66e4f32a1c8f72fb6b4bf8495cdcd76c9ef18e05534bc40f9b451001aec263d4397efca190917d21a0097545e7939bdb1b0b97565099494be57f1c000000000000000000000000000000000000000000000000000000000000000000000000000000000001c000000000000000000000000000000000000000000000000000000000000000400000000000000000000000001965cd0bf68db7d007613e79d8386d48b9061ea6000000000000000000000000000000000000000000000000000000000000016000000000000000000000000000000000000000000000000000000000665753460000000000000000000000000000000000000000000000000000000066574e96e59adfa7cfcbd44afd173daf0ef2737dd3c0febf13e6c5c809d3ab13600e5ee300000000000000000000000000000000000000000000000000000000000000a000000000000000000000000000000000000000000000000000000000000000e00000000000000000000000000000000000000000000000000000000000000001dd65708cccb9aa642d81d15c579756a58a0a5dc78375520426533b3bbc09293400000000000000000000000000000000000000000000000000000000000000417b2f4eabe748a69bd0a3600349873b06e2d1fc9c32359bccd3dcc8c3c1ea756c2ad4ce5d7059700e4f866ae7a5504ec689f4576e8afa40face78983322c2be501b0000000000000000000000000000000000000000000000000000000000000000000000000000000000000086bf7f8f157509ed296485887a3e7c5de71975ed00000000000000000000000000000000000000000000000000000000000004960000000000000000000000000000000000000000000000000000000000000160000000000000000000000000000000000000000000000000000000000000018000000000000000000000000000000000000000000000000000000000000057370000000000000000000000000000000000000000000000000000000000019b4b000000000000000000000000000000000000000000000000000000000000c8a000000000000000000000000000000000000000000000000000000000069dbacc00000000000000000000000000000000000000000000000000000000068e778000000000000000000000000000000000000000000000000000000000000002400000000000000000000000000000000000000000000000000000000000000300000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000840000189a000000000000000000000000ca9eef6ab0595c2c2a926b5aa12b673ae69462fb0000000000000000000000000000000000000000000000000000105ef39b2000000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000958817340e0a3435e06254f2ed411e6418cd070d6f000000000000000000000000000000000000000000000000000000006657534200000000000000000000000000000000000000000000000000000000000000003e09853ae4682efdf309d09af7c5481df4152a5f997b56ab4e42a991c599324750e30f76001073b5469cac46068f1fab9202d7cecc85ddd0e65f5d9cea0eb12b1b000000000000000000000000000000000000000000000000000000000000000000000000000000000001c000000000000000000000000000000000000000000000000000000000000000400000000000000000000000001965cd0bf68db7d007613e79d8386d48b9061ea60000000000000000000000000000000000000000000000000000000000000160000000000000000000000000000000000000000000000000000000006657559d00000000000000000000000000000000000000000000000000000000665750ed77177799f78621fa87d83126d829f06c2e7cd4860f3c9ca4f4f1c7b03e1effd600000000000000000000000000000000000000000000000000000000000000a000000000000000000000000000000000000000000000000000000000000000e000000000000000000000000000000000000000000000000000000000000000011a97a30055598ff5d310ce62a953c7efa17083bf2043aa757ad3fcf34a6676840000000000000000000000000000000000000000000000000000000000000041625380383980b37744db6001c9dfc9313097c4ad5d963f974fb4657dae1c19211b5aaf414b87a794ff236d111576a978c8678e7756eeabea65de27541ae6f5201b00000000000000000000000000000000000000000000000000000000000000000000000000000000000000e8190d9cb6bef482c8315061ecd4be1eb08440eb00000000000000000000000000000000000000000000000000000000000002ea000000000000000000000000000000000000000000000000000000000000016000000000000000000000000000000000000000000000000000000000000001800000000000000000000000000000000000000000000000000000000000009e160000000000000000000000000000000000000000000000000000000000019bfd000000000000000000000000000000000000000000000000000000000000cb4000000000000000000000000000000000000000000000000000000000069dbacc00000000000000000000000000000000000000000000000000000000068e778000000000000000000000000000000000000000000000000000000000000002a00000000000000000000000000000000000000000000000000000000000000360000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000e40000189a0000000000000000000000005fd84259d66cd46123540766be93dfe6d43130d7000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000600000000000000000000000000000000000000000000000000000000000000044a9059cbb0000000000000000000000003e9c6ea0deca37fe5312a9045706e8946121b4d50000000000000000000000000000000000000000000000000000000000000282000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000958817340e0a3435e06254f2ed411e6418cd070d6f00000000000000000000000000000000000000000000000000000000665753380000000000000000000000000000000000000000000000000000000000000000caf938c7babf46dcee19335db9e0da279335335d00a8e895f90aa8bd3dc3fb101decca25e8bad1d90a8de818dd65f2aa2026fdffe8a7d00f37a9b8e8da9721421b000000000000000000000000000000000000000000000000000000000000000000000000000000000001c000000000000000000000000000000000000000000000000000000000000000400000000000000000000000001965cd0bf68db7d007613e79d8386d48b9061ea60000000000000000000000000000000000000000000000000000000000000160000000000000000000000000000000000000000000000000000000006657534c0000000000000000000000000000000000000000000000000000000066574e9cf58317f65dd53973aeca652dc56709959425b4dd821a6689780debf334c23f3800000000000000000000000000000000000000000000000000000000000000a000000000000000000000000000000000000000000000000000000000000000e000000000000000000000000000000000000000000000000000000000000000014a0c7d57cd365dd513782b6674da5754b2c229646aee2215b062bf4b861cc50600000000000000000000000000000000000000000000000000000000000000412efbaad853f6c2a374a0bcc9dfef8a54ff66ed9c2d199b8b4f679c1561bda62910b2b92a69232f26b033476e026e40c37923e401fd066de36ae3c37738726c0f1c00000000000000000000000000000000000000000000000000000000000000c001a0ff5343a591142a2f6d35d978d92b9e5cb0a32c89054d72508252a8bb563f3ccba03ff63e1121bde22fbe776a577fe18f7361e224ea6505c855a31f59d58d35f57b"));

    Wei wei =
        L1CostCalculator.l1CostFjord(
            rollUpGasData, funcBaseFee, funcBlobBaseFee, funcFeeScalar, funcBlobBaseFeeScalar);
    assertEquals(UInt256.valueOf(6466915509330L), wei.toUInt256());
  }
}