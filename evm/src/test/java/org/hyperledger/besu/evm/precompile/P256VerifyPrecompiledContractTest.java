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
package org.hyperledger.besu.evm.precompile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.hyperledger.besu.evm.frame.MessageFrame;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class P256VerifyPrecompiledContractTest {

  private static P256VerifyPrecompiledContract contract;
  private final MessageFrame messageFrame = mock(MessageFrame.class);

  @BeforeAll
  public static void init() {
    contract = new P256VerifyPrecompiledContract();
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("getP256VerifyTestVectors")
  void testComputePrecompile(final PrecompileTestParameters parameters) {
    PrecompiledContract.PrecompileContractResult result =
        contract.computePrecompile(parameters.input(), messageFrame);

    assertThat(result.getOutput()).isEqualTo(parameters.expected());
  }

  public static List<PrecompileTestParameters> getP256VerifyTestVectors() throws IOException {
    final JsonNode jsonNode;
    try (final InputStream testVectors =
        P256VerifyPrecompiledContractTest.class.getResourceAsStream("p256Verify.json")) {
      jsonNode = new ObjectMapper().readTree(testVectors);
    }
    final ArrayNode testCases = (ArrayNode) jsonNode;
    return IntStream.range(0, testCases.size())
        .mapToObj(
            i -> {
              final JsonNode testCase = testCases.get(i);
              final Bytes input = Bytes.fromHexString(testCase.get("Input").asText());
              final Bytes expected = Bytes.fromHexString(testCase.get("Expected").asText());
              final long gasRequire = testCase.get("Gas").asLong();
              final String name = testCase.get("Name").asText();
              return new PrecompileTestParameters(input, expected, gasRequire, name);
            })
        .collect(Collectors.toList());
  }

  record PrecompileTestParameters(Bytes input, Bytes expected, long gasRequire, String name) {}
}
