package com.ledgerblocks.poc.flow

import co.paralleluniverse.fibers.Suspendable
import com.ledgerblocks.poc.contract.IdentityContract
import com.ledgerblocks.poc.state.IdentityState

import net.corda.accounts.flows.RequestKeyForAccountFlow
import net.corda.accounts.service.KeyManagementBackedAccountService
import net.corda.accounts.states.AccountInfo
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*


import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*


@InitiatingFlow
@StartableByRPC
class IdentityStateFlow(private val identityname: String,private val imei: String,private val type: String): FlowLogic<UUID>(){

    @Suspendable
    override fun call(): UUID {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val  newImei:String
        if(type.equals('m')) {
             newImei=type+imei
        }
        else if (type.equals('b'))
        {
            newImei=type+imei
        }
          else
        {
            newImei=imei
        }
            val id = type+identityname + newImei
            val accountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)
            val newAccountCreation = accountService.createAccount(id)
            val storedAccountInfo = accountService.accountInfo(id)
            //val freshkeyNewAccount = subFlow(RequestKeyForAccountFlow(storedAccountInfo!!.state.data))
            val identityState = IdentityState(identityname, imei, storedAccountInfo!!.state.data.accountId, storedAccountInfo.state.data.accountHost)
            val transactionBuilder = TransactionBuilder(notary)
                .addOutputState(identityState)
                .addCommand(IdentityContract.OPEN,serviceHub.myInfo.legalIdentities.first().owningKey)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)
        transactionBuilder.verify(serviceHub)

        return subFlow(FinalityFlow(signedTransaction, emptyList())).coreTransaction.outRefsOfType<IdentityState>().single().state.data.uuid
    }

}

@InitiatedBy(IdentityStateFlow::class)
class IdentityStateFlowResponderFlow(val otherPartySession: FlowSession): FlowLogic<SignedTransaction>(){

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
