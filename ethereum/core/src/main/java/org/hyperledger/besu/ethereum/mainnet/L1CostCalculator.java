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

/** L1 Cost calculator. */
public class L1CostCalculator {

  private static final long TX_DATA_ZERO_COST = 4L;
  private static final long TX_DATA_NON_ZERO_GAS_EIP2028_COST = 16L;
  private static final long TX_DATA_NON_ZERO_GAS_FRONTIER_COST = 68L;

  private static final Address l1BlockAddr =
      Address.fromHexString("0x4200000000000000000000000000000000000015");
  private static final UInt256 l1BaseFeeSlot = UInt256.valueOf(1L);
  private static final UInt256 overheadSlot = UInt256.valueOf(5L);
  private static final UInt256 scalarSlot = UInt256.valueOf(6L);
  private long cachedBlock;
  private UInt256 l1FBaseFee;
  private UInt256 overhead;
  private UInt256 scalar;

  public L1CostCalculator() {
    this.cachedBlock = 0L;
    this.l1FBaseFee = UInt256.ZERO;
    this.overhead = UInt256.ZERO;
    this.scalar = UInt256.ZERO;
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
    long gas = 0;
    boolean isRegolith = options.isRegolith(blockHeader.getTimestamp());

    gas += calculateRollupDataGasCost(transaction.getRollupGasData(), isRegolith);

    boolean isOptimism = options.isOptimism();
    boolean isDepositTx = TransactionType.OPTIMISM_DEPOSIT.equals(transaction.getType());
    if (!isOptimism || isDepositTx || gas == 0) {
      return Wei.ZERO;
    }
    if (blockHeader.getNumber() != cachedBlock) {
      MutableAccount systemConfig = worldState.getOrCreate(l1BlockAddr);
      l1FBaseFee = systemConfig.getStorageValue(l1BaseFeeSlot);
      overhead = systemConfig.getStorageValue(overheadSlot);
      scalar = systemConfig.getStorageValue(scalarSlot);
      cachedBlock = blockHeader.getNumber();
    }
    UInt256 l1GasUsed =
        UInt256.valueOf(gas)
            .add(overhead)
            .multiply(l1FBaseFee)
            .multiply(scalar)
            .divide(UInt256.valueOf(1_000_000L));

    return Wei.of(l1GasUsed);
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
