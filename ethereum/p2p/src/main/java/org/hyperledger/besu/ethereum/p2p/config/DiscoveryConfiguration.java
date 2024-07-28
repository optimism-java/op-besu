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
package org.hyperledger.besu.ethereum.p2p.config;

import org.hyperledger.besu.plugin.data.EnodeURL;
import org.hyperledger.besu.util.NetworkUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DiscoveryConfiguration {

  public static final List<String> V5_OP_BOOTNODES =
      List.<String>of(
          // OP Labs
          "enode://ca2774c3c401325850b2477fd7d0f27911efbf79b1e8b335066516e2bd8c4c9e0ba9696a94b1cb030a88eac582305ff55e905e64fb77fe0edcd70a4e5296d3ec@34.65.175.185:30305",
          "enode://dd751a9ef8912be1bfa7a5e34e2c3785cc5253110bd929f385e07ba7ac19929fb0e0c5d93f77827291f4da02b2232240fbc47ea7ce04c46e333e452f8656b667@34.65.107.0:30305",
          "enode://c5d289b56a77b6a2342ca29956dfd07aadf45364dde8ab20d1dc4efd4d1bc6b4655d902501daea308f4d8950737a4e93a4dfedd17b49cd5760ffd127837ca965@34.65.202.239:30305",
          // Base
          "enode://87a32fd13bd596b2ffca97020e31aef4ddcc1bbd4b95bb633d16c1329f654f34049ed240a36b449fda5e5225d70fe40bc667f53c304b71f8e68fc9d448690b51@3.231.138.188:30301",
          "enode://ca21ea8f176adb2e229ce2d700830c844af0ea941a1d8152a9513b966fe525e809c3a6c73a2c18a12b74ed6ec4380edf91662778fe0b79f6a591236e49e176f9@184.72.129.189:30301",
          "enode://acf4507a211ba7c1e52cdf4eef62cdc3c32e7c9c47998954f7ba024026f9a6b2150cd3f0b734d9c78e507ab70d59ba61dfe5c45e1078c7ad0775fb251d7735a2@3.220.145.177:30301",
          "enode://8a5a5006159bf079d06a04e5eceab2a1ce6e0f721875b2a9c96905336219dbe14203d38f70f3754686a6324f786c2f9852d8c0dd3adac2d080f4db35efc678c5@3.231.11.52:30301",
          "enode://cdadbe835308ad3557f9a1de8db411da1a260a98f8421d62da90e71da66e55e98aaa8e90aa7ce01b408a54e4bd2253d701218081ded3dbe5efbbc7b41d7cef79@54.198.153.150:30301");

  public static final List<String> V5_OP_TESTNET_BOOTNODES =
      List.<String>of(
          // OP Labs
          "enode://2bd2e657bb3c8efffb8ff6db9071d9eb7be70d7c6d7d980ff80fc93b2629675c5f750bc0a5ef27cd788c2e491b8795a7e9a4a6e72178c14acc6753c0e5d77ae4@34.65.205.244:30305",
          "enode://db8e1cab24624cc62fc35dbb9e481b88a9ef0116114cd6e41034c55b5b4f18755983819252333509bd8e25f6b12aadd6465710cd2e956558faf17672cce7551f@34.65.173.88:30305",
          "enode://bfda2e0110cfd0f4c9f7aa5bf5ec66e6bd18f71a2db028d36b8bf8b0d6fdb03125c1606a6017b31311d96a36f5ef7e1ad11604d7a166745e6075a715dfa67f8a@34.65.229.245:30305",
          // Base
          "enode://548f715f3fc388a7c917ba644a2f16270f1ede48a5d88a4d14ea287cc916068363f3092e39936f1a3e7885198bef0e5af951f1d7b1041ce8ba4010917777e71f@18.210.176.114:30301",
          "enode://6f10052847a966a725c9f4adf6716f9141155b99a0fb487fea3f51498f4c2a2cb8d534e680ee678f9447db85b93ff7c74562762c3714783a7233ac448603b25f@107.21.251.55:30301");

  private boolean active = true;
  private String bindHost = NetworkUtility.INADDR_ANY;
  private int bindPort = 30303;
  private String advertisedHost = "127.0.0.1";
  private int bucketSize = 16;
  private List<EnodeURL> bootnodes = new ArrayList<>();
  private String dnsDiscoveryURL;
  private boolean discoveryV5Enabled = false;
  private boolean filterOnEnrForkId = NetworkingConfiguration.DEFAULT_FILTER_ON_ENR_FORK_ID;

  public static DiscoveryConfiguration create() {
    return new DiscoveryConfiguration();
  }

  public static void assertValidBootnodes(final List<EnodeURL> bootnodes) {
    final List<EnodeURL> invalidEnodes =
        bootnodes.stream().filter(e -> !e.isRunningDiscovery()).collect(Collectors.toList());

    if (invalidEnodes.size() > 0) {
      final String invalidBootnodes =
          invalidEnodes.stream().map(EnodeURL::toString).collect(Collectors.joining(","));
      final String errorMsg =
          "Bootnodes must have discovery enabled. Invalid bootnodes: " + invalidBootnodes + ".";
      throw new IllegalArgumentException(errorMsg);
    }
  }

  public String getBindHost() {
    return bindHost;
  }

  public DiscoveryConfiguration setBindHost(final String bindHost) {
    this.bindHost = bindHost;
    return this;
  }

  public int getBindPort() {
    return bindPort;
  }

  public DiscoveryConfiguration setBindPort(final int bindPort) {
    this.bindPort = bindPort;
    return this;
  }

  public boolean isActive() {
    return active;
  }

  public DiscoveryConfiguration setActive(final boolean active) {
    this.active = active;
    return this;
  }

  public List<EnodeURL> getBootnodes() {
    return bootnodes;
  }

  public DiscoveryConfiguration setBootnodes(final List<EnodeURL> bootnodes) {
    assertValidBootnodes(bootnodes);
    this.bootnodes = bootnodes;
    return this;
  }

  public String getAdvertisedHost() {
    return advertisedHost;
  }

  public DiscoveryConfiguration setAdvertisedHost(final String advertisedHost) {
    this.advertisedHost = advertisedHost;
    return this;
  }

  public int getBucketSize() {
    return bucketSize;
  }

  public DiscoveryConfiguration setBucketSize(final int bucketSize) {
    this.bucketSize = bucketSize;
    return this;
  }

  public String getDNSDiscoveryURL() {
    return dnsDiscoveryURL;
  }

  public DiscoveryConfiguration setDnsDiscoveryURL(final String dnsDiscoveryURL) {
    this.dnsDiscoveryURL = dnsDiscoveryURL;
    return this;
  }

  public void setDiscoveryV5Enabled(final boolean discoveryV5Enabled) {
    this.discoveryV5Enabled = discoveryV5Enabled;
  }

  public boolean isDiscoveryV5Enabled() {
    return discoveryV5Enabled;
  }

  public void setFilterOnEnrForkId(final boolean filterOnEnrForkId) {
    this.filterOnEnrForkId = filterOnEnrForkId;
  }

  public boolean isFilterOnEnrForkIdEnabled() {
    return filterOnEnrForkId;
  }

  @Override
  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof DiscoveryConfiguration)) {
      return false;
    }
    final DiscoveryConfiguration that = (DiscoveryConfiguration) o;
    return active == that.active
        && bindPort == that.bindPort
        && bucketSize == that.bucketSize
        && Objects.equals(bindHost, that.bindHost)
        && Objects.equals(advertisedHost, that.advertisedHost)
        && Objects.equals(bootnodes, that.bootnodes)
        && Objects.equals(dnsDiscoveryURL, that.dnsDiscoveryURL);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        active, bindHost, bindPort, advertisedHost, bucketSize, bootnodes, dnsDiscoveryURL);
  }

  @Override
  public String toString() {
    return "DiscoveryConfiguration{"
        + "active="
        + active
        + ", bindHost='"
        + bindHost
        + '\''
        + ", bindPort="
        + bindPort
        + ", advertisedHost='"
        + advertisedHost
        + '\''
        + ", bucketSize="
        + bucketSize
        + ", bootnodes="
        + bootnodes
        + ", dnsDiscoveryURL="
        + dnsDiscoveryURL
        + ", isDiscoveryV5Enabled="
        + discoveryV5Enabled
        + ", isFilterOnEnrForkIdEnabled="
        + filterOnEnrForkId
        + '}';
  }
}
