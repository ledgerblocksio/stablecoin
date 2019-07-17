package com.ledgerblocks.poc.contract

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey

class TokenContract: Contract {



    companion object {
        var ID = TokenContract::class.qualifiedName!!
        // val TRANSFER_TO_ACCOUNT: LoanCommands = LoanCommands("ACCOUNT_TRANSFER")
        // val TRANSFER_TO_ACCOUNT: TokenContract = TokenContract()
    }

    override fun verify(tx: LedgerTransaction) {
        val commands = tx.commands.requireSingleCommand<CommandData>()
        val setOfSigners = commands.signers.toSet()
        when (commands.value) {
            is Commands.Issue -> verifyIssue(tx, setOfSigners)
            // is Commands.Transfer -> verifyTransfer(tx, setOfSigners)
            is Commands.Combine -> verifyCombine(tx, setOfSigners)
            is Commands.TransferAccount -> verifyTransferAccount(tx, setOfSigners)

        }
    }

    private fun verifyIssue(tx: LedgerTransaction, setOfSigners: Set<PublicKey>) {
        requireThat {
            "There are no inputs" using (tx.inputs.isEmpty())
            /*"There is a single output" using (tx.outputs.size == 1)
            val output = tx.outputsOfType<TokenState>().single()
            "Request for issuance must only be signed by the owner" using (setOfSigners.contains((output.owner.owningKey)) && setOfSigners.size == 1)
            "Only token owner must be added to the list of participants when requesting issuance of a token on the ledger" using (output.participants.containsAll(listOf(output.owner)) && output.participants.size == 1)*/
        }
    }

    private fun verifyTransferAccount(tx: LedgerTransaction, setOfSigners: Set<PublicKey>) {
        requireThat {
            "There are single inputs" using (tx.inputs.size==1)
            "There is a single output" using (tx.outputs.size == 1)
            /*"There is a single output" using (tx.outputs.size == 1)
            val output = tx.outputsOfType<TokenState>().single()
            "Request for issuance must only be signed by the owner" using (setOfSigners.contains((output.owner.owningKey)) && setOfSigners.size == 1)
            "Only token owner must be added to the list of participants when requesting issuance of a token on the ledger" using (output.participants.containsAll(listOf(output.owner)) && output.participants.size == 1)*/
        }
    }

   

    private fun verifyCombine(tx: LedgerTransaction, setofSigners: Set<PublicKey>) {

    }

    interface Commands: CommandData {
        class Issue: Commands
        class Transfer: Commands
        class TransferAccount: Commands
        class Combine: Commands

    }
}
