package com.ledgerblocks.poc.flow

import co.paralleluniverse.fibers.Suspendable
import com.ledgerblocks.poc.contract.IdentityContract
import com.ledgerblocks.poc.state.IdentityState

import net.corda.accounts.service.KeyManagementBackedAccountService
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name


import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*


@InitiatingFlow
@StartableByRPC
class IdentityStateFlow(private val name: String, private val imei: String, private val type: String): FlowLogic<UUID>(){

    @Suspendable
    override fun call(): UUID {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val  newImei:String
        val  newParty : String
        if(type.equals('m')) {
             newImei=type+imei
            newParty= "O=PartyA,L=London,C=GB"
        }
        else if (type.equals('b'))
        {
            newImei=type+imei
            newParty= "O=PartyB,L=New York,C=US"
        }
          else
        {
            newImei=imei
           newParty= "O=PartyA,L=London,C=GB"
        }
            val id = type+name + newImei
            val accountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)
            val newAccountCreation = accountService.createAccount(id)
            val storedAccountInfo = accountService.accountInfo(id)
       val x500Name = CordaX500Name.parse(newParty)
        val party= serviceHub.networkMapCache.getPeerByLegalName(x500Name)!!

            accountService.shareAccountInfoWithParty(storedAccountInfo!!.state.data.accountId, party)
        val accounts = accountService.allAccounts()
            val identityState = IdentityState(name, imei, storedAccountInfo!!.state.data.accountId, storedAccountInfo.state.data.accountHost)
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
