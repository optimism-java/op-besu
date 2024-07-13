package org.hyperledger.besu.datatypes;

import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RollupGasDataTest {



  @Test
  void testFlzCompressLength() {
    long flzSize = RollupGasData.flzCompressLength(new byte[0]);
    assertEquals(0, flzSize);

    byte[] data = new byte[1000];
    Arrays.fill(data, (byte) 1);
    flzSize = RollupGasData.flzCompressLength(data);
    assertEquals(21, flzSize);

    data = new byte[1000];
    flzSize = RollupGasData.flzCompressLength(data);
    assertEquals(21, flzSize);

    byte[] emptyTx = Hex.decode("DD80808094095E7BAEA6A6C7C4C2DFEB977EFAC326AF552D878080808080");
    flzSize = RollupGasData.flzCompressLength(emptyTx);
    assertEquals(31, flzSize);

    byte[] contractCallTx = Hex.decode("02f901550a758302df1483be21b88304743f94f8" +
        "0e51afb613d764fa61751affd3313c190a86bb870151bd62fd12adb8" +
        "e41ef24f3f0000000000000000000000000000000000000000000000" +
        "00000000000000006e000000000000000000000000af88d065e77c8c" +
        "c2239327c5edb3a432268e5831000000000000000000000000000000" +
        "000000000000000000000000000003c1e50000000000000000000000" +
        "00000000000000000000000000000000000000000000000000000000" +
        "000000000000000000000000000000000000000000000000a0000000" +
        "00000000000000000000000000000000000000000000000000000000" +
        "148c89ed219d02f1a5be012c689b4f5b731827bebe00000000000000" +
        "0000000000c001a033fd89cb37c31b2cba46b6466e040c61fc9b2a36" +
        "75a7f5f493ebd5ad77c497f8a07cdf65680e238392693019b4092f61" +
        "0222e71b7cec06449cb922b93b6a12744e");
    flzSize = RollupGasData.flzCompressLength(contractCallTx);
    assertEquals(202, flzSize);
  }

}