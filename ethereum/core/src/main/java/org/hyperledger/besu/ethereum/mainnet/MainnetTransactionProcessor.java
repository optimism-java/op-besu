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
package org.hyperledger.besu.ethereum.mainnet;

import static org.hyperledger.besu.ethereum.mainnet.PrivateStateUtils.KEY_IS_PERSISTING_PRIVATE_STATE;
import static org.hyperledger.besu.ethereum.mainnet.PrivateStateUtils.KEY_PRIVATE_METADATA_UPDATER;
import static org.hyperledger.besu.ethereum.mainnet.PrivateStateUtils.KEY_TRANSACTION;
import static org.hyperledger.besu.ethereum.mainnet.PrivateStateUtils.KEY_TRANSACTION_HASH;

import org.hyperledger.besu.config.GenesisConfigOptions;
import org.hyperledger.besu.datatypes.AccessListEntry;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.TransactionType;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.core.ProcessableBlockHeader;
import org.hyperledger.besu.ethereum.core.Transaction;
import org.hyperledger.besu.ethereum.core.feemarket.CoinbaseFeePriceCalculator;
import org.hyperledger.besu.ethereum.mainnet.feemarket.FeeMarket;
import org.hyperledger.besu.ethereum.privacy.storage.PrivateMetadataUpdater;
import org.hyperledger.besu.ethereum.processing.TransactionProcessingResult;
import org.hyperledger.besu.ethereum.transaction.TransactionInvalidReason;
import org.hyperledger.besu.ethereum.trie.MerkleTrieException;
import org.hyperledger.besu.ethereum.vm.BlockHashLookup;
import org.hyperledger.besu.evm.account.Account;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.code.CodeV0;
import org.hyperledger.besu.evm.frame.ExceptionalHaltReason;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.gascalculator.GasCalculator;
import org.hyperledger.besu.evm.processor.AbstractMessageProcessor;
import org.hyperledger.besu.evm.tracing.OperationTracer;
import org.hyperledger.besu.evm.worldstate.WorldUpdater;

