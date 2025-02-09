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

package org.kryptokrona.sdk.service.client

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.kryptokrona.sdk.service.common.HttpClient
import org.kryptokrona.sdk.service.model.Service
import org.kryptokrona.sdk.service.model.request.StatusRequest
import org.kryptokrona.sdk.service.model.request.key.GetSpendKeysRequest
import org.kryptokrona.sdk.service.model.request.key.GetViewKeyRequest
import org.kryptokrona.sdk.service.model.request.mnemonic.GetMnemonicSeedRequest
import org.kryptokrona.sdk.service.model.response.StatusResponse
import org.kryptokrona.sdk.service.model.response.key.GetSpendKeysResponse
import org.kryptokrona.sdk.service.model.response.key.GetViewKeyResponse
import org.kryptokrona.sdk.service.model.response.mnemonic.GetMnemonicSeedResponse
import org.slf4j.LoggerFactory
import java.nio.channels.UnresolvedAddressException

/**
 * Key Service Client
 *
 * @author Marcus Cvjeticanin
 * @since 0.3.0
 * @param service The service to connect to.
 */
class KeyServiceClient(private val service: Service) {

    private val logger = LoggerFactory.getLogger("KeyServiceClient")

    suspend fun getSpendKeys(getSpendKeysRequest: GetSpendKeysRequest): GetSpendKeysResponse? {
        val jsonBody = Json.encodeToString(getSpendKeysRequest)

        val builder = HttpRequestBuilder().apply {
            method = HttpMethod.Post
            service.ssl.let {
                if (it) {
                    url.takeFrom("https://${service.hostName}:${service.port}/json_rpc")
                } else {
                    url.takeFrom("http://${service.hostName}:${service.port}/json_rpc")
                }
            }
            contentType(ContentType.Application.Json)
            headers {
                append("Content-Length", jsonBody.length.toString())
            }
            setBody(jsonBody)
        }

        try {
            return HttpClient.client.post(builder).body<GetSpendKeysResponse>()
        } catch (e: HttpRequestTimeoutException) {
            logger.error("Error getting spend keys. Could not reach the server.", e)
        } catch (e: UnresolvedAddressException) {
            logger.error("Error getting spend keys. Could not resolve the address.", e)
        } catch (e: JsonConvertException) {
            logger.error("Error getting spend keys. Could not parse the response.", e)
        }

        return null
    }

    /**
     * Get the view key from the wallet.
     *
     * @author Marcus Cvjeticanin
     * @since 0.3.0
     * @param getViewKeyRequest The request to send to the wallet.
     * @return The response from the wallet.
     */
    suspend fun getViewKey(getViewKeyRequest: GetViewKeyRequest): GetViewKeyResponse? {
        val jsonBody = Json.encodeToString(getViewKeyRequest)

        val builder = HttpRequestBuilder().apply {
            method = HttpMethod.Post
            service.ssl.let {
                if (it) {
                    url.takeFrom("https://${service.hostName}:${service.port}/json_rpc")
                } else {
                    url.takeFrom("http://${service.hostName}:${service.port}/json_rpc")
                }
            }
            contentType(ContentType.Application.Json)
            headers {
                append("Content-Length", jsonBody.length.toString())
            }
            setBody(jsonBody)
        }

        try {
            return HttpClient.client.post(builder).body<GetViewKeyResponse>()
        } catch (e: HttpRequestTimeoutException) {
            logger.error("Error getting view key. Could not reach the server.", e)
        } catch (e: UnresolvedAddressException) {
            logger.error("Error getting view key. Could not resolve the address.", e)
        } catch (e: JsonConvertException) {
            logger.error("Error getting view key. Could not parse the response.", e)
        }

        return null
    }

    /**
     * Get the mnemonic seed of the wallet.
     *
     * @author Marcus Cvjeticanin
     * @since 0.3.0
     * @param getMnemonicSeedRequest The request.
     * @return The response.
     */
    suspend fun getMnemonicSeed(getMnemonicSeedRequest: GetMnemonicSeedRequest): GetMnemonicSeedResponse? {
        val jsonBody = Json.encodeToString(getMnemonicSeedRequest)

        val builder = HttpRequestBuilder().apply {
            method = HttpMethod.Post
            service.ssl.let {
                if (it) {
                    url.takeFrom("https://${service.hostName}:${service.port}/json_rpc")
                } else {
                    url.takeFrom("http://${service.hostName}:${service.port}/json_rpc")
                }
            }
            contentType(ContentType.Application.Json)
            headers {
                append("Content-Length", jsonBody.length.toString())
            }
            setBody(jsonBody)
        }

        try {
            return HttpClient.client.post(builder).body<GetMnemonicSeedResponse>()
        } catch (e: HttpRequestTimeoutException) {
            logger.error("Error getting mnemonic seed. Could not reach the server.", e)
        } catch (e: UnresolvedAddressException) {
            logger.error("Error getting mnemonic seed. Could not resolve the address.", e)
        } catch (e: JsonConvertException) {
            logger.error("Error getting mnemonic seed. Could not parse the response.", e)
        }

        return null
    }
}
