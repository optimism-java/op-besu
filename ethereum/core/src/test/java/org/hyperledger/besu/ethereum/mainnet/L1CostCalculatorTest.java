package org.hyperledger.besu.ethereum.mainnet;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.RollupGasData;
import org.hyperledger.besu.datatypes.Wei;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

//  private static Wei ecotoneGas;
//  private static Wei regolithGas;
//  private static Wei bedrockGas;
  private static RollupGasData emptyRollData;

  //
  //	blobBaseFee       = big.NewInt(10 * 1e6)
  //	baseFeeScalar     = big.NewInt(2)
  //	blobBaseFeeScalar = big.NewInt(3)
  //
  //	// below are the expected cost func outcomes for the above parameter settings on the emptyTx
  //	// which is defined in transaction_test.go
  //	bedrockFee  = big.NewInt(11326000000000)
  //	regolithFee = big.NewInt(3710000000000)
  //	ecotoneFee  = big.NewInt(960900) // (480/16)*(2*16*1000 + 3*10) == 960900
  //
  //	bedrockGas  = big.NewInt(1618)
  //	regolithGas = big.NewInt(530) // 530  = 1618 - (16*68)
  //	ecotoneGas  = big.NewInt(480)

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

//    bedrockGas = Wei.of(UInt256.valueOf(1618L));
//    regolithGas = Wei.of(UInt256.valueOf(530));
//    ecotoneGas = Wei.of(UInt256.valueOf(480));

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
}