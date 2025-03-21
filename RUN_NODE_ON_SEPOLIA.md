# Run node on Sepolia

1. Run a op-besu

pull docker image:

```shell
docker pull ghcr.io/optimism-java/op-besu:latest
```

create a directory that will be used later:

```shell
mkdir data_sepolia
```

generate a jwt key, it will also be used by hildr:

```shell
openssl rand -hex 32 > jwt.txt
```

run a op-besu by docker:

```shell
docker run -d -it --name op-besu -p 8545:8545 -p 8551:8551 -v ./jwt.txt:/jwt/jwtsecret \
-v ./data_sepolia:/data/ \
ghcr.io/optimism-java/op-besu:latest \
--network=OP_SEPOLIA \
--p2p-enabled=false \
--sync-mode=FULL \
--discovery-enabled=false \
--data-path="/data/" \
--engine-rpc-enabled \
--engine-jwt-secret="/jwt/jwtsecret" \
--rpc-http-enabled \
--host-allowlist="*" \
--engine-host-allowlist="*" \
--logging=INFO \
--version-compatibility-protection=false
```

Sometimes docker could be running under the root user, it may throw some exception logs like below:

```shell
java.io.FileNotFoundException: /data/VERSION_METADATA.json (Permission denied)
        at java.base/java.io.FileOutputStream.open0(Native Method)
        at java.base/java.io.FileOutputStream.open(FileOutputStream.java:289)
        at java.base/java.io.FileOutputStream.<init>(FileOutputStream.java:230)
        at java.base/java.io.FileOutputStream.<init>(FileOutputStream.java:179)
        at com.fasterxml.jackson.core.TokenStreamFactory._fileOutputStream(TokenStreamFactory.java:334)
        at com.fasterxml.jackson.core.JsonFactory.createGenerator(JsonFactory.java:1547)
        at com.fasterxml.jackson.databind.ObjectMapper.createGenerator(ObjectMapper.java:1257)
        at com.fasterxml.jackson.databind.ObjectMapper.writeValue(ObjectMapper.java:3980)
        at org.hyperledger.besu.ethereum.core.VersionMetadata.writeToDirectory(VersionMetadata.java:71)
        at org.hyperledger.besu.ethereum.core.VersionMetadata.versionCompatibilityChecks(VersionMetadata.java:119)
        at org.hyperledger.besu.cli.BesuCommand.run(BesuCommand.java:1114)
        at picocli.CommandLine.executeUserObject(CommandLine.java:2026)
        at picocli.CommandLine.access$1500(CommandLine.java:148)
        at picocli.CommandLine$RunLast.executeUserObjectOfLastSubcommandWithSameParent(CommandLine.java:2461)
        at picocli.CommandLine$RunLast.handle(CommandLine.java:2453)
        at picocli.CommandLine$RunLast.handle(CommandLine.java:2415)
        at picocli.CommandLine$AbstractParseResultHandler.execute(CommandLine.java:2273)
        at picocli.CommandLine$RunLast.execute(CommandLine.java:2417)
        at picocli.CommandLine.execute(CommandLine.java:2170)
        at org.hyperledger.besu.cli.BesuCommand.lambda$createExecuteTask$1(BesuCommand.java:1041)
        at picocli.CommandLine.execute(CommandLine.java:2170)
        at org.hyperledger.besu.cli.BesuCommand.lambda$createPluginRegistrationTask$2(BesuCommand.java:1051)
        at picocli.CommandLine.execute(CommandLine.java:2170)
        at org.hyperledger.besu.cli.util.ConfigDefaultValueProviderStrategy.execute(ConfigDefaultValueProviderStrategy.java:58)
        at picocli.CommandLine.execute(CommandLine.java:2170)
        at org.hyperledger.besu.cli.BesuCommand.executeCommandLine(BesuCommand.java:1078)
        at org.hyperledger.besu.cli.BesuCommand.parse(BesuCommand.java:1020)
        at org.hyperledger.besu.Besu.main(Besu.java:41)
/data/VERSION_METADATA.json (Permission denied)
```

Just need to modify the access permissions of `data_sepolia` to fix it:

```shell
sudo chmod 777 data_sepolia -R
```

2. Run a op-node

pull docker image:

```shell
docker pull us-docker.pkg.dev/oplabs-tools-artifacts/images/op-node:v1.12.2
```

get IP of the op-besu container, and op-node or hildr container will use it to connect to op-besu via the docker bridge:

```bash
docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' op-besu
```

Run a op-node:
```shell
docker run -d -it --name op-node -p 11545:11545 \
-v ./jwt.txt:/jwt/jwt.txt \
--entrypoint op-node \
us-docker.pkg.dev/oplabs-tools-artifacts/images/op-node:v1.12.2 \
--network op-sepolia \
--l1.rpckind=basic \
--l1=<l1-rpc-url> \
--l2=<op-besu-engine-rpc-url> \
--rpc.addr=0.0.0.0 \
--rpc.port=11545 \
--l2.jwt-secret=/jwt/jwt.txt \
--l1.trustrpc \
--l1.beacon=<l1-beacon-sepolia-rpc-url> \
--syncmode=consensus-layer
```

The synchronization needs to handle empty messages at the beginning, and the actual block synchronization will take place about 10 minutes later.

Use curl get block data from op-besu:

```bash
curl --request POST 'http://localhost:8545' \
--header 'Content-Type: application/json' \
--data-raw '{"id":2, "jsonrpc":"2.0", "method": "eth_getBlockByNumber", "params":["0xe", true]}'
```

You can confirm whether the block and transaction information is correct through the [Sepolia network's blockchain explorer](https://sepolia-optimism.etherscan.io/).

3. Or run a hildr

Hildr is just can run on amd64 architecture.

pull docker image:

```shell
docker pull ghcr.io/optimism-java/hildr:latest
```

run a hildr node:

```bash
docker run -d -it --name hildr -p 11545:11545 \
-v ./jwt.txt:/jwt/jwt.txt \
ghcr.io/optimism-java/hildr:latest \
--network optimism-sepolia \
--jwt-file /jwt/jwt.txt \
--l1-rpc-url <l1_sepolia_rpc_url> \
--l1-ws-rpc-url <l1_sepolia_ws_rpc_url> \
--l1-beacon-url <l1_beacon_chain_sepolia_rpc_url> \
--l2-rpc-url <op_besu_rpc> \
--l2-engine-url <op_besu_engine_rpc> \
--rpc-port 11545 \
--log-level INFO \
--sync-mode full
```