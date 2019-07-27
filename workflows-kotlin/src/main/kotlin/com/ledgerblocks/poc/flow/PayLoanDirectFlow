package com.ledgerblocks.poc.flow

import co.paralleluniverse.fibers.Suspendable

import com.ledgerblocks.poc.contract.LoanContract

import com.ledgerblocks.poc.state.PayLoanState

import com.ledgerblocks.poc.state.TokenState
import net.corda.accounts.service.KeyManagementBackedAccountService
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*



import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*

@InitiatingFlow
@StartableByRPC//private val name: String
class PayLoanDirectFlow(private val uuid: UUID, private val amtToPay: Int, private val cardNumber: Int,private val lbuuid: UUID): FlowLogic<String>(){

    @Suspendable
    override fun call(): String {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val payment:String
       
        val accountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)
       
        val lbAccountInfo=accountService.accountInfo(lbuuid)
        val resultMoveTokenInfo:StateAndRef<TokenState>

        resultMoveTokenInfo=subFlow(MoveTokensBetweenAccounts(uuid,lbuuid,amtToPay))

        if(!(resultMoveTokenInfo.equals(0))) {
            payment = "success"
        }
        else {
            payment = "failure"
        }
        val bAccountInfo=accountService.accountInfo(uuid)
       val payLoanState= PayLoanState(uuid,lbuuid,payment, bAccountInfo!!.state.data.accountHost)
        val transactionBuilder = TransactionBuilder(notary)
                .addInputState(resultMoveTokenInfo)
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

@InitiatedBy(PayLoanDirectFlow::class)
class PayLoanDirectFlowResponderFlow(val otherPartySession: FlowSession): FlowLogic<SignedTransaction>(){

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
