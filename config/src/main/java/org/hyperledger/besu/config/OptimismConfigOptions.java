package org.hyperledger.besu.config;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.OptionalLong;

/**
 * the Optimism config options.
 */
public class OptimismConfigOptions {

  /** The constant DEFAULT. */
  public static final OptimismConfigOptions DEFAULT =
      new OptimismConfigOptions(JsonUtil.createEmptyObjectNode());

  private final ObjectNode optimismConfigRoot;

  /**
   * Instantiates a new Optimism config options.
   *
   * @param optimismConfigRoot the Optimism config root
   */
  OptimismConfigOptions(final ObjectNode optimismConfigRoot) {
    this.optimismConfigRoot = optimismConfigRoot;
  }

  /**
   * Gets EIP1559 elasticity.
   *
   * @return the EIP1559 elasticity
   */
  public OptionalLong getEIP1559Elasticity() {
    return JsonUtil.getLong(optimismConfigRoot, "eip1559Elasticity");
  }

  /**
   * Gets EIP1559 denominator.
   *
   * @return the EIP1559 denominator
   */
  public OptionalLong getEIP1559Denominator() {
    return JsonUtil.getLong(optimismConfigRoot, "eip1559Denominator");
  }

  /**
   * Gets EIP1559 denominatorCanyon.
   *
   * @return the EIP1559 denominatorCanyon
   */
  public OptionalLong getEIP1559DenominatorCanyon() {
    return JsonUtil.getLong(optimismConfigRoot, "eip1559DenominatorCanyon");
  }

  /**
   * As map.
   *
   * @return the map
   */
  Map<String, Object> asMap() {
    final ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
    getEIP1559Elasticity().ifPresent(l -> builder.put("eip1559Elasticity", l));
    getEIP1559Denominator().ifPresent(l -> builder.put("eip1559Denominator", l));
    getEIP1559DenominatorCanyon().ifPresent(l -> builder.put("eip1559DenominatorCanyon", l));
    return builder.build();
  }

}
