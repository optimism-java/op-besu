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
package org.hyperledger.besu.ethereum.mainnet;

import org.hyperledger.besu.config.GenesisConfigOptions;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.RollupGasData;
import org.hyperledger.besu.datatypes.TransactionType;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.ethereum.core.ProcessableBlockHeader;
import org.hyperledger.besu.ethereum.core.Transaction;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.worldstate.WorldUpdater;

import org.apache.tuweni.units.bigints.UInt256;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Arrays;

/** L1 Cost calculator. */
public class L1CostCalculator {

  private static final UInt256 SIXTEEN = UInt256.valueOf(16L);

  private static final BigInteger L1_COST_INTERCEPT = BigInteger.valueOf(-42_585_600L);
  private static final UInt256 L1_COST_FAST_LZ_COEF = UInt256.valueOf(836_500L);

  private static final UInt256 MIN_TX_SIZE = UInt256.valueOf(100L);
  private static final UInt256 MIN_TX_SIZE_SCALED = UInt256.valueOf(1_000_000L).multiply(MIN_TX_SIZE);
  private static final UInt256 ECOTONE_DIVISOR = UInt256.valueOf(1_000_000L * 16L);
  private static final UInt256 FJORD_DIVISOR = UInt256.valueOf(1_000_000_000_000L);

  private static final int BASE_FEE_SCALAR_SLOT_OFFSET = 12;
//  private static final int BLOB_BASE_FEE_SCALAR_SLOT_OFFSET = 8;

  private static final int SCALAR_SECTION_START = 32 - BASE_FEE_SCALAR_SLOT_OFFSET - 4;

//  private static final byte[] BedrockL1AttributesSelector = new byte[]{0x01, 0x5d, (byte) 0x8e, (byte) 0xb9};
//  private static final byte[] EcotoneL1AttributesSelector = new byte[]{0x44, 0x0a, 0x5e, 0x20};

  private static final long TX_DATA_ZERO_COST = 4L;

  private static final long TX_DATA_NON_ZERO_GAS_EIP2028_COST = 16L;
  private static final long TX_DATA_NON_ZERO_GAS_FRONTIER_COST = 68L;

  private static final Address l1BlockAddr =
      Address.fromHexString("0x4200000000000000000000000000000000000015");
  private static final UInt256 l1BaseFeeSlot = UInt256.valueOf(1L);
  private static final UInt256 overheadSlot = UInt256.valueOf(5L);
  private static final UInt256 scalarSlot = UInt256.valueOf(6L);

  private static final byte[] emptyScalars = new byte[8];

  /**
   * l1BlobBaseFeeSlot was added with the Ecotone upgrade and stores the blobBaseFee L1 gas
   * attribute.
   */
  private static final UInt256 l1BlobBaseFeeSlot = UInt256.valueOf(7L);

  /**
   * l1FeeScalarsSlot as of the Ecotone upgrade stores the 32-bit basefeeScalar and
   * blobBaseFeeScalar L1 gas attributes at offsets `BaseFeeScalarSlotOffset` and
   * `BlobBaseFeeScalarSlotOffset` respectively.
   */
  private static final UInt256 l1FeeScalarsSlot = UInt256.valueOf(3L);

  public L1CostCalculator() {
  }

