package com.ledgerblocks.poc.clent

import net.corda.client.rpc.CordaRPCClient
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.loggerFor

/**
 * Connects to a Corda node via RPC and performs RPC operations on the node.
 *
 * The RPC connection is configured using command line arguments.
 */
fun main(args: Array<String>) = Client().main(args)

public class Client {
    companion object {
        val logger = loggerFor<Client>()
    }

    fun main(args: Array<String>) {
        // Create an RPC connection to the node.
        require(args.size == 2) { "Usage: Client <node address> <rpc username> <rpc password>" }

        // Create a connection to PartyA and PartyB.
        val (issuerProxy, receiverProxy) = args.map { arg ->
            val nodeAddress = NetworkHostAndPort.parse(arg)
            val client = CordaRPCClient(nodeAddress)
            client.start("user1", "test").proxy
        }

        val receiverIdentity = receiverProxy.nodeInfo().legalIdentities.first()

    }
}