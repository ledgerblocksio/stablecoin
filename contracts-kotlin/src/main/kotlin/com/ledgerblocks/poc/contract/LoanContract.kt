package com.ledgerblocks.poc.contract


import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand

import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey


class LoanContract: Contract {

    companion object {
        var ID = LoanContract::class.qualifiedName!!
    }

    override fun verify(tx: LedgerTransaction) {
        val commands = tx.commands.requireSingleCommand<CommandData>()
        val setOfSigners = commands.signers.toSet()
        when (commands.value) {
            is Commands.Loan -> verifyLoan(tx, setOfSigners)
        }
    }

    private fun verifyLoan(tx: LedgerTransaction, setOfSigners: Set<PublicKey>) {
        requireThat {
            "There are no inputs" using (tx.inputs.isEmpty())
        }
    }

    interface Commands: CommandData {
        class Loan: Commands
    }
}