  /**
   * Calculates the l1 cost of transaction.
   *
   * @param options genesis config options
   * @param blockHeader block header info
   * @param transaction transaction is checked
   * @param worldState instance of the WorldState
   * @return l1 costed gas
   */
  public Wei l1Cost(
      final GenesisConfigOptions options,
      final ProcessableBlockHeader blockHeader,
      final Transaction transaction,
      final WorldUpdater worldState) {
    if (!options.isOptimism()) {
      return Wei.ZERO;
    }

    // Note: the various state variables below are not initialized from the DB until this
    // point to allow deposit transactions from the block to be processed first by state
    // transition.  This behavior is consensus critical!
    if (!options.isEcotone(blockHeader.getTimestamp())) {
      return l1CostInBedrock(options, blockHeader, transaction, worldState);
    } else {
      final MutableAccount systemConfig = worldState.getOrCreate(l1BlockAddr);

      var l1BlobBaseFee = systemConfig.getStorageValue(l1BlobBaseFeeSlot);
      var l1FeeScalars = systemConfig.getStorageValue(l1FeeScalarsSlot).toArray();
      byte[] l1FeeScalarBytes = Arrays.copyOfRange(l1FeeScalars, SCALAR_SECTION_START, SCALAR_SECTION_START + 8);

      // Edge case: the very first Ecotone block requires we use the Bedrock cost
      // function. We detect this scenario by checking if the Ecotone parameters are
      // unset.  Not here we rely on assumption that the scalar parameters are adjacent
      // in the buffer and basefeeScalar comes first.
      if (l1BlobBaseFee.bitLength() == 0 &&
              Arrays.equals(emptyScalars, l1FeeScalarBytes)) {

        return l1CostInBedrock(options, blockHeader, transaction, worldState);
      }

      var l1BaseFee = systemConfig.getStorageValue(l1BaseFeeSlot);
      var offset = SCALAR_SECTION_START;
      var l1BaseFeeScalar = UInt256.valueOf(Numeric.toBigInt(Arrays.copyOfRange(l1FeeScalars, offset, offset + 4)));
      var l1BlobBaseFeeScalar = UInt256.valueOf(Numeric.toBigInt(Arrays.copyOfRange(l1FeeScalars, offset + 4, offset + 8)));
      if (options.isFjord(blockHeader.getTimestamp())) {
        return l1CostFjord(transaction.getRollupGasData(), l1BaseFee, l1BlobBaseFee, l1BaseFeeScalar, l1BlobBaseFeeScalar);
      }
      return l1CostInEcotone(transaction.getRollupGasData(), l1BaseFee, l1BlobBaseFee, l1BaseFeeScalar, l1BlobBaseFeeScalar);
    }
  }

  static Wei l1CostInBedrock(
          final GenesisConfigOptions options,
          final ProcessableBlockHeader blockHeader,
          final Transaction tx,
          final WorldUpdater worldState) {
    final boolean isDepositTx = TransactionType.OPTIMISM_DEPOSIT.equals(tx.getType());
    if (isDepositTx) {
      return Wei.ZERO;
    }
    final boolean isRegolith = options.isRegolith(blockHeader.getTimestamp());
    final MutableAccount systemConfig = worldState.getOrCreate(l1BlockAddr);
    var l1BaseFee = systemConfig.getStorageValue(l1BaseFeeSlot);
    var overhead = systemConfig.getStorageValue(overheadSlot);
    var l1FeeScalar = systemConfig.getStorageValue(scalarSlot);
    return l1CostInBedrockProcess(tx.getRollupGasData(), l1BaseFee, overhead, l1FeeScalar, isRegolith);
  }

  static Wei l1CostInBedrockProcess(
      final RollupGasData rollupGasData,
      final UInt256 l1BaseFee,
      final UInt256 overhead,
      final UInt256 l1FeeScalar,
      final boolean isRegolith) {
    var gas = calculateRollupDataGasCost(rollupGasData, isRegolith);
    if (gas == 0L) {
      return Wei.ZERO;
    }
    UInt256 l1GasUsed =
        UInt256.valueOf(gas)
            .add(overhead)
            .multiply(l1BaseFee)
            .multiply(l1FeeScalar)
            .divide(UInt256.valueOf(1_000_000L));

    return Wei.of(l1GasUsed);
  }

