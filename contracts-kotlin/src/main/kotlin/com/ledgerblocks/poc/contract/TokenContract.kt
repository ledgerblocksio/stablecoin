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
/*

    private fun verifyTransfer(tx: LedgerTransaction, setOfSigners: Set<PublicKey>) {
        requireThat {


            val input = tx.inputsOfType<TokenState>().single()
            "There are two outputs" using (tx.outputs.size == 2)
            val outputs = tx.outputsOfType<TokenState>()

            val outputOfIssuer: TokenState
            val outputOfReceiver: TokenState


            if (outputs[0].owner == input.owner) {
                outputOfIssuer = outputs[0]
                outputOfReceiver = outputs[1]
            } else {
                outputOfIssuer = outputs[1]
                outputOfReceiver = outputs[0]
            }

            "Issuer must have sufficient balance to transfer amount" using (outputOfIssuer.amount > 0)
            "Total amount of outputs must match input" using (outputOfIssuer.amount + outputOfReceiver.amount == input.amount)
        }
    }
*/

    /*private fun verifyTransferAccount(tx: LedgerTransaction, setOfSigners: Set<PublicKey>) {

        val commands = tx.commands.requireSingleCommand<CommandData>()
        val attachedAccounts = tx.referenceInputRefsOfType(AccountInfo::class.java)
        val output = tx.outputsOfType(TokenState::class.java).single()
        val input = tx.inputsOfType(TokenState::class.java).single()

        val accountForInputState = attachedAccounts.singleOrNull { it.state.data.signingKey == input.owningAccount }?.state?.data
        val accountForOutputState = attachedAccounts.singleOrNull { it.state.data.signingKey == output.owningAccount }?.state?.data

        requireNotNull(accountForOutputState) { "The account info state for the new owner must be attached to the transaction" }


        if (input.owningAccount != null) {
            requireNotNull(accountForInputState) { "The account info state for the existing owner must be attached to the transaction" }
            require(accountForInputState?.signingKey in commands.signers) { "The account that is selling the loan must be a required signer" }
            require(accountForInputState?.accountHost?.owningKey in commands.signers) { "The hosting party for the account that is sending the loan must be a required signer" }
        }

        require(accountForOutputState?.signingKey in commands.signers) { "The account that is buying the loan must be a required signer" }
        require(accountForOutputState?.accountHost?.owningKey in commands.signers) { "The hosting party for the account that is receiving the loan must be a required signer" }

    }*/


    private fun verifyCombine(tx: LedgerTransaction, setofSigners: Set<PublicKey>) {

    }

    interface Commands: CommandData {
        class Issue: Commands
        class Transfer: Commands
        class TransferAccount: Commands
        class Combine: Commands

    }
}