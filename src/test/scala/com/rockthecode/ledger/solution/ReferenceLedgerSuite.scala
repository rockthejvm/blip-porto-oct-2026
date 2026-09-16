package com.rockthecode.ledger.solution

import com.rockthecode.ledger.LedgerSuite

import java.nio.file.Path

/** Trainer: the reference solution against every scenario. Part of `sbt checkSolutions`. */
class ReferenceLedgerSuite extends LedgerSuite(ReferenceLedger, Path.of("src/main/resources/ledger"))
