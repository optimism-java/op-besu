/*
 * Copyright ConsenSys AG.
 * Copyright OptimismJ.
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
package org.hyperledger.besu.config;

import java.util.Map;
import java.util.OptionalLong;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;

/** the Optimism config options. */
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
        return JsonUtil.getLong(optimismConfigRoot, "eip1559elasticity");
    }

    /**
     * Gets EIP1559 denominator.
     *
     * @return the EIP1559 denominator
     */
    public OptionalLong getEIP1559Denominator() {
        return JsonUtil.getLong(optimismConfigRoot, "eip1559denominator");
    }

    /**
     * Gets EIP1559 denominatorCanyon.
     *
     * @return the EIP1559 denominatorCanyon
     */
    public OptionalLong getEIP1559DenominatorCanyon() {
        return JsonUtil.getLong(optimismConfigRoot, "eip1559denominatorcanyon");
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
