package com.ledgerblocks.poc.flow

import co.paralleluniverse.fibers.Suspendable

import com.ledgerblocks.poc.contract.LoanContract

import com.ledgerblocks.poc.state.PayLoanState

import com.ledgerblocks.poc.state.TokenState
import net.corda.accounts.service.KeyManagementBackedAccountService

import net.corda.core.flows.*
import net.corda.core.node.services.queryBy


import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.io.FileInputStream

import java.util.*

@InitiatingFlow
@StartableByRPC//private val name: String
class PayLoanDirectThroughMerchantFlow(private val bUUID: UUID, private val mUUID: UUID, private val lbUUID: UUID, private val amtToPay1: Int): FlowLogic<String>(){
    @Suspendable
    override fun call(): String {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val payment:String

        val accountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)

      //  subFlow(DeductLoanAmountFlow(bUUID,amtToPay1))
        requestDeductLoan(bUUID,amtToPay1)
        subFlow(AddTokensToMerchantFlow(mUUID,amtToPay1))
        val mTokenStateinfo=serviceHub.vaultService.queryBy<TokenState>().states.filter {it.state.data.toAccountId.equals(mUUID)}
        val mTokenBal = mTokenStateinfo.get(mTokenStateinfo.size-1).state.data.amount
        val lbAccountInfo = accountService.accountInfo(lbUUID)

        println("lbAccountInfo=$lbAccountInfo")

        if(!(mTokenBal.equals(0))) {
            payment = "success"
        }
        else {
            payment = "failure"
        }
        val mAccountInfo=accountService.accountInfo(mUUID)
        val payLoanState= PayLoanState(mUUID,lbUUID,payment, mAccountInfo!!.state.data.accountHost)
        val transactionBuilder = TransactionBuilder(notary)
                .addInputState(mTokenStateinfo.get(mTokenStateinfo.size-1))
                .addOutputState(payLoanState)
                .addCommand(LoanContract.Commands.Pay(),serviceHub.myInfo.legalIdentities.first().owningKey)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)
        transactionBuilder.verify(serviceHub)
        val accountSession = initiateFlow(lbAccountInfo!!.state.data.accountHost)
        val sessions = if (!serviceHub.myInfo.isLegalIdentity(lbAccountInfo.state.data.accountHost))
            Collections.singletonList(accountSession)
        else
            Collections.emptyList()
        return subFlow(FinalityFlow(signedTransaction, sessions)).coreTransaction.outRefsOfType<PayLoanState>().single().state.data.payment

    }

}

@InitiatedBy(PayLoanDirectThroughMerchantFlow::class)
class PayLoanDirectThroughMerchantFlowResponderFlow(val otherPartySession: FlowSession): FlowLogic<SignedTransaction>(){

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
