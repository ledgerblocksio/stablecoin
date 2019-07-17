package com.ledgerblocks.poc.flow

import co.paralleluniverse.fibers.Suspendable
import com.ledgerblocks.poc.contract.IdentityContract
import com.ledgerblocks.poc.contract.LoanContract
import com.ledgerblocks.poc.state.IdentityState
import com.ledgerblocks.poc.state.LoanState
import com.sun.java.swing.plaf.gtk.GTKConstants
import net.corda.accounts.flows.RequestKeyForAccountFlow
import net.corda.accounts.service.KeyManagementBackedAccountService
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

import java.util.*

@InitiatingFlow
@StartableByRPC
class LoanStateFlow(private val uuid: UUID,private val loanAmount: Int,private val loanPeriod: Int,private val loanPurpose: String,private val interestRate: Int,private val emi: Int): FlowLogic<String>(){

    @Suspendable
    override fun call(): String {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
       val loanDecision :String
        val accountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)


        val uuidLoanStateinfo=serviceHub.vaultService.queryBy<LoanState>().states.filter {it.state.data.uuid.equals(uuid)  }

        val count= uuidLoanStateinfo.size

       val accountInfo=accountService.accountInfo(uuid)
       val lAmount = subFlow(IssueTokenToAccountIdFlow(uuid,loanAmount))
       if (count==0)
           loanDecision="Aprroved"+"loanAmount:"+ lAmount

        else
           loanDecision="Rejected"

        val loanState = LoanState(uuid, loanAmount, loanPeriod, loanPurpose,loanDecision,interestRate,emi, accountInfo!!.state.data.accountHost)
        val transactionBuilder = TransactionBuilder(notary)
                .addOutputState(loanState)
                .addCommand(LoanContract.Commands.Loan(),serviceHub.myInfo.legalIdentities.first().owningKey)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)
        transactionBuilder.verify(serviceHub)
println("test=$loanDecision")

        return subFlow(FinalityFlow(signedTransaction, emptyList())).coreTransaction.outRefsOfType<LoanState>().single().state.data.loanDecision
    }

}

@InitiatedBy(LoanStateFlow::class)
class LoanStateFlowResponderFlow(val otherPartySession: FlowSession): FlowLogic<SignedTransaction>(){

    @Suspendable
    final override fun call(): SignedTransaction {

        val stx = subFlow(object : SignTransactionFlow(otherPartySession) {

            override fun checkTransaction(stx: SignedTransaction) {
                //extraTransactionValidation(stx)
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
