package com.ledgerblocks.poc.flow

import co.paralleluniverse.fibers.Suspendable

import com.ledgerblocks.poc.contract.LoanContract

import com.ledgerblocks.poc.state.LoanState

import net.corda.accounts.service.KeyManagementBackedAccountService

import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name

import net.corda.core.node.services.queryBy

import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

import java.util.*

@InitiatingFlow
@StartableByRPC
class LoanStateFlow(private val uuid: UUID, private val loanAmount: Int,private val loanPeriod: Int,private val interestRate: Int,private val emi: Int,private val loanPurpose: String): FlowLogic<String>(){

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
            loanDecision="Aprroved+"+"loanAmount:"+lAmount

        else
            loanDecision="Rejected+"+"loanAmount: "+0

        val loanState = LoanState(uuid,loanAmount, loanPeriod, loanPurpose,loanDecision,interestRate,emi, accountInfo!!.state.data.accountHost)
        val transactionBuilder = TransactionBuilder(notary)
                .addOutputState(loanState)
                .addCommand(LoanContract.Commands.Loan(),serviceHub.myInfo.legalIdentities.first().owningKey)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)
        transactionBuilder.verify(serviceHub)
        println("test=$loanDecision")
        var  party1 = "O=PartyA,L=London,C=GB"
        var  party2 = "O=PartyB,L=New York,C=US"
        var  party3 = "O=PartyC,L=Paris,C=FR"

        val x500Name = CordaX500Name.parse(party1)
        val bParty= serviceHub.networkMapCache.getPeerByLegalName(x500Name)!!

        val x500Name1 = CordaX500Name.parse(party2)
        val mParty= serviceHub.networkMapCache.getPeerByLegalName(x500Name1)!!

        val x500Name2 = CordaX500Name.parse(party3)
        val lbParty= serviceHub.networkMapCache.getPeerByLegalName(x500Name2)!!



        return subFlow(FinalityFlow(signedTransaction, emptyList())).coreTransaction.outRefsOfType<LoanState>().single().state.data.loanDecision/*also {

            val broadcastToParties =
                    serviceHub.networkMapCache.allNodes.map { node -> node.legalIdentities.first() }
                            .minus(serviceHub.networkMapCache.notaryIdentities)
                            .minus(mParty)
                            .minus(bParty)
                            .minus(lbParty)
            subFlow(
                    BroadcastTransactionFlow(
                            it, broadcastToParties
                    )
            )
        }*/



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
