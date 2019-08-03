package com.ledgerblocks.poc.server

import com.ledgerblocks.poc.flow.*


import com.ledgerblocks.poc.state.IdentityState
import com.ledgerblocks.poc.state.LoanState
import com.ledgerblocks.poc.state.TokenState
import net.corda.core.contracts.StateAndRef
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.*
import java.io.File
import java.util.*

import javax.servlet.http.HttpServletRequest
import kotlin.collections.ArrayList

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
    @GetMapping(value = ["me"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Displays all amounts states that exist in the node's vault.
     */
    @GetMapping(value = ["identityStates"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getidentityStates(): ResponseEntity<List<StateAndRef<IdentityState>>> {
        return ResponseEntity.ok(proxy.vaultQueryBy<IdentityState>().states)
    }

    /**
     * createIdentity
     */
    @PostMapping(value = ["identity"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun createIdentity(request: HttpServletRequest): ResponseEntity<Any?> {
        val name = request.getParameter("name")
        val mobileToken = request.getParameter("mobileToken")
        val imei = request.getParameter("imei")
        val type = request.getParameter("type")
        //val currentDirectory = System.getProperty("user.dir")
        //val lbPropDirectory = currentDirectory+"lbuuid.properties"
        //val fis = FileInputStream("/home/user/Desktop/samples/linearlb/clients/src/main/kotlin/com/ledgerblocks/poc/server/lbuuid.properties")
        //val fis = FileInputStream(lbPropDirectory)
        //val properties = Properties()
        //properties.load(fis)
        //val lbUUID =  properties.getProperty("lbuuid")
        //val lbUUID1 = UUID.fromString(lbUUID)
        if (name == null) {
            return ResponseEntity.badRequest().body("Query parameter 'name' must not be null.\n")
        }
        if (imei == null) {
            return ResponseEntity.badRequest().body("Query parameter 'imei' must not be null.\n")
        }
        if (type == null) {
            return ResponseEntity.badRequest().body("Query parameter 'type' must not be null.\n")
        }
        return try {
            val accountId = proxy.startTrackedFlow(::IdentityStateFlow, name , mobileToken, imei, type).returnValue.get()
            ResponseEntity.status(HttpStatus.CREATED).body("uuid:${accountId}")
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }


    /**
     * createLoan
     */

    @PostMapping(value = ["loan"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun createLoan(request: HttpServletRequest): ResponseEntity<String> {
        val uuid = request.getParameter("uuid")
        val uuid1 = UUID.fromString(uuid)
      //  val lbUUID = request.getParameter("lbUUID")
       // val lbUUID1 = UUID.fromString(lbUUID)
        val loanAmount = request.getParameter("loanAmount").toInt()
        val loanPeriod = request.getParameter("loanPeriod").toInt()
        val loanPurpose = request.getParameter("loanPurpose")
        val interestRate = request.getParameter("interestRate").toInt()
        val emi = request.getParameter("emi").toInt()


        if (uuid == null) {
            return ResponseEntity.badRequest().body("Query parameter 'uuid' must not be null.\n")
        }
        /*if (lbUUID == null) {
            return ResponseEntity.badRequest().body("Query parameter 'lbUUID' must not be null.\n")
        }*/
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
            val loan = proxy.startTrackedFlow(::LoanStateFlow, uuid1, loanAmount, loanPeriod, interestRate, emi, loanPurpose).returnValue.get()


            ResponseEntity.status(HttpStatus.CREATED).body("decision:${loan}")

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }


    /**
     * createPurchase
     */
    @PostMapping(value = ["purchase"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun createPurchase(request: HttpServletRequest): ResponseEntity<String> {
        val bUuid = request.getParameter("bUUID")
        val bUuid1 = UUID.fromString(bUuid)
        val mUuid = request.getParameter("mUUID")
        val mUuid1 = UUID.fromString(mUuid)
        val purchaseAmt = request.getParameter("purchaseAmount").toInt()
        val goodsDesc = request.getParameter("goodsDesc")
        if (bUuid == null) {
            return ResponseEntity.badRequest().body("Query parameter 'bUUID' must not be null.\n")
        }
        if (mUuid == null) {
            return ResponseEntity.badRequest().body("Query parameter 'mUUID' must not be null.\n")
        }
        if (purchaseAmt <= 0) {
            return ResponseEntity.badRequest().body("Query parameter ' purchaseAmount' must be non-negative..\n")
        }
        if (goodsDesc == null) {
            return ResponseEntity.badRequest().body("Query parameter 'goodsDesc' must not be null.\n")
        }
        return try {
            val purchase = proxy.startTrackedFlow(::GoodsPurchaseFlow, bUuid1, mUuid1, purchaseAmt, goodsDesc).returnValue.get()
            val bUuidTokenStateinfo=proxy.vaultQueryBy<TokenState>().states.filter { it.state.data.accountId.equals(bUuid1)}
            bUuidTokenStateinfo.forEach{tokenState:StateAndRef<TokenState> ->
                val stateAmount = tokenState.state.data.amount
                println("amount=$stateAmount")
            }
            val borrowerTokenBal= bUuidTokenStateinfo.get(bUuidTokenStateinfo.size-1).state.data.amount
            val bUuidLoanStateinfo=proxy.vaultQueryBy<LoanState>().states.filter { it.state.data.uuid.equals(bUuid1)}
            val bInitialLoanAmount= bUuidLoanStateinfo.get(0).state.data.loanAmount
            ResponseEntity.status(HttpStatus.CREATED).body("purchase:${purchase}+loanAmount:${bInitialLoanAmount}+avaBlance:${borrowerTokenBal}")
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }



    /**
     * Dashboard
     */

    @GetMapping(value = ["dashboard"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun dashBoardApi(request: HttpServletRequest): ResponseEntity<Any> {
        val uuid = request.getParameter("uuid")
        val Uuid1 = UUID.fromString(uuid)
        val type = request.getParameter("type")
        if (uuid == null) {
            return ResponseEntity.badRequest().body("Query parameter 'uuid' must not be null.\n")
        }
        if (type == null) {
            return ResponseEntity.badRequest().body("Query parameter 'type' must not be null.\n")
        }
        val mIdentityStateinfo=proxy.vaultQueryBy<IdentityState>().states.filter {it.state.data.uuid.equals(Uuid1)}
        val bIdentityStateinfo=proxy.vaultQueryBy<IdentityState>().states.filter {it.state.data.uuid.equals(Uuid1)}
        val bTokenStateinfo=proxy.vaultQueryBy<TokenState>().states.filter {it.state.data.accountId.equals(Uuid1)}
        val bLoanStateinfo=proxy.vaultQueryBy<LoanState>().states.filter {it.state.data.uuid.equals(Uuid1)}
        val result=if(type.equals('m')) "mName:${mIdentityStateinfo.get(0).state.data.name}+tokenBalance:${75000}" else  "name:${bIdentityStateinfo.get(bIdentityStateinfo.size-1).state.data.name}+loanAmount:${bLoanStateinfo.get(0).state.data.loanAmount}+avaBlance:${bTokenStateinfo.get(bTokenStateinfo.size-1).state.data.amount}"
        return ResponseEntity.ok(result)
    }



    /**
     * lbUuid
     */

    @PostMapping(value = ["lbUUID"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun createLbUuid(request: HttpServletRequest): ResponseEntity<Any?> {
        val name = request.getParameter("name")
        // val mobileToken = request.getParameter("mobileToken")
        val imei = request.getParameter("imei")
        val type = request.getParameter("type")
        if (name == null) {
            return ResponseEntity.badRequest().body("Query parameter 'name' must not be null.\n")
        }
        if (imei == null) {
            return ResponseEntity.badRequest().body("Query parameter 'imei' must not be null.\n")
        }
        if (type == null) {
            return ResponseEntity.badRequest().body("Query parameter 'type' must not be null.\n")
        }
        return try {
            val accountId = proxy.startTrackedFlow(::LeadgerBlocksAccountFlow, name , imei, type).returnValue.get()
            val currentDirectory = System.getProperty("user.dir")
            val fis = FileOutputStream(currentDirectory + "/src/main/resources/lbuuid.properties")
            val prop = Properties()
            prop.setProperty("lbuuid", "${accountId}")
            prop.store(fis,"save")
            ResponseEntity.status(HttpStatus.CREATED).body("uuid:${accountId}")
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }


    /**
     * pay-loan-direct
     */
    @PostMapping(value = ["payLoanDirect"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun payLoanDirect(request: HttpServletRequest): ResponseEntity<String> {
        val uuid = request.getParameter("uuid")
        val uuid1 = UUID.fromString(uuid)
        val amtToPay = request.getParameter("amtToPay").toInt()
        val cardNumber = request.getParameter("cardNumber").toInt()
       // val lbUUID = request.getParameter("lbUUID")
     //   val lbUUID1 = UUID.fromString(lbUUID)
        val currentDirectory = System.getProperty("user.dir")
        val fis = FileInputStream(currentDirectory + "/src/main/resources/lbuuid.properties")
        val properties = Properties()
        properties.load(fis)
        val lbUUID =  properties.getProperty("lbuuid")
        val lbUUID1 = UUID.fromString(lbUUID)
        if (uuid == null) {
            return ResponseEntity.badRequest().body("Query parameter 'uuid' must not be null.\n")
        }
        if (lbUUID == null) {
            return ResponseEntity.badRequest().body("Query parameter 'lbUUID' must not be null.\n")
        }
        if (amtToPay <= 0) {
            return ResponseEntity.badRequest().body("Query parameter ' amtToPay' must be non-negative..\n")
        }
        if (cardNumber == null) {
            return ResponseEntity.badRequest().body("Query parameter 'cardNumber' must not be null.\n")
        }
        return try {
            val payment = proxy.startTrackedFlow(::PayLoanDirectFlow, uuid1,lbUUID1, amtToPay, cardNumber).returnValue.get()
            val buuidLoanStateinfo=proxy.vaultQueryBy<LoanState>().states.filter { it.state.data.uuid.equals(uuid1)}
            println("loanstates=$buuidLoanStateinfo")
            val bBalLoanAmount= buuidLoanStateinfo.get(buuidLoanStateinfo.size-1).state.data.loanAmount
            ResponseEntity.status(HttpStatus.CREATED).body("payment:${payment}+balLoanAmt:${bBalLoanAmount}")
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }

    }


    @PostMapping(value = ["addEmail"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun addEmail (request: HttpServletRequest): ResponseEntity<String> {
        val email = request.getParameter("email")
        if (email == null){
            return ResponseEntity.badRequest().body("Email must not be empty")
        }
        val emailFile = System.getProperty("user.dir")+"/src/main/resources/email.csv"
        return try {
            File(emailFile).appendText(email)
            File(emailFile).appendText(System.getProperty("line.separator"))
            ResponseEntity.status(HttpStatus.CREATED).body("added:yes")
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }


    @PostMapping(value = ["verifyEmail"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun verifyEmail (request: HttpServletRequest): ResponseEntity<String> {
        val email = request.getParameter("email")
        var allowed = "no"
        var fileReader: BufferedReader? = null
        if (email == null){
            return ResponseEntity.badRequest().body("Email must not be empty")
        }
        return try {
            fileReader = BufferedReader(FileReader(System.getProperty("user.dir")+"/src/main/resources/email.csv"))
            var emailLine = fileReader.readLine()
            while (emailLine != null){
                if(emailLine.equals(email)){
                    allowed="yes"
                    break
                }
                emailLine =fileReader.readLine()
            }
            ResponseEntity.status(HttpStatus.CREATED).body("verified:${allowed}")
        } catch (ex: Throwable){
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }finally {
            try {
                fileReader!!.close()
            } catch (e: IOException) {
                println("Closing fileReader Error!")
                e.printStackTrace()
            }
        }
    }
}
