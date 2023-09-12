/*
 * Copyright (C) 2023 The ORT Project Authors (See <https://github.com/oss-review-toolkit/ort-server/blob/main/NOTICE>)
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

package org.ossreviewtoolkit.server.workers.common.common

import com.fasterxml.jackson.databind.exc.MismatchedInputException

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe

import io.mockk.every
import io.mockk.mockk

import org.ossreviewtoolkit.server.config.ConfigException
import org.ossreviewtoolkit.server.config.ConfigManager
import org.ossreviewtoolkit.server.config.Path
import org.ossreviewtoolkit.server.workers.common.readConfigFile
import org.ossreviewtoolkit.server.workers.common.readConfigFileWithDefault

class ExtensionsTest : WordSpec({
    data class ConfigClass(val name: String, val value: String)

    val path = "path"
    val defaultPath = "defaultPath"
    val fallbackValue = ConfigClass(name = "fallback", value = "value")

    val configFile = ConfigClass("config", "value")

    val configFileYaml = """
        name: "config"
        value: "value"
    """.trimIndent()

    val invalidConfigFileYaml = "invalid"

    val configException = ConfigException("message", null)

    "readConfigFileWithDefault" should {
        "deserialize the file at path if path is not null" {
            val configManager = mockk<ConfigManager> {
                every { getFile(any(), Path(path)) } returns configFileYaml.byteInputStream()
            }

            configManager.readConfigFileWithDefault(path, defaultPath, fallbackValue) shouldBe configFile
        }

        "throw an exception if the file at path cannot be read" {
            val configManager = mockk<ConfigManager> {
                every { getFile(any(), Path(path)) } throws configException
            }

            shouldThrow<ConfigException> {
                configManager.readConfigFileWithDefault(path, defaultPath, fallbackValue)
            } shouldBe configException
        }

        "throw an exception if the file at path cannot be deserialized" {
            val configManager = mockk<ConfigManager> {
                every { getFile(any(), Path(path)) } returns invalidConfigFileYaml.byteInputStream()
            }

            shouldThrow<MismatchedInputException> {
                configManager.readConfigFileWithDefault(path, defaultPath, fallbackValue)
            }
        }

        "deserialize the file at the default path if path is null" {
            val configManager = mockk<ConfigManager> {
                every { getFile(any(), Path(defaultPath)) } returns configFileYaml.byteInputStream()
            }

            configManager.readConfigFileWithDefault(null, defaultPath, fallbackValue) shouldBe configFile
        }

        "return the fallback value if the file at default path cannot be read" {
            val configManager = mockk<ConfigManager> {
                every { getFile(any(), Path(defaultPath)) } throws configException
            }

            configManager.readConfigFileWithDefault(null, defaultPath, fallbackValue) shouldBe fallbackValue
        }

        "throw an exception if the file at default path cannot be deserialized" {
            val configManager = mockk<ConfigManager> {
                every { getFile(any(), Path(defaultPath)) } returns invalidConfigFileYaml.byteInputStream()
            }

            shouldThrow<MismatchedInputException> {
                configManager.readConfigFileWithDefault(null, defaultPath, fallbackValue) shouldBe fallbackValue
            }
        }
    }

    "readConfigFile" should {
        "deserialize the config file" {
            val configManager = mockk<ConfigManager> {
                every { getFile(any(), Path(path)) } returns configFileYaml.byteInputStream()
            }

            configManager.readConfigFile<ConfigClass>(path) shouldBe configFile
        }

        "call the exception handler if a ConfigException occurs" {
            val configManager = mockk<ConfigManager> {
                every { getFile(any(), Path(path)) } throws configException
            }

            var capturedException: ConfigException? = null
            configManager.readConfigFile("path") { capturedException = it }

            capturedException shouldBe configException
        }

        "throw an exception if the config file cannot be deserialized" {
            val configManager = mockk<ConfigManager> {
                every { getFile(any(), Path(path)) } returns invalidConfigFileYaml.byteInputStream()
            }

            shouldThrow<MismatchedInputException> {
                configManager.readConfigFile<ConfigClass>("path") shouldBe configFile
            }
        }
    }
})
