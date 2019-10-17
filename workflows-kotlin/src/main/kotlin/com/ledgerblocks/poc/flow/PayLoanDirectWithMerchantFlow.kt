package com.ledgerblocks.poc.flow

import co.paralleluniverse.fibers.Suspendable


import com.ledgerblocks.poc.contract.TokenContract
import com.ledgerblocks.poc.state.PayLoanState


import com.ledgerblocks.poc.state.TokenState
import net.corda.accounts.flows.RequestKeyForAccountFlow
import net.corda.accounts.service.KeyManagementBackedAccountService

import net.corda.core.flows.*
import net.corda.core.node.services.queryBy


import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

import java.time.LocalDate

import java.util.*

@InitiatingFlow
@StartableByRPC//private val name: String
class PayLoanDirectThroughMerchantFlow(private val bUUID: UUID, private val mUUID: UUID, private val lbUUID: UUID, private val amtToPay1: Int): FlowLogic<SignedTransaction>(){
    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val payment:String

        val accountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)

         subFlow(DeductLoanAmountFlow(bUUID,amtToPay1))

      //  requestDeductLoan(bUUID,amtToPay1)
      //  subFlow(AddTokensToMerchantFlow(bUUID,mUUID,amtToPay1))
        val mTokenStateinfo=serviceHub.vaultService.queryBy<TokenState>().states.filter { it.state.data.toAccountId!!.equals(mUUID)||it.state.data.fromAccountId!!.equals(mUUID)}
        println("mTokenStateinfo=$mTokenStateinfo")
        val mTokenBal = mTokenStateinfo.get(mTokenStateinfo.size-1).state.data.tokenBalance

        println("mTokenBal=$mTokenBal")

        val lbAccountInfo = accountService.accountInfo(lbUUID)

        println("lbAccountInfo=$lbAccountInfo")
        val mAccountInfo=accountService.accountInfo(mUUID)
        val mParty = mAccountInfo!!.state.data.accountHost
        val freshkeyToAccount = subFlow(RequestKeyForAccountFlow(mAccountInfo!!.state.data))
        //  val bInitialLoanAmount= bUuidLoanStateinfo.get(0).state.data.loanAmount
        //val exchange:String

        if(!(amtToPay1.equals(0))) {

            payment = "success"

            val paymentState= PayLoanState(mUUID,lbUUID,payment, mAccountInfo!!.state.data.accountHost)
            val updatedAmount: Int
            //var resultMoveTokenInfo:SignedTransaction ? = null

            updatedAmount = mTokenBal - amtToPay1


            val tokenState = TokenState(updatedAmount, amtToPay1, mUUID, lbUUID, "payloanMerchant", LocalDate.now().toString(), freshkeyToAccount.owningKey, mAccountInfo!!.state.data.accountHost, listOf(mAccountInfo!!.state.data.accountHost))

            // val tokenState = TokenState(mParty,mParty,updatedAmount,uuid,uuid,exchange,freshkeyToAccount.owningKey, mAccountInfo!!.state.data.accountHost, listOf(mAccountInfo!!.state.data.accountHost))

            val transactionBuilder = TransactionBuilder(notary)
                    //.addInputState(resultMoveTokenInfo!!.coreTransaction.outRefsOfType<TokenState>().single())
                    .addOutputState(tokenState)
                    .addOutputState(paymentState)
                    .addCommand(TokenContract.Commands.Exchange(), serviceHub.myInfo.legalIdentities.first().owningKey)
            val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)
            transactionBuilder.verify(serviceHub)
            val accountSession = initiateFlow(mAccountInfo!!.state.data.accountHost)
            val sessions = if (!serviceHub.myInfo.isLegalIdentity(mAccountInfo.state.data.accountHost))
                Collections.singletonList(accountSession)
            else
                Collections.emptyList()
            return subFlow(FinalityFlow(signedTransaction, sessions)).also {

                val broadcastToParties =
                        serviceHub.networkMapCache.allNodes.map { node -> node.legalIdentities.first() }
                                .minus(serviceHub.networkMapCache.notaryIdentities)
                //.minus(mParty)
                //  .minus(bParty)
                subFlow(
                        BroadcastTransactionFlow(
                                it, broadcastToParties
                        )
                )
            }

        }


        else {
            payment = "failure"

            val paymentState = PayLoanState(mUUID, lbUUID, payment, mAccountInfo!!.state.data.accountHost)
            val updatedAmount: Int
            //var resultMoveTokenInfo:SignedTransaction ? = null

            updatedAmount = mTokenBal


            val tokenState = TokenState(updatedAmount, amtToPay1, mUUID, lbUUID, "payloanMerchant", LocalDate.now().toString(), freshkeyToAccount.owningKey, mAccountInfo!!.state.data.accountHost, listOf(mAccountInfo!!.state.data.accountHost))

            // val tokenState = TokenState(mParty,mParty,updatedAmount,uuid,uuid,exchange,freshkeyToAccount.owningKey, mAccountInfo!!.state.data.accountHost, listOf(mAccountInfo!!.state.data.accountHost))

            val transactionBuilder = TransactionBuilder(notary)
                    //.addInputState(resultMoveTokenInfo!!.coreTransaction.outRefsOfType<TokenState>().single())
                    .addOutputState(tokenState)
                    .addOutputState(paymentState)
                    .addCommand(TokenContract.Commands.Exchange(), serviceHub.myInfo.legalIdentities.first().owningKey)
            val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)
            transactionBuilder.verify(serviceHub)
            val accountSession = initiateFlow(mAccountInfo!!.state.data.accountHost)
            val sessions = if (!serviceHub.myInfo.isLegalIdentity(mAccountInfo.state.data.accountHost))
                Collections.singletonList(accountSession)
            else
                Collections.emptyList()
            return subFlow(FinalityFlow(signedTransaction, sessions)).also {

                val broadcastToParties =
                        serviceHub.networkMapCache.allNodes.map { node -> node.legalIdentities.first() }
                                .minus(serviceHub.networkMapCache.notaryIdentities)
                //.minus(mParty)
                //  .minus(bParty)
                subFlow(
                        BroadcastTransactionFlow(
                                it, broadcastToParties
                        )
                )
            }
        }
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
