// Copyright (c) 2022-2023, The Kryptokrona Developers
//
// Written by Marcus Cvjeticanin
//
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without modification, are
// permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
//
// 2. Redistributions in binary form must reproduce the above copyright notice, this list
//    of conditions and the following disclaimer in the documentation and/or other
//    materials provided with the distribution.
//
// 3. Neither the name of the copyright holder nor the names of its contributors may be
//    used to endorse or promote products derived from this software without specific
//    prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
// EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
// MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
// THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
// PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
// THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package org.kryptokrona.sdk.core.service

import kotlinx.coroutines.*
import org.kryptokrona.sdk.crypto.util.convertHexToBytes
import org.kryptokrona.sdk.http.client.BlockClient
import org.kryptokrona.sdk.http.client.WalletClient
import org.kryptokrona.sdk.http.model.request.block.BlockDetailsByHeightRequest
import org.kryptokrona.sdk.http.model.response.walletsyncdata.WalletSyncData
import org.kryptokrona.sdk.http.model.request.wallet.WalletSyncDataRequest
import org.kryptokrona.sdk.http.model.response.node.Info
import org.kryptokrona.sdk.http.model.response.walletsyncdata.Block
import org.kryptokrona.sdk.http.model.response.walletsyncdata.Transaction
import org.kryptokrona.sdk.http.model.response.walletsyncdata.TransactionInput
import org.kryptokrona.sdk.util.config.Config
import org.kryptokrona.sdk.util.model.node.Node
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.LocalDateTime.now

/**
 * WalletService class.
 *
 * @author Marcus Cvjeticanin
 * @since 0.1.1
 * @param node The node that the wallet service is connected to.
 */
class WalletService(node: Node) {

    private val logger = LoggerFactory.getLogger("WalletService")

    private val walletClient = WalletClient(node)

    private val blockClient = BlockClient(node)

    private val nodeService = NodeService(node)

    private var syncJob: Job = Job()

    private var nodeInfo: Info? = null

    private var startHeight: Long = 0

    private var walletHeight: Long = 0

    private var checkpoints: MutableList<String> = mutableListOf()

    /**
     * Stored blocks for later processing
     */
    private var storedBlocks = mutableListOf<Block>()

    /**
     * Whether we are already downloading a chunk of blocks
     */
    private var fetchingBlocks: Boolean = false

    private var shouldSleep: Boolean = false

    private var lastDownloadedBlocks: LocalDateTime = now()

    fun getNodeInfo() = nodeInfo

    fun getStoredBlocks() = storedBlocks

    fun setStartHeight(height: Long) {
        startHeight = height
    }

    /**
     * Starts the sync process.
     */
    suspend fun startSync() = coroutineScope {
        logger.info("Starting sync process...")
        walletHeight = startHeight

        // get the first hash for the checkpoints
        val blockDetails = blockClient.getBlockDetailsByHeight(BlockDetailsByHeightRequest(startHeight))
        blockDetails?.block?.hash?.let { checkpoints.plusAssign(it) }

        var lastCheckPoint = listOf(checkpoints.last())
        val requestData = WalletSyncDataRequest(blockHashCheckpoints = lastCheckPoint)
        val walletSyncData = getSyncData(requestData)
        walletSyncData.let { wsd ->
            if (wsd != null) {
                checkpoints.plusAssign(wsd.items.last().blockHash)
                walletHeight += wsd.items.size.toLong()
            }
        }

        syncJob = launch {
            launch(Dispatchers.IO) {
                while(isActive) {
                    logger.debug("Syncing blocks...")

                    nodeInfo?.height?.let {
                        if (walletHeight < it) {
                            lastCheckPoint = listOf(checkpoints.last())
                            val data = WalletSyncDataRequest(blockHashCheckpoints = lastCheckPoint)
                            val syncData = getSyncData(data)

                            syncData?.let { sd ->
                                if (sd.items.isNotEmpty()) {
                                    checkpoints.plusAssign(sd.items.last().blockHash)
                                    checkpoints = checkpoints.distinct().toMutableList() // removing duplicates
                                    walletHeight += sd.items.size.toLong()

                                    // add new blocks to stored blocks
                                    sd.items.forEach { block ->
                                        storedBlocks.add(block)
                                    }
                                    logger.info("Fetched ${sd.items.size} block(s)")
                                }
                            }

                            logger.debug("Wallet height: $walletHeight")
                        }
                    }

                    delay(Config.SYNC_THREAD_INTERVAL)
                }
            }

            launch(Dispatchers.IO) {
                while(isActive) {
                    nodeInfo = nodeService.getNodeInfo()
                    logger.info("Node height: ${nodeInfo?.height}")
                    delay(Config.NODE_UPDATE_INTERVAL)
                }
            }
        }

        syncJob.children.forEach { it.join() }
    }

    /**
     * Stops the sync process.
     */
    suspend fun stopSync() = coroutineScope {
        syncJob.children.forEach { it.cancel() }

        logger.info("Stopping sync process...")
    }

    /**
     * Gets the wallet sync data.
     *
     * @return The wallet sync data.
     */
    private suspend fun getSyncData(walletSyncDataRequest: WalletSyncDataRequest): WalletSyncData? {
        logger.debug("Getting wallet sync data...")
        return walletClient.getWalletSyncData(walletSyncDataRequest)
    }

    suspend fun processBlocks() {
        logger.info("Processing blocks...")

        storedBlocks.forEach { block ->
            block.transactions.forEach { transaction ->
                checkTransactionOutputs(transaction, block.blockHeight)
            }

            // update wallet scan height
            walletHeight = block.blockHeight

            // remove the checked block from storedBlocks
            storedBlocks.remove(block)
        }
    }

    private fun checkTransactionOutputs(transaction: Transaction, blockHeight: Long) {
        val privateViewKey = "b72c00a54aef2ee122ceeb1358c46357512d74846887eaf6bd5141556a797c01"
        val publicSpendKey = "57b6a1553b053fd53b421a6ff1ab0092c9df7c2ad66fa4b28f9fe840905c7a9f"

        val pubSpend = convertHexToBytes("")
        val privView = convertHexToBytes("")
        val txPubKey = convertHexToBytes("")

        val inputs = mutableListOf<TransactionInput>()
        val derivation = ""

        transaction.outputs.forEach { output ->
            val key = output.key
            val byteKey = convertHexToBytes("")
            val amount = output.amount

            val derivedSpendKey = ""

            // if pub_spend != derived_spend_key
                // continue

            // this transaction contains outputs that belong to us. Create the key image and transaction input and save it.
            // let (key_image, private_ephemeral) = get_key_image_from_output(&derivation, index as u64, &pub_spend);

            // this is not spent yet. We just got it :)
            val spendHeight = 0

            //Construct our transaction input, there may be more inputs from this transactions
            /*val txInput = TransactionInput(
                keyImage = "",
                amount = amount,

            )*/

             // yellow!("Transaction found \n {:#?}", &tx_input);

            // inputs.add(txInput)
        }

    }


}