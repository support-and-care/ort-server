/*
 * Copyright (C) 2023 The ORT Server Authors (See <https://github.com/eclipse-apoapsis/ort-server/blob/main/NOTICE>)
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

package org.eclipse.apoapsis.ortserver.dao.repositories.resolvedconfiguration

import org.eclipse.apoapsis.ortserver.dao.repositories.repositoryconfiguration.PackageConfigurationsTable

import org.jetbrains.exposed.sql.Table

/**
 * An intermediate table to store references from [ResolvedConfigurationsTable] and [PackageConfigurationsTable].
 */
object ResolvedConfigurationsPackageConfigurationsTable : Table("resolved_configurations_package_configurations") {
    val resolvedConfigurationId = reference("resolved_configuration_id", ResolvedConfigurationsTable)
    val packageConfigurationId = reference("package_configuration_id", PackageConfigurationsTable)

    override val primaryKey: PrimaryKey
        get() = PrimaryKey(resolvedConfigurationId, packageConfigurationId, name = "${tableName}_pkey")
}