import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainnetTransactionProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(MainnetTransactionProcessor.class);

  protected final GasCalculator gasCalculator;

  protected final TransactionValidatorFactory transactionValidatorFactory;

  private final AbstractMessageProcessor contractCreationProcessor;

  private final AbstractMessageProcessor messageCallProcessor;

  private final int maxStackSize;

  private final boolean clearEmptyAccounts;

  protected final boolean warmCoinbase;

  protected final FeeMarket feeMarket;
  private final CoinbaseFeePriceCalculator coinbaseFeePriceCalculator;

  private final Optional<GenesisConfigOptions> genesisConfigOptions;

  private final Optional<L1CostCalculator> l1CostCalculator;

  public MainnetTransactionProcessor(
      final GasCalculator gasCalculator,
      final TransactionValidatorFactory transactionValidatorFactory,
      final AbstractMessageProcessor contractCreationProcessor,
      final AbstractMessageProcessor messageCallProcessor,
      final boolean clearEmptyAccounts,
      final boolean warmCoinbase,
      final int maxStackSize,
      final FeeMarket feeMarket,
      final CoinbaseFeePriceCalculator coinbaseFeePriceCalculator) {
    this.gasCalculator = gasCalculator;
    this.transactionValidatorFactory = transactionValidatorFactory;
    this.contractCreationProcessor = contractCreationProcessor;
    this.messageCallProcessor = messageCallProcessor;
    this.clearEmptyAccounts = clearEmptyAccounts;
    this.warmCoinbase = warmCoinbase;
    this.maxStackSize = maxStackSize;
    this.feeMarket = feeMarket;
    this.coinbaseFeePriceCalculator = coinbaseFeePriceCalculator;
    this.genesisConfigOptions = Optional.empty();
    this.l1CostCalculator = Optional.empty();
  }

  public MainnetTransactionProcessor(
      final GasCalculator gasCalculator,
      final TransactionValidatorFactory transactionValidatorFactory,
      final AbstractMessageProcessor contractCreationProcessor,
      final AbstractMessageProcessor messageCallProcessor,
      final boolean clearEmptyAccounts,
      final boolean warmCoinbase,
      final int maxStackSize,
      final FeeMarket feeMarket,
      final CoinbaseFeePriceCalculator coinbaseFeePriceCalculator,
      final Optional<GenesisConfigOptions> genesisConfigOptions,
      final Optional<L1CostCalculator> l1CostCalculator) {
    this.gasCalculator = gasCalculator;
    this.transactionValidatorFactory = transactionValidatorFactory;
    this.contractCreationProcessor = contractCreationProcessor;
    this.messageCallProcessor = messageCallProcessor;
    this.clearEmptyAccounts = clearEmptyAccounts;
    this.warmCoinbase = warmCoinbase;
    this.maxStackSize = maxStackSize;
    this.feeMarket = feeMarket;
    this.coinbaseFeePriceCalculator = coinbaseFeePriceCalculator;
    this.genesisConfigOptions = genesisConfigOptions;
    this.l1CostCalculator = l1CostCalculator;
  }

  /**
   * Applies a transaction to the current system state.
   *
   * @param blockchain The current blockchain
   * @param worldState The current world state
   * @param blockHeader The current block header
   * @param transaction The transaction to process
   * @param miningBeneficiary The address which is to receive the transaction fee
   * @param blockHashLookup The {@link BlockHashLookup} to use for BLOCKHASH operations
   * @param isPersistingPrivateState Whether the resulting private state will be persisted
   * @param transactionValidationParams Validation parameters that will be used by the {@link
   *     MainnetTransactionValidator}
   * @return the transaction result
   * @see MainnetTransactionValidator
   * @see TransactionValidationParams
   */
  public TransactionProcessingResult processTransaction(
      final Blockchain blockchain,
      final WorldUpdater worldState,
      final ProcessableBlockHeader blockHeader,
      final Transaction transaction,
      final Address miningBeneficiary,
      final BlockHashLookup blockHashLookup,
      final Boolean isPersistingPrivateState,
      final TransactionValidationParams transactionValidationParams,
      final Wei blobGasPrice) {
    return processTransaction(
        blockchain,
        worldState,
        blockHeader,
        transaction,
        miningBeneficiary,
        OperationTracer.NO_TRACING,
        blockHashLookup,
        isPersistingPrivateState,
        transactionValidationParams,
        null,
        blobGasPrice);
  }

  /**
   * Applies a transaction to the current system state.
   *
   * @param blockchain The current blockchain
   * @param worldState The current world state
   * @param blockHeader The current block header
   * @param transaction The transaction to process
   * @param miningBeneficiary The address which is to receive the transaction fee
   * @param blockHashLookup The {@link BlockHashLookup} to use for BLOCKHASH operations
   * @param isPersistingPrivateState Whether the resulting private state will be persisted
   * @param transactionValidationParams Validation parameters that will be used by the {@link
   *     MainnetTransactionValidator}
   * @param operationTracer operation tracer {@link OperationTracer}
   * @return the transaction result
   * @see MainnetTransactionValidator
   * @see TransactionValidationParams
   */
  public TransactionProcessingResult processTransaction(
      final Blockchain blockchain,
      final WorldUpdater worldState,
      final ProcessableBlockHeader blockHeader,
      final Transaction transaction,
      final Address miningBeneficiary,
      final BlockHashLookup blockHashLookup,
      final Boolean isPersistingPrivateState,
      final TransactionValidationParams transactionValidationParams,
      final OperationTracer operationTracer,
      final Wei blobGasPrice) {
    return processTransaction(
        blockchain,
        worldState,
        blockHeader,
        transaction,
        miningBeneficiary,
        operationTracer,
        blockHashLookup,
        isPersistingPrivateState,
        transactionValidationParams,
        null,
        blobGasPrice);
  }

  /**
   * Applies a transaction to the current system state.
   *
   * @param blockchain The current blockchain
   * @param worldState The current world state
   * @param blockHeader The current block header
   * @param transaction The transaction to process
   * @param operationTracer The tracer to record results of each EVM operation
   * @param miningBeneficiary The address which is to receive the transaction fee
   * @param blockHashLookup The {@link BlockHashLookup} to use for BLOCKHASH operations
   * @param isPersistingPrivateState Whether the resulting private state will be persisted
   * @return the transaction result
   */
  public TransactionProcessingResult processTransaction(
      final Blockchain blockchain,
      final WorldUpdater worldState,
      final ProcessableBlockHeader blockHeader,
      final Transaction transaction,
      final Address miningBeneficiary,
      final OperationTracer operationTracer,
      final BlockHashLookup blockHashLookup,
      final Boolean isPersistingPrivateState,
      final Wei blobGasPrice) {
    return processTransaction(
        blockchain,
        worldState,
        blockHeader,
        transaction,
        miningBeneficiary,
        operationTracer,
        blockHashLookup,
        isPersistingPrivateState,
        ImmutableTransactionValidationParams.builder().build(),
        null,
        blobGasPrice);
  }

  /**
   * Applies a transaction to the current system state.
   *
   * @param blockchain The current blockchain
   * @param worldState The current world state
   * @param blockHeader The current block header
   * @param transaction The transaction to process
   * @param operationTracer The tracer to record results of each EVM operation
   * @param miningBeneficiary The address which is to receive the transaction fee
   * @param blockHashLookup The {@link BlockHashLookup} to use for BLOCKHASH operations
   * @param isPersistingPrivateState Whether the resulting private state will be persisted
   * @param transactionValidationParams The transaction validation parameters to use
   * @return the transaction result
   */
  public TransactionProcessingResult processTransaction(
      final Blockchain blockchain,
      final WorldUpdater worldState,
      final ProcessableBlockHeader blockHeader,
      final Transaction transaction,
      final Address miningBeneficiary,
      final OperationTracer operationTracer,
      final BlockHashLookup blockHashLookup,
      final Boolean isPersistingPrivateState,
      final TransactionValidationParams transactionValidationParams,
      final Wei blobGasPrice) {
    return processTransaction(
        blockchain,
        worldState,
        blockHeader,
        transaction,
        miningBeneficiary,
        operationTracer,
        blockHashLookup,
        isPersistingPrivateState,
        transactionValidationParams,
        null,
        blobGasPrice);
  }

  public TransactionProcessingResult processTransaction(
      final Blockchain ignoredBlockchain,
      final WorldUpdater worldState,
      final ProcessableBlockHeader blockHeader,
      final Transaction transaction,
      final Address miningBeneficiary,
      final OperationTracer operationTracer,
      final BlockHashLookup blockHashLookup,
      final Boolean isPersistingPrivateState,
      final TransactionValidationParams transactionValidationParams,
      final PrivateMetadataUpdater privateMetadataUpdater,
      final Wei blobGasPrice) {
    try {
      transaction
          .getMint()
          .ifPresent(
              mint -> {
                WorldUpdater mintUpdater = worldState.updater();
                final MutableAccount sender =
                    mintUpdater.getOrCreateSenderAccount(transaction.getSender());
                sender.incrementBalance(transaction.getMint().orElse(Wei.ZERO));
                mintUpdater.commit();
              });
      final var transactionValidator = transactionValidatorFactory.get();
      LOG.trace("Starting execution of {}", transaction);
      ValidationResult<TransactionInvalidReason> validationResult =
          transactionValidator.validate(
              transaction,
              blockHeader.getTimestamp(),
              blockHeader.getBaseFee(),
              transactionValidationParams);
      // Make sure the transaction is intrinsically valid before trying to
      // compare against a sender account (because the transaction may not
      // be signed correctly to extract the sender).
      if (!validationResult.isValid()) {
        LOG.debug("Invalid transaction: {}", validationResult.getErrorMessage());
        return TransactionProcessingResult.invalid(validationResult);
      }

      final Address senderAddress = transaction.getSender();

      final MutableAccount sender = worldState.getOrCreateSenderAccount(senderAddress);

      validationResult =
          transactionValidator.validateForSender(transaction, sender, transactionValidationParams);
      if (!validationResult.isValid()) {
        LOG.debug("Invalid transaction: {}", validationResult.getErrorMessage());
        return TransactionProcessingResult.invalid(validationResult);
      }

      final long previousNonce = sender.incrementNonce();
      LOG.trace(
          "Incremented sender {} nonce ({} -> {})",
          senderAddress,
          previousNonce,
          sender.getNonce());

      Wei transactionGasPrice;
      if (!TransactionType.OPTIMISM_DEPOSIT.equals(transaction.getType())) {
        transactionGasPrice =
            feeMarket.getTransactionPriceCalculator().price(transaction, blockHeader.getBaseFee());

        final long blobGas = gasCalculator.blobGasCost(transaction.getBlobCount());

        final Wei upfrontGasCost =
            transaction.getUpfrontGasCost(transactionGasPrice, blobGasPrice, blobGas);
        final Wei previousBalance = sender.decrementBalance(upfrontGasCost);
        LOG.trace(
            "Deducted sender {} upfront gas cost {} ({} -> {})",
            senderAddress,
            upfrontGasCost,
            previousBalance,
            sender.getBalance());
      } else {
        transactionGasPrice = Wei.ZERO;
      }

      genesisConfigOptions.ifPresent(
          options -> {
            if (!options.isOptimism()
                || !options.isRegolith(blockHeader.getTimestamp())
                || TransactionType.OPTIMISM_DEPOSIT.equals(transaction.getType())) {
              return;
            }
            final long l1Cost =
                l1CostCalculator
                    .map(
                        l1CostCalculator ->
                            l1CostCalculator.l1Cost(
                                genesisConfigOptions.get(), blockHeader, transaction, worldState))
                    .orElse(0L);
            sender.decrementBalance(Wei.of(l1Cost));
          });

      final List<AccessListEntry> accessListEntries = transaction.getAccessList().orElse(List.of());
      // we need to keep a separate hash set of addresses in case they specify no storage.
      // No-storage is a common pattern, especially for Externally Owned Accounts
      final Set<Address> addressList = new HashSet<>();
      final Multimap<Address, Bytes32> storageList = HashMultimap.create();
      int accessListStorageCount = 0;
      for (final var entry : accessListEntries) {
        final Address address = entry.address();
        addressList.add(address);
        final List<Bytes32> storageKeys = entry.storageKeys();
        storageList.putAll(address, storageKeys);
        accessListStorageCount += storageKeys.size();
      }
      if (warmCoinbase) {
        addressList.add(miningBeneficiary);
      }

      final long intrinsicGas =
          gasCalculator.transactionIntrinsicGasCost(
              transaction.getPayload(), transaction.isContractCreation());
      final long accessListGas =
          gasCalculator.accessListGasCost(accessListEntries.size(), accessListStorageCount);
      final long gasAvailable = transaction.getGasLimit() - intrinsicGas - accessListGas;
      LOG.trace(
          "Gas available for execution {} = {} - {} - {} (limit - intrinsic - accessList)",
          gasAvailable,
          transaction.getGasLimit(),
          intrinsicGas,
          accessListGas);

      final WorldUpdater worldUpdater = worldState.updater();
      final ImmutableMap.Builder<String, Object> contextVariablesBuilder =
          ImmutableMap.<String, Object>builder()
              .put(KEY_IS_PERSISTING_PRIVATE_STATE, isPersistingPrivateState)
              .put(KEY_TRANSACTION, transaction)
              .put(KEY_TRANSACTION_HASH, transaction.getHash());
      if (privateMetadataUpdater != null) {
        contextVariablesBuilder.put(KEY_PRIVATE_METADATA_UPDATER, privateMetadataUpdater);
      }

      final MessageFrame.Builder commonMessageFrameBuilder =
          MessageFrame.builder()
              .maxStackSize(maxStackSize)
              .worldUpdater(worldUpdater.updater())
              .initialGas(gasAvailable)
              .originator(senderAddress)
              .gasPrice(transactionGasPrice)
              .blobGasPrice(blobGasPrice)
              .sender(senderAddress)
              .value(transaction.getValue())
              .apparentValue(transaction.getValue())
              .blockValues(blockHeader)
              .completer(__ -> {})
              .miningBeneficiary(miningBeneficiary)
              .blockHashLookup(blockHashLookup)
              .contextVariables(contextVariablesBuilder.build())
              .accessListWarmAddresses(addressList)
              .accessListWarmStorage(storageList)
              .isDepositTx(TransactionType.OPTIMISM_DEPOSIT.equals(transaction.getType()))
              .isSystemTx(transaction.getIsSystemTx().orElse(false))
              .mint(transaction.getMint());

      if (transaction.getVersionedHashes().isPresent()) {
        commonMessageFrameBuilder.versionedHashes(
            Optional.of(transaction.getVersionedHashes().get().stream().toList()));
      } else {
        commonMessageFrameBuilder.versionedHashes(Optional.empty());
      }

      final MessageFrame initialFrame;
      if (transaction.isContractCreation()) {
        final Address contractAddress =
            Address.contractAddress(senderAddress, sender.getNonce() - 1L);

        final Bytes initCodeBytes = transaction.getPayload();
        initialFrame =
            commonMessageFrameBuilder
                .type(MessageFrame.Type.CONTRACT_CREATION)
                .address(contractAddress)
                .contract(contractAddress)
                .inputData(Bytes.EMPTY)
                .code(contractCreationProcessor.getCodeFromEVM(null, initCodeBytes))
                .build();
      } else {
        @SuppressWarnings("OptionalGetWithoutIsPresent") // isContractCall tests isPresent
        final Address to = transaction.getTo().get();
        final Optional<Account> maybeContract = Optional.ofNullable(worldState.get(to));
        initialFrame =
            commonMessageFrameBuilder
                .type(MessageFrame.Type.MESSAGE_CALL)
                .address(to)
                .contract(to)
                .inputData(transaction.getPayload())
                .code(
                    maybeContract
                        .map(c -> messageCallProcessor.getCodeFromEVM(c.getCodeHash(), c.getCode()))
                        .orElse(CodeV0.EMPTY_CODE))
                .build();
      }
      Deque<MessageFrame> messageFrameStack = initialFrame.getMessageFrameStack();

      if (initialFrame.getCode().isValid()) {
        while (!messageFrameStack.isEmpty()) {
          process(messageFrameStack.peekFirst(), operationTracer);
        }
      } else {
        initialFrame.setState(MessageFrame.State.EXCEPTIONAL_HALT);
        initialFrame.setExceptionalHaltReason(Optional.of(ExceptionalHaltReason.INVALID_CODE));
      }

      if (initialFrame.getState() == MessageFrame.State.COMPLETED_SUCCESS) {
        worldUpdater.commit();
      }

      if (LOG.isTraceEnabled()) {
        LOG.trace(
            "Gas used by transaction: {}, by message call/contract creation: {}",
            transaction.getGasLimit() - initialFrame.getRemainingGas(),
            gasAvailable - initialFrame.getRemainingGas());
      }

      boolean isRegolith =
          genesisConfigOptions
              .map(options -> options.isRegolith(blockHeader.getTimestamp()))
              .orElse(false);

      // if deposit: skip refunds, skip tipping coinbase
      // Regolith changes this behaviour to report the actual gasUsed instead of always reporting
      // all gas used.
      if (initialFrame.isDepositTx() && !isRegolith) {
        var gasUsed = transaction.getGasLimit();
        if (initialFrame.isSystemTx()) {
          gasUsed = 0L;
        }
        if (initialFrame.getState() == MessageFrame.State.COMPLETED_SUCCESS) {
          return TransactionProcessingResult.successful(
              initialFrame.getLogs(), gasUsed, 0L, initialFrame.getOutputData(), validationResult);
        } else {
          return TransactionProcessingResult.failed(
              gasUsed, 0L, validationResult, initialFrame.getRevertReason());
        }
      }

      // Refund the sender by what we should and pay the miner fee (note that we're doing them one
      // after the other so that if it is the same account somehow, we end up with the right result)
      final long selfDestructRefund =
          gasCalculator.getSelfDestructRefundAmount() * initialFrame.getSelfDestructs().size();
      final long baseRefundGas = initialFrame.getGasRefund() + selfDestructRefund;
      final long refundedGas = refunded(transaction, initialFrame, baseRefundGas);
      final Wei refundedWei = transactionGasPrice.multiply(refundedGas);
      final Wei balancePriorToRefund = sender.getBalance();
      sender.incrementBalance(refundedWei);
      LOG.atTrace()
          .setMessage("refunded sender {}  {} wei ({} -> {})")
          .addArgument(senderAddress)
          .addArgument(refundedWei)
          .addArgument(balancePriorToRefund)
          .addArgument(sender.getBalance())
          .log();

      final long gasUsedByTransaction = transaction.getGasLimit() - initialFrame.getRemainingGas();

      // Skip coinbase payments for deposit tx in Regolith
      if (initialFrame.isDepositTx() && isRegolith) {
        if (initialFrame.getState() == MessageFrame.State.COMPLETED_SUCCESS) {
          return TransactionProcessingResult.successful(
              initialFrame.getLogs(),
              gasUsedByTransaction,
              refundedGas,
              initialFrame.getOutputData(),
              validationResult);
        } else {
          return TransactionProcessingResult.failed(
              gasUsedByTransaction, refundedGas, validationResult, initialFrame.getRevertReason());
        }
      }

      // update the coinbase
      final var coinbase = worldState.getOrCreate(miningBeneficiary);
      final long usedGas = transaction.getGasLimit() - refundedGas;
      final CoinbaseFeePriceCalculator coinbaseCalculator;
      if (blockHeader.getBaseFee().isPresent()) {
        final Wei baseFee = blockHeader.getBaseFee().get();
        if (transactionGasPrice.compareTo(baseFee) < 0) {
          return TransactionProcessingResult.failed(
              gasUsedByTransaction,
              refundedGas,
              ValidationResult.invalid(
                  TransactionInvalidReason.TRANSACTION_PRICE_TOO_LOW,
                  "transaction price must be greater than base fee"),
              Optional.empty());
        }
        coinbaseCalculator = coinbaseFeePriceCalculator;
      } else {
        coinbaseCalculator = CoinbaseFeePriceCalculator.frontier();
      }

      final Wei coinbaseWeiDelta =
          coinbaseCalculator.price(usedGas, transactionGasPrice, blockHeader.getBaseFee());

      coinbase.incrementBalance(coinbaseWeiDelta);

      initialFrame.getSelfDestructs().forEach(worldState::deleteAccount);

      if (clearEmptyAccounts) {
        worldState.clearAccountsThatAreEmpty();
      }

      // Check that we are post bedrock to enable op-geth to be able to create pseudo pre-bedrock
      // blocks (these are pre-bedrock, but don't follow l2 geth rules)
      // Note optimismConfig will not be nil if rules.IsOptimismBedrock is true
      genesisConfigOptions.ifPresent(
          options -> {
            if (!options.isBedrockBlock(blockHeader.getNumber())) {
              return;
            }
            MutableAccount opBaseFeeRecipient =
                worldState.getOrCreate(
                    Address.fromHexString("0x4200000000000000000000000000000000000019"));
            opBaseFeeRecipient.incrementBalance(
                blockHeader.getBaseFee().get().multiply(gasUsedByTransaction));

            l1CostCalculator.ifPresent(
                costCal -> {
                  final long l1Cost = costCal.l1Cost(options, blockHeader, transaction, worldState);
                  MutableAccount opL1FeeRecipient =
                      worldState.getOrCreate(
                          Address.fromHexString("0x420000000000000000000000000000000000001A"));
                  opL1FeeRecipient.incrementBalance(Wei.of(l1Cost));
                });
          });

      if (initialFrame.getState() == MessageFrame.State.COMPLETED_SUCCESS) {
        return TransactionProcessingResult.successful(
            initialFrame.getLogs(),
            gasUsedByTransaction,
            refundedGas,
            initialFrame.getOutputData(),
            validationResult);
      } else {
        return TransactionProcessingResult.failed(
            gasUsedByTransaction, refundedGas, validationResult, initialFrame.getRevertReason());
      }
    } catch (final MerkleTrieException re) {
      // need to throw to trigger the heal
      throw re;
    } catch (final RuntimeException re) {
      if (TransactionType.OPTIMISM_DEPOSIT.equals(transaction.getType())) {
        worldState.revert();
        worldState.getAccount(transaction.getSender()).incrementNonce();
        var gasUsed = transaction.getGasLimit();
        if (transaction.getIsSystemTx().get()
            && genesisConfigOptions.get().isRegolith(blockHeader.getTimestamp())) {
          gasUsed = 0L;
        }
        final String msg = String.format("failed deposit: %s", re);
        return TransactionProcessingResult.failed(
            gasUsed,
            0L,
            ValidationResult.valid(),
            Optional.of(Bytes.wrap(msg.getBytes(StandardCharsets.UTF_8))));
      }
      LOG.error("Critical Exception Processing Transaction", re);
      return TransactionProcessingResult.invalid(
          ValidationResult.invalid(
              TransactionInvalidReason.INTERNAL_ERROR, "Internal Error in Besu - " + re));
    }
  }

  public void process(final MessageFrame frame, final OperationTracer operationTracer) {
    final AbstractMessageProcessor executor = getMessageProcessor(frame.getType());

    executor.process(frame, operationTracer);
  }

  private AbstractMessageProcessor getMessageProcessor(final MessageFrame.Type type) {
    return switch (type) {
      case MESSAGE_CALL -> messageCallProcessor;
      case CONTRACT_CREATION -> contractCreationProcessor;
    };
  }

  protected long refunded(
      final Transaction transaction, final MessageFrame initialFrame, final long gasRefund) {
    // Integer truncation takes care of the floor calculation needed after the divide.
    final long gasRemaining = initialFrame.getRemainingGas();
    final long maxRefundAllowance =
        (transaction.getGasLimit() - gasRemaining) / gasCalculator.getMaxRefundQuotient();
    final long refundAllowance = Math.min(maxRefundAllowance, gasRefund);
    initialFrame.incrementRemainingGas(refundAllowance);
    return gasRemaining + refundAllowance;
  }
}
