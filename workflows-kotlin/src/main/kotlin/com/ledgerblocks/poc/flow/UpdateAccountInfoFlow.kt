package com.ledgerblocks.poc.flow



import co.paralleluniverse.fibers.Suspendable
import com.ledgerblocks.poc.contract.IdentityContract
import com.ledgerblocks.poc.state.IdentityState


import net.corda.accounts.service.KeyManagementBackedAccountService
import net.corda.core.flows.*

import net.corda.core.node.services.queryBy


import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*


@InitiatingFlow
@StartableByRPC
class UpdateAccountInfoFlow(private val uuid: UUID, private val fcmToken: String): FlowLogic<String>(){

    @Suspendable
    override fun call(): String {
        var update="no"
        val notary = serviceHub.networkMapCache.notaryIdentities.first()



        val accountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)

        val identityStates=serviceHub.vaultService.queryBy<IdentityState>().states.filter {it.state.data.uuid.equals(uuid)}

        if(identityStates.isNotEmpty()) {

            val identityStateInfo = identityStates.get(0).state.data
            update="yes"
            val storedAccountInfo = accountService.accountInfo(uuid)
            val identityState = IdentityState(identityStateInfo.name, fcmToken, identityStateInfo.imei, storedAccountInfo!!.state.data.accountId, identityStateInfo.host)
            val transactionBuilder = TransactionBuilder(notary)
                    .addOutputState(identityState)
                    .addCommand(IdentityContract.OPEN, serviceHub.myInfo.legalIdentities.first().owningKey)
            val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)
            transactionBuilder.verify(serviceHub)
        }
        return update
    }

}

@InitiatedBy(UpdateAccountInfoFlow::class)
class UpdateAccountInfoFlowResponderFlow(val otherPartySession: FlowSession): FlowLogic<SignedTransaction>(){

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