  static Wei l1CostInEcotone(final RollupGasData costData,
                             final UInt256 l1BaseFee,
                             final UInt256 l1BlobBaseFee,
                             final UInt256 l1BaseFeeScalar,
                             final UInt256 l1BlobBaseFeeScalar) {
    //calldataGas = (costData.zeroes * 4) + (costData.ones * 16)
    var calldataGas = costData.getZeroes() * TX_DATA_ZERO_COST + costData.getOnes() * TX_DATA_NON_ZERO_GAS_EIP2028_COST;
    var calldataGasUsed = UInt256.valueOf(calldataGas);

    // Ecotone L1 cost function:
    //
    //   (calldataGas/16)*(l1BaseFee*16*l1BaseFeeScalar + l1BlobBaseFee*l1BlobBaseFeeScalar)/1e6
    //
    // We divide "calldataGas" by 16 to change from units of calldata gas to "estimated # of bytes when
    // compressed". Known as "compressedTxSize" in the spec.
    //
    // Function is actually computed as follows for better precision under integer arithmetic:
    //
    //   calldataGas*(l1BaseFee*16*l1BaseFeeScalar + l1BlobBaseFee*l1BlobBaseFeeScalar)/16e6
    var calldataCostPerByte = l1BaseFee.multiply(SIXTEEN).multiply(l1BaseFeeScalar);
    var blobCostPerByte = l1BlobBaseFee.multiply(l1BlobBaseFeeScalar);

    // fee = (calldataCostPerByte + blobCostPerByte) * calldataGasUsed / (16e6)
    var fee = calldataCostPerByte
            .add(blobCostPerByte)
        .multiply(calldataGasUsed)
        .divide(ECOTONE_DIVISOR);
    return Wei.of(fee);
  }

  static Wei l1CostFjord(final RollupGasData costData,
                         final UInt256 l1BaseFee,
                         final UInt256 l1BlobBaseFee,
                         final UInt256 l1BaseFeeScalar,
                         final UInt256 l1BlobBaseFeeScalar) {
    // Fjord L1 cost function:
    //l1FeeScaled = baseFeeScalar*l1BaseFee*16 + blobFeeScalar*l1BlobBaseFee
    //estimatedSize = max(minTransactionSize, intercept + fastlzCoef*fastlzSize)
    //l1Cost = estimatedSize * l1FeeScaled / 1e12

    var scaledL1BaseFee = l1BaseFeeScalar.multiply(l1BaseFee);
    var calldataCostPerByte = scaledL1BaseFee.multiply(SIXTEEN);
    var blobCostPerByte = l1BlobBaseFeeScalar.multiply(l1BlobBaseFee);
    var l1FeeScaled = calldataCostPerByte.add(blobCostPerByte);

    var fastLzSize = UInt256.valueOf(costData.getFastLzSize());
    var estimatedSize = UInt256.valueOf(L1_COST_INTERCEPT
        .add(L1_COST_FAST_LZ_COEF.multiply(fastLzSize).toBigInteger()));

    if (estimatedSize.compareTo(MIN_TX_SIZE_SCALED) < 0) {
      estimatedSize = MIN_TX_SIZE_SCALED;
    }

    var l1CostScaled = estimatedSize.multiply(l1FeeScaled);
    var l1Cost = l1CostScaled.divide(FJORD_DIVISOR);
    return Wei.of(l1Cost);
  }

  /**
   * calculates rollup data gas cost.
   *
   * @param rollupGasData rollup gas data record
   * @param isRegolith flag that transaction time is bigger than regolith time
   * @return the transaction gas value
   */
  public static long calculateRollupDataGasCost(
      final RollupGasData rollupGasData, final boolean isRegolith) {
    var gas = rollupGasData.getZeroes() * TX_DATA_ZERO_COST;
    if (isRegolith) {
      gas += rollupGasData.getOnes() * TX_DATA_NON_ZERO_GAS_EIP2028_COST;
    } else {
      gas +=
          (rollupGasData.getOnes() + TX_DATA_NON_ZERO_GAS_FRONTIER_COST)
              * TX_DATA_NON_ZERO_GAS_EIP2028_COST;
    }
    return gas;
  }
}
