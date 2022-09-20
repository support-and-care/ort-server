/*
 * Copyright (C) 2022 The ORT Project Authors (See <https://github.com/oss-review-toolkit/ort-server/blob/main/NOTICE>)
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

import org.jetbrains.exposed.exceptions.ExposedSQLException

import org.ossreviewtoolkit.server.dao.PostgresErrorCodes
import org.ossreviewtoolkit.server.dao.UniqueConstraintException
import org.ossreviewtoolkit.server.dao.blockingQuery
import org.ossreviewtoolkit.server.dao.tables.OrganizationDao
import org.ossreviewtoolkit.server.dao.tables.ProductDao
import org.ossreviewtoolkit.server.dao.tables.ProductsTable
import org.ossreviewtoolkit.server.model.repositories.ProductRepository
import org.ossreviewtoolkit.server.model.util.OptionalValue

class DaoProductRepository : ProductRepository {
    override fun create(name: String, description: String?, organizationId: Long) = blockingQuery {
        ProductDao.new {
            this.name = name
            this.description = description
            this.organization = OrganizationDao[organizationId]
        }.mapToModel()
    }.onFailure {
        if (it is ExposedSQLException) {
            when (it.sqlState) {
                PostgresErrorCodes.UNIQUE_CONSTRAINT_VIOLATION.value -> {
                    throw UniqueConstraintException(
                        "Failed to create product '$name', as a product with this name already exists in the " +
                                "organization '$organizationId'.",
                        it
                    )
                }
            }
        }

        throw it
    }.getOrThrow()

    override fun get(id: Long) = blockingQuery { ProductDao[id].mapToModel() }.getOrNull()

    override fun listForOrganization(organizationId: Long) = blockingQuery {
        ProductDao.find { ProductsTable.organization eq organizationId }.map { it.mapToModel() }
    }.getOrDefault(emptyList())

    override fun update(id: Long, name: OptionalValue<String>, description: OptionalValue<String?>) = blockingQuery {
        val product = ProductDao[id]

        name.ifPresent { product.name = it }
        description.ifPresent { product.description = it }

        ProductDao[id].mapToModel()
    }.getOrThrow()

    override fun delete(id: Long) = blockingQuery { ProductDao[id].delete() }.getOrThrow()
}
