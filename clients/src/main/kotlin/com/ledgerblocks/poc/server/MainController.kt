package com.ledgerblocks.poc.server


import com.ledgerblocks.poc.flow.IdentityStateFlow
import com.ledgerblocks.poc.flow.LoanStateFlow
import com.ledgerblocks.poc.state.IdentityState
import net.corda.core.contracts.StateAndRef

import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

import javax.servlet.http.HttpServletRequest

import java.util.UUID

val SERVICE_NAMES = listOf("Notary", "Network Map Service")

/**
 *  A Spring Boot Server API controller for interacting with the node via RPC.
 */

@RestController
@RequestMapping("/api/lb/") // The paths for GET and POST requests are relative to this base path.
class MainController(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val myLegalName = rpc.proxy.nodeInfo().legalIdentities.first().name
    private val proxy = rpc.proxy



    /**
     * Returns the node's name.
     */
    @GetMapping(value = [ "me" ], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Displays all amounts states that exist in the node's vault.
     */
    @GetMapping(value = [ "amounts" ], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAmounts() : ResponseEntity<List<StateAndRef<IdentityState>>> {
        return ResponseEntity.ok(proxy.vaultQueryBy<IdentityState>().states)
    }

    /**
     * createidentity
     */
    @PostMapping(value = "identity")
    fun createidentity(request: HttpServletRequest): ResponseEntity<String> {
        val identityname = request.getParameter("identityname")
        val imei = request.getParameter("imei")
        val type = request.getParameter("type")
        if(identityname == null){
            return ResponseEntity.badRequest().body("Query parameter 'Identityname' must not be null.\n")
        }
        if (imei == null) {
            return ResponseEntity.badRequest().body("Query parameter 'imei' must not be null.\n")
        }
        if (type == null) {
            return ResponseEntity.badRequest().body("Query parameter 'type' must not be null.\n")
        }
        return try {

            val accountId=proxy.startTrackedFlow(::IdentityStateFlow, identityname,imei,type).returnValue.get()
            ResponseEntity.status(HttpStatus.CREATED).body("UUID:${accountId} \n")

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }



    /**
     * createLoan
     */
    @PostMapping(value = "loan")
    fun createLoan(request: HttpServletRequest): ResponseEntity<String> {


        val uuid = request.getParameter("uuid")
        val loanAmount = request.getParameter("loanAmount").toInt()
        val loanPeriod = request.getParameter("loanPeriod").toInt()
        val loanPurpose = request.getParameter("loanPurpose")
        val interestRate = request.getParameter("interestRate").toInt()
        val emi = request.getParameter("emi").toInt()
        if(uuid == null){
            return ResponseEntity.badRequest().body("Query parameter 'uuid' must not be null.\n")
        }
        if (loanAmount <= 0) {
            return ResponseEntity.badRequest().body("Query parameter ' loanAmount' must be non-negative..\n")
        }
        if (loanPeriod <= 0) {
            return ResponseEntity.badRequest().body("Query parameter ' loanPeriod' must be non-negative..\n")
        }
        if (loanPurpose == null) {
            return ResponseEntity.badRequest().body("Query parameter 'loanPurpose' must not be null.\n")
        }
        if (interestRate <= 0) {
            return ResponseEntity.badRequest().body("Query parameter ' interestRate' must be non-negative..\n")
        }
        if (emi <= 0) {
            return ResponseEntity.badRequest().body("Query parameter ' emi' must be non-negative..\n")
        }
        return try {

            val accountId=proxy.startTrackedFlow(::LoanStateFlow, UUID.fromString(uuid),loanAmount,loanPeriod,loanPurpose,interestRate,emi).returnValue.get()
            ResponseEntity.status(HttpStatus.CREATED).body("decision:${loan} \n")

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

}
