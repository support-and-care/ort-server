/*
 * Copyright (C) 2024 The ORT Server Authors (See <https://github.com/eclipse-apoapsis/ort-server/blob/main/NOTICE>)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package org.eclipse.apoapsis.ortserver.workers.reporter

import java.security.SecureRandom
import java.util.Base64

import kotlin.time.Duration

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * A data class representing a link with a token that can be used to download a report without authentication.
 */
data class ReportDownloadLink(
    /** The link for accessing the report. */
    val downloadLink: String,

    /** The time when the link expires. */
    val expirationTime: Instant
)

/**
 * A helper class to generate links based on tokens that can be used to download reports without authentication.
 * Basically, a token is an unguessable random string that is assigned to a report and stored in the database together
 * with an expiration time. The prefix of the link can be configured. When a user provides a link with such a token,
 * the corresponding report can be looked up, and the validity can be checked.
 *
 * The support for report download links can be turned off by setting the token length to less than or equal to zero.
 * Then empty download link strings with an expiration time in the past are generated.
 */
internal class ReportDownloadLinkGenerator(
    /**
     * The prefix to be used for download links. This should contain the protocol and the host. This generator adds
     * the part starting with "/api/vX".
     */
    val linkPrefix: String,

    /**
     * The length of the token (in bytes) generated by this class. Note that the string representation of the tokens
     * typically has a different length, since the bytes are base64 encoded.
     */
    val tokenLength: Int,

    /** The time how a token is valid after it has been generated. */
    val validityTime: Duration,

    /** The clock to calculate the expiration time. */
    private val clock: Clock = Clock.System
) {
    companion object {
        /**
         * A link to be returned if no token length is provided and thus the token mechanism is disabled.
         */
        private val disabledLink = ReportDownloadLink("", Instant.fromEpochMilliseconds(0))
    }

    /** The object to generate random bytes. */
    private val random = SecureRandom()

    /**
     * Return a new [ReportDownloadLink] object for the given [runId] that corresponds to the configuration of this
     * instance.
     */
    fun generateLink(runId: Long): ReportDownloadLink {
        if (tokenLength <= 0) return disabledLink

        val tokenBytes = ByteArray(tokenLength)
        random.nextBytes(tokenBytes)
        val tokenString = Base64.getEncoder().encodeToString(tokenBytes)

        val link = "$linkPrefix/api/v1/runs/$runId/downloads/report/$tokenString"
        return ReportDownloadLink(link, clock.now() + validityTime)
    }
}
