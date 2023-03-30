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

package org.ossreviewtoolkit.server.dao.repositories

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.containExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

import java.lang.IllegalArgumentException
import java.sql.SQLException

import org.ossreviewtoolkit.server.dao.test.DatabaseTestExtension
import org.ossreviewtoolkit.server.dao.test.Fixtures
import org.ossreviewtoolkit.server.model.RepositoryType
import org.ossreviewtoolkit.server.model.Secret
import org.ossreviewtoolkit.server.model.util.asPresent

class DaoSecretRepositoryTest : StringSpec() {
    private val secretRepository = DaoSecretRepository()

    private lateinit var fixtures: Fixtures
    private var organizationId = -1L
    private var productId = -1L
    private var repositoryId = -1L

    private val path = "https://secret-storage.com/ssh_host_rsa_key"
    private val name = "rsa certificate"
    private val description = "ssh rsa certificate"

    init {
        extension(
            DatabaseTestExtension {
                fixtures = Fixtures()
                organizationId = fixtures.organization.id
                productId = fixtures.product.id
                repositoryId = fixtures.repository.id
            }
        )

        "create should create an entry in the database" {
            val name = "secret1"
            val secret = createSecret(name, organizationId, null, null)

            val dbEntry = secretRepository.getByOrganizationIdAndName(organizationId, name)

            dbEntry.shouldNotBeNull()
            dbEntry shouldBe secret
        }

        "update should update an organization secret in the database" {
            val name = "secret2"
            val secret = createSecret(name, organizationId, null, null)

            secretRepository.updateForOrganizationAndName(
                organizationId,
                name,
                description.asPresent()
            )

            val dbEntry = secretRepository.getByOrganizationIdAndName(organizationId, name)

            dbEntry.shouldNotBeNull()
            dbEntry shouldBe Secret(
                secret.id,
                path + name,
                name,
                description,
                fixtures.organization,
                null,
                null
            )
        }

        "update should update a product secret in the database" {
            val name = "secret3"
            val secret = createSecret(name, null, productId, null)

            secretRepository.updateForProductAndName(
                productId,
                name,
                description.asPresent()
            )

            val dbEntry = secretRepository.getByProductIdAndName(productId, name)

            dbEntry.shouldNotBeNull()
            dbEntry shouldBe Secret(
                secret.id,
                path + name,
                name,
                description,
                null,
                fixtures.product,
                null
            )
        }

        "update should update a repository secret in the database" {
            val name = "secret2"
            val secret = createSecret(name, null, null, repositoryId)

            secretRepository.updateForRepositoryAndName(
                repositoryId,
                name,
                description.asPresent()
            )

            val dbEntry = secretRepository.getByRepositoryIdAndName(repositoryId, name)

            dbEntry.shouldNotBeNull()
            dbEntry shouldBe Secret(
                secret.id,
                path + name,
                name,
                description,
                null,
                null,
                fixtures.repository
            )
        }

        "delete should delete the database entry" {
            val name = "secret3"
            createSecret(name, null, null, repositoryId)

            secretRepository.deleteForRepositoryAndName(repositoryId, name)

            secretRepository.listForRepository(repositoryId) shouldBe emptyList()
        }

        "adding an ambiguous secret should cause an exception" {
            shouldThrow<SQLException> {
                secretRepository.create(path, name, description, organizationId, productId, repositoryId)
            }
        }

        "Reading all secrets of specific type" should {
            "return all stored results for a specific organization" {
                val organizationSecret1 = createSecret("secret4", organizationId, null, null)
                val organizationSecret2 = createSecret("secret5", organizationId, null, null)
                createSecret(
                    "secret6",
                    fixtures.createOrganization("extra organization", "org description").id,
                    null,
                    null
                )
                createSecret("productSecret1", null, productId, null)
                createSecret("repositorySecret1", null, null, repositoryId)

                secretRepository.listForOrganization(organizationId) should containExactlyInAnyOrder(
                    organizationSecret1,
                    organizationSecret2
                )
            }

            "return all stored results for a specific product" {
                val productSecret1 = createSecret("secret7", null, productId, null)
                val productSecret2 = createSecret("secret8", null, productId, null)
                createSecret(
                    "secret9",
                    null,
                    fixtures.createProduct("extra product", "prod description", fixtures.organization.id).id,
                    null
                )
                createSecret("organizationSecret1", organizationId, null, null)
                createSecret("repositorySecret2", null, null, repositoryId)

                secretRepository.listForProduct(productId) should containExactlyInAnyOrder(
                    productSecret1,
                    productSecret2
                )
            }

            "return all stored results for a specific repository" {
                val repositorySecret1 = createSecret("secret10", null, null, repositoryId)
                val repositorySecret2 = createSecret("secret11", null, null, repositoryId)
                createSecret(
                    "secret12",
                    null,
                    null,
                    fixtures.createRepository(RepositoryType.GIT, "repo description", fixtures.product.id).id
                )
                createSecret("organizationSecret2", organizationId, null, null)
                createSecret("productSecret2", organizationId, null, null)

                secretRepository.listForRepository(repositoryId) should containExactlyInAnyOrder(
                    repositorySecret1,
                    repositorySecret2
                )
            }
        }
    }

    private fun createSecret(
        name: String,
        organizationId: Long?,
        productId: Long?,
        repositoryId: Long?
    ) = when {
        organizationId != null -> secretRepository.create("$path$name", name, description, organizationId, null, null)
        productId != null -> secretRepository.create("$path$name", name, description, null, productId, null)
        repositoryId != null -> secretRepository.create("$path$name", name, description, null, null, repositoryId)
        else -> throw IllegalArgumentException("The secret wasn't created")
    }
}
