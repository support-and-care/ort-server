/*
 * Copyright (C) 2022 The ORT Server Authors (See <https://github.com/eclipse-apoapsis/ort-server/blob/main/NOTICE>)
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

package org.eclipse.apoapsis.ortserver.dao.repositories.advisorrun

import org.eclipse.apoapsis.ortserver.model.PluginConfig
import org.eclipse.apoapsis.ortserver.model.runs.advisor.AdvisorConfiguration

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

/**
 * A table to represent an advisor configuration.
 */
object AdvisorConfigurationsTable : LongIdTable("advisor_configurations") {
    val advisorRunId = reference("advisor_run_id", AdvisorRunsTable)
}

class AdvisorConfigurationDao(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<AdvisorConfigurationDao>(AdvisorConfigurationsTable)

    var advisorRun by AdvisorRunDao referencedOn AdvisorConfigurationsTable.advisorRunId

    var options by AdvisorConfigurationOptionDao via AdvisorConfigurationsOptionsTable
    var secrets by AdvisorConfigurationSecretDao via AdvisorConfigurationsSecretsTable

    fun mapToModel(): AdvisorConfiguration {
        val optionsByAdvisor = options.groupBy { it.advisor }
            .mapValues { (_, value) -> value.associate { it.option to it.value } }
        val secretsByAdvisor = secrets.groupBy { it.advisor }
            .mapValues { (_, value) -> value.associate { it.secret to it.value } }

        val config = buildMap {
            (optionsByAdvisor.keys + secretsByAdvisor.keys).forEach { advisor ->
                val pluginConfig = PluginConfig(
                    options = optionsByAdvisor[advisor].orEmpty(),
                    secrets = secretsByAdvisor[advisor].orEmpty()
                )

                put(advisor, pluginConfig)
            }
        }

        return AdvisorConfiguration(config)
    }
}
