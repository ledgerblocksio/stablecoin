/*
package com.ledgerblocks.poc.flow

import co.paralleluniverse.fibers.Suspendable
import com.ledgerblocks.poc.contract.LoanContract

import com.ledgerblocks.poc.state.LoanState

import net.corda.accounts.flows.RequestKeyForAccountFlow
import net.corda.accounts.service.KeyManagementBackedAccountService
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*


@StartableByRPC
@InitiatingFlow
class MoveLoanAmountBetweenAccounts(private val bUUID: UUID, private val mUUID: UUID,private val amtToPay: Int): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val accountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)
        val bAccountInfo=accountService.accountInfo(bUUID)
        val mAccountInfo=accountService.accountInfo(mUUID)
        val freshkeyForbAccountInfo= subFlow(RequestKeyForAccountFlow(bAccountInfo!!.state.data))
        val freshkeyFormAccountInfo= subFlow(RequestKeyForAccountFlow(mAccountInfo!!.state.data))

        val owningKeyForbAccount =accountService.accountInfo(freshkeyForbAccountInfo.owningKey)
        val owningKeyFormAccount =accountService.accountInfo(freshkeyFormAccountInfo.owningKey)

        val signingAccounts = listOfNotNull(owningKeyForbAccount, owningKeyFormAccount)

        val requiredSigners =
                signingAccounts.map { it.state.data.accountHost.owningKey } + listOfNotNull(
                        freshkeyForbAccountInfo.owningKey, freshkeyFormAccountInfo.owningKey,ourIdentity.owningKey)
        val inputLoanStateReference = serviceHub.vaultService.queryBy<LoanState>().states.filter { it.state.data.uuid.equals(bUUID) }

        val size=  inputLoanStateReference.size

        println("1inputLoanStateReference=$size")

        val borrowerLoan= inputLoanStateReference.get(inputLoanStateReference.size-1).state.data
        println("borrowerToken-Amount=${borrowerLoan.loanAmount}")
        val updatedAmtToPay = borrowerLoan.loanAmount-amtToPay
        println("updatedloanAmount=$amtToPay")
        val loanState = LoanState(bUUID,updatedAmtToPay, borrowerLoan.loanPeriod, borrowerLoan.loanPurpose,borrowerLoan.loanDecision,borrowerLoan.interestRate, borrowerLoan.emi, bAccountInfo!!.state.data.accountHost)

        println("updated loanState=$loanState")
        val transactionBuilder = TransactionBuilder(notary)
                .addOutputState(loanState)
                .addCommand(LoanContract.Commands.Loan(),serviceHub.myInfo.legalIdentities.first().owningKey)
        val partiallySignedTransaction = serviceHub.signInitialTransaction(transactionBuilder,listOfNotNull(
                ourIdentity.owningKey
        ))
        val accountSession1 = initiateFlow(bAccountInfo.state.data.accountHost)
        val accountSession = initiateFlow(mAccountInfo.state.data.accountHost)
        val fullySignedTransaction = subFlow(CollectSignaturesFlow(partiallySignedTransaction, listOf(accountSession,accountSession1)))

        val sessions = if (!serviceHub.myInfo.isLegalIdentity(mAccountInfo.state.data.accountHost))
            Collections.singletonList(accountSession)
        else
            Collections.emptyList()
        val sessions1 = if (!serviceHub.myInfo.isLegalIdentity(bAccountInfo.state.data.accountHost))
            Collections.singletonList(accountSession1)
        else
            Collections.emptyList()
        val size1=  inputLoanStateReference.size
        println("1inputLoanStateReference=$size1")

        return  subFlow(FinalityFlow(fullySignedTransaction, sessions))//.coreTransaction.outRefsOfType(LoanState::class.java).filter {it.state.data.uuid.equals(bUUID)}.get(inputLoanStateReference.size-1) .
                .also {

            val broadcastToParties =
                    serviceHub.networkMapCache.allNodes.map { node -> node.legalIdentities.first() }
                            .minus(serviceHub.networkMapCache.notaryIdentities)
            subFlow(
                    BroadcastTransactionFlow(
                            fullySignedTransaction, broadcastToParties
                    )
            )
        }
    }


}
@InitiatedBy(MoveLoanAmountBetweenAccounts::class)
class MoveLoanAmountBetweenAccountsResponderFlow(val otherPartySession: FlowSession): FlowLogic<SignedTransaction>(){

    @Suspendable
    final override fun call(): SignedTransaction {

        val stx = subFlow(object : SignTransactionFlow(otherPartySession) {

            override fun checkTransaction(stx: SignedTransaction) {
            }
        })
        val tx = subFlow(
                ReceiveFinalityFlow(
                        otherSideSession = otherPartySession,
                        expectedTxId = stx.id
                )
        )
        logger.info("Received transaction from finality")
        return tx
    }
}
*/
