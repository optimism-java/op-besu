package org.hyperledger.besu.ethereum.mainnet;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.RollupGasData;
import org.hyperledger.besu.datatypes.Wei;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        L1CostCalculator.l1CostInEcotone(emptyRollData, baseFee, blobBaseFee, baseFeeScalar, blobBaseFeeScalar);
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
      Wei wei = L1CostCalculator.l1CostFjord(
          new RollupGasData(0, 0, size),
          funcBaseFee,
          funcBlobBaseFee,
          funcFeeScalar,
          funcBlobBaseFeeScalar
      );
      assertEquals(Wei.of(UInt256.valueOf(3203000L)), wei);
    }

    fastLzsize = List.of(171L, 175L, 200L);
    for (Long size : fastLzsize) {
      Wei wei = L1CostCalculator.l1CostFjord(
          new RollupGasData(0, 0, size),
          funcBaseFee,
          funcBlobBaseFee,
          funcFeeScalar,
          funcBlobBaseFeeScalar
      );
      assertTrue(UInt256.valueOf(3203000L).compareTo(wei.toUInt256()) < 0);
    }
  }
    @Test
    void l1CostInFjord2() {
      var funcBaseFee = UInt256.valueOf(2000000L);
      var funcBlobBaseFee = UInt256.valueOf(3000000L);
      var funcFeeScalar = UInt256.valueOf(20L);
      var funcBlobBaseFeeScalar = UInt256.valueOf(15L);

      Wei wei = L1CostCalculator.l1CostFjord(
          new RollupGasData(0, 0, 235),
          funcBaseFee,
          funcBlobBaseFee,
          funcFeeScalar,
          funcBlobBaseFeeScalar
      );
      assertEquals(UInt256.valueOf(105484L), wei.toUInt256());
    }

  @Test
  void l1CostInFjord3() {
    var funcBaseFee = UInt256.valueOf(1000000000L);
    var funcBlobBaseFee = UInt256.valueOf(10000000L);
    var funcFeeScalar = UInt256.valueOf(2L);
    var funcBlobBaseFeeScalar = UInt256.valueOf(3L);

    Wei wei = L1CostCalculator.l1CostFjord(
        new RollupGasData(0, 30, 31),
        funcBaseFee,
        funcBlobBaseFee,
        funcFeeScalar,
        funcBlobBaseFeeScalar
    );
    assertEquals(UInt256.valueOf(3203000L), wei.toUInt256());
  }

}