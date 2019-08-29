package com.ledgerblocks.poc.flow

import co.paralleluniverse.fibers.Suspendable
import com.ledgerblocks.poc.contract.LoanContract
import com.ledgerblocks.poc.state.LoanState
import net.corda.accounts.flows.RequestKeyForAccountFlow
import net.corda.accounts.service.KeyManagementBackedAccountService
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*


@StartableByRPC
@InitiatingFlow
class DeductLoanAmountFlow(private val bUUID: UUID,private val amtToPay: Int): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val accountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)

        val bAccountInfo=accountService.accountInfo(bUUID)

        val freshkeyFormAccountInfo= subFlow(RequestKeyForAccountFlow(bAccountInfo!!.state.data))
        val owningKeyFormAccount =accountService.accountInfo(freshkeyFormAccountInfo.owningKey)
        val bUuidLoanStateinfo=serviceHub.vaultService.queryBy<LoanState>().states.filter {it.state.data.uuid.equals(bUUID)  }

        println("bUuidLoanStateinfo=$bUuidLoanStateinfo")
        val bInitialLoanAmount= bUuidLoanStateinfo.get(0).state.data.loanAmount
        val loanPeriod=bUuidLoanStateinfo.get(0).state.data.loanPeriod
        val loanPurpose=bUuidLoanStateinfo.get(0).state.data.loanPurpose
        val loanDecision=bUuidLoanStateinfo.get(0).state.data.loanDecision
        val interestRate=bUuidLoanStateinfo.get(0).state.data.interestRate
        val emi=bUuidLoanStateinfo.get(0).state.data.emi
        val loanState = LoanState(bUUID,amtToPay-bInitialLoanAmount, loanPeriod, loanPurpose,loanDecision,interestRate,emi, bAccountInfo!!.state.data.accountHost)
        val transactionBuilder = TransactionBuilder(notary)
                .addOutputState(loanState)
                .addCommand(LoanContract.Commands.Loan(),serviceHub.myInfo.legalIdentities.first().owningKey)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)
        transactionBuilder.verify(serviceHub)
        println("test=$loanDecision")

        return subFlow(FinalityFlow(signedTransaction, emptyList()))

    }
}

@InitiatedBy(DeductLoanAmountFlow::class)
class DeductLoanAmountFlowResponderFlow(val otherPartySession: FlowSession): FlowLogic<SignedTransaction>(){

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