package com.ledgerblocks.poc.flow

import co.paralleluniverse.fibers.Suspendable
import com.ledgerblocks.poc.contract.IdentityContract
import com.ledgerblocks.poc.state.IdentityState
import net.corda.accounts.service.KeyManagementBackedAccountService
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name


import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder



@InitiatingFlow
@StartableByRPC
class IdentityStateFlow(private val name: String, private val fcmToken: String, private val imei: String, private val type: String): FlowLogic<SignedTransaction>(){

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        var  newIMEI: String
        val  newParty : String
        val  newParty1 : String
        if(type.equals('m')) {
            newIMEI=type+imei
            newParty="O=PartyA,L=London,C=GB"
            newParty1="O=PartyC,L=Paris,C=FR"
        }
        else if (type.equals('b'))
        {
            newIMEI=type+imei
            newParty="O=PartyB,L=New York,C=US"
            newParty1="O=PartyC,L=Paris,C=FR"
        }
        else if(type.equals('o')) {
            newIMEI=type+imei
            newParty="O=PartyA,L=London,C=GB"
            newParty1="O=PartyB,L=New York,C=US"
        }
        else
        {
            newIMEI=imei
            newParty="O=PartyA,L=London,C=GB"
            newParty1="O=PartyC,L=Paris,C=FR"
       }
        val id = type+name+imei
        val accountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)
        val newAccountCreation = accountService.createAccount(id)
        val storedAccountInfo = accountService.accountInfo(id)
        val x500Name = CordaX500Name.parse(newParty)
        val party= serviceHub.networkMapCache.getPeerByLegalName(x500Name)!!

        val x500Name1 = CordaX500Name.parse(newParty1)
        val party1= serviceHub.networkMapCache.getPeerByLegalName(x500Name1)!!
        accountService.shareAccountInfoWithParty(storedAccountInfo!!.state.data.accountId, party)
        accountService.shareAccountInfoWithParty(storedAccountInfo!!.state.data.accountId, party1)
        val accounts = accountService.allAccounts()
        val identityState = IdentityState(name, fcmToken, imei, storedAccountInfo!!.state.data.accountId, storedAccountInfo.state.data.accountHost)
        val transactionBuilder = TransactionBuilder(notary)
                .addOutputState(identityState)
                .addCommand(IdentityContract.OPEN,serviceHub.myInfo.legalIdentities.first().owningKey)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)
        transactionBuilder.verify(serviceHub)
        return subFlow(FinalityFlow(signedTransaction, emptyList())).also {
            val broadcastToParties =
                    serviceHub.networkMapCache.allNodes.map { node -> node.legalIdentities.first() }
                            .minus(serviceHub.networkMapCache.notaryIdentities)
            subFlow(
                    BroadcastTransactionFlow(
                            it, broadcastToParties
                    )
            )
        }

        //.coreTransaction.outRefsOfType<IdentityState>().single().state.data.uuid
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
