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

package org.eclipse.apoapsis.ortserver.core.api

import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.patch
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.github.smiley4.ktorswaggerui.dsl.routing.put

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route

import org.eclipse.apoapsis.ortserver.api.v1.mapping.mapToApi
import org.eclipse.apoapsis.ortserver.api.v1.mapping.mapToApiSummary
import org.eclipse.apoapsis.ortserver.api.v1.mapping.mapToModel
import org.eclipse.apoapsis.ortserver.api.v1.model.CreateOrtRun
import org.eclipse.apoapsis.ortserver.api.v1.model.CreateSecret
import org.eclipse.apoapsis.ortserver.api.v1.model.JobSummaries
import org.eclipse.apoapsis.ortserver.api.v1.model.Jobs
import org.eclipse.apoapsis.ortserver.api.v1.model.SortDirection
import org.eclipse.apoapsis.ortserver.api.v1.model.SortProperty
import org.eclipse.apoapsis.ortserver.api.v1.model.UpdateRepository
import org.eclipse.apoapsis.ortserver.api.v1.model.UpdateSecret
import org.eclipse.apoapsis.ortserver.api.v1.model.Username
import org.eclipse.apoapsis.ortserver.core.apiDocs.deleteRepositoryById
import org.eclipse.apoapsis.ortserver.core.apiDocs.deleteSecretByRepositoryIdAndName
import org.eclipse.apoapsis.ortserver.core.apiDocs.deleteUserFromRepositoryGroup
import org.eclipse.apoapsis.ortserver.core.apiDocs.getOrtRunByIndex
import org.eclipse.apoapsis.ortserver.core.apiDocs.getOrtRuns
import org.eclipse.apoapsis.ortserver.core.apiDocs.getRepositoryById
import org.eclipse.apoapsis.ortserver.core.apiDocs.getSecretByRepositoryIdAndName
import org.eclipse.apoapsis.ortserver.core.apiDocs.getSecretsByRepositoryId
import org.eclipse.apoapsis.ortserver.core.apiDocs.patchRepositoryById
import org.eclipse.apoapsis.ortserver.core.apiDocs.patchSecretByRepositoryIdAndName
import org.eclipse.apoapsis.ortserver.core.apiDocs.postOrtRun
import org.eclipse.apoapsis.ortserver.core.apiDocs.postSecretForRepository
import org.eclipse.apoapsis.ortserver.core.apiDocs.putUserToRepositoryGroup
import org.eclipse.apoapsis.ortserver.core.authorization.requirePermission
import org.eclipse.apoapsis.ortserver.core.services.OrchestratorService
import org.eclipse.apoapsis.ortserver.core.utils.pagingOptions
import org.eclipse.apoapsis.ortserver.core.utils.requireIdParameter
import org.eclipse.apoapsis.ortserver.core.utils.requireParameter
import org.eclipse.apoapsis.ortserver.model.Secret
import org.eclipse.apoapsis.ortserver.model.authorization.RepositoryPermission
import org.eclipse.apoapsis.ortserver.services.RepositoryService
import org.eclipse.apoapsis.ortserver.services.SecretService

import org.koin.ktor.ext.inject

fun Route.repositories() = route("repositories/{repositoryId}") {
    val orchestratorService by inject<OrchestratorService>()
    val repositoryService by inject<RepositoryService>()
    val secretService by inject<SecretService>()

    get(getRepositoryById) { _ ->
        requirePermission(RepositoryPermission.READ)

        val id = call.requireIdParameter("repositoryId")

        repositoryService.getRepository(id)?.let { call.respond(HttpStatusCode.OK, it.mapToApi()) }
            ?: call.respond(HttpStatusCode.NotFound)
    }

    patch(patchRepositoryById) { _ ->
        requirePermission(RepositoryPermission.WRITE)

        val id = call.requireIdParameter("repositoryId")
        val updateRepository = call.receive<UpdateRepository>()

        val updatedRepository = repositoryService.updateRepository(
            id,
            updateRepository.type.mapToModel { it.mapToModel() },
            updateRepository.url.mapToModel()
        )

        call.respond(HttpStatusCode.OK, updatedRepository.mapToApi())
    }

    delete(deleteRepositoryById) {
        requirePermission(RepositoryPermission.DELETE)

        val id = call.requireIdParameter("repositoryId")

        repositoryService.deleteRepository(id)

        call.respond(HttpStatusCode.NoContent)
    }

    route("runs") {
        get(getOrtRuns) { _ ->
            requirePermission(RepositoryPermission.READ_ORT_RUNS)

            val repositoryId = call.requireIdParameter("repositoryId")
            val pagingOptions = call.pagingOptions(SortProperty("index", SortDirection.ASCENDING))

            val jobsForOrtRuns = repositoryService.getOrtRuns(repositoryId, pagingOptions.mapToModel()).mapData {
                    val jobSummaries = repositoryService.getJobs(repositoryId, it.index)?.mapToApiSummary()
                        ?: JobSummaries()
                    it.mapToApiSummary(jobSummaries)
            }

            val pagedResponse = jobsForOrtRuns.mapToApi { it }

            call.respond(HttpStatusCode.OK, pagedResponse)
        }

        post(postOrtRun) {
            requirePermission(RepositoryPermission.TRIGGER_ORT_RUN)

            val repositoryId = call.requireIdParameter("repositoryId")
            val createOrtRun = call.receive<CreateOrtRun>()

            call.respond(
                HttpStatusCode.Created,
                orchestratorService.createOrtRun(
                    repositoryId,
                    createOrtRun.revision,
                    createOrtRun.path,
                    createOrtRun.jobConfigs.mapToModel(),
                    createOrtRun.jobConfigContext,
                    createOrtRun.labels
                ).mapToApi(Jobs())
            )
        }

        route("{ortRunIndex}") {
            get(getOrtRunByIndex) { _ ->
                requirePermission(RepositoryPermission.READ_ORT_RUNS)

                val repositoryId = call.requireIdParameter("repositoryId")
                val ortRunIndex = call.requireIdParameter("ortRunIndex")

                repositoryService.getOrtRun(repositoryId, ortRunIndex)?.let {
                    repositoryService.getJobs(repositoryId, ortRunIndex)?.let { jobs ->
                        call.respond(HttpStatusCode.OK, it.mapToApi(jobs.mapToApi()))
                    }
                } ?: call.respond(HttpStatusCode.NotFound)
            }
        }
    }

    route("secrets") {
        get(getSecretsByRepositoryId) { _ ->
            requirePermission(RepositoryPermission.READ)

            val repositoryId = call.requireIdParameter("repositoryId")
            val pagingOptions = call.pagingOptions(SortProperty("name", SortDirection.ASCENDING))

            val secretsForRepository = secretService.listForRepository(repositoryId, pagingOptions.mapToModel())

            val pagedResponse = secretsForRepository.mapToApi(Secret::mapToApi)

            call.respond(HttpStatusCode.OK, pagedResponse)
        }

        route("{secretName}") {
            get(getSecretByRepositoryIdAndName) { _ ->
                requirePermission(RepositoryPermission.READ)

                val repositoryId = call.requireIdParameter("repositoryId")
                val secretName = call.requireParameter("secretName")

                secretService.getSecretByRepositoryIdAndName(repositoryId, secretName)
                    ?.let { call.respond(HttpStatusCode.OK, it.mapToApi()) }
                    ?: call.respond(HttpStatusCode.NotFound)
            }

            patch(patchSecretByRepositoryIdAndName) {
                requirePermission(RepositoryPermission.WRITE_SECRETS)

                val repositoryId = call.requireIdParameter("repositoryId")
                val secretName = call.requireParameter("secretName")
                val updateSecret = call.receive<UpdateSecret>()

                call.respond(
                    HttpStatusCode.OK,
                    secretService.updateSecretByRepositoryAndName(
                        repositoryId,
                        secretName,
                        updateSecret.value.mapToModel(),
                        updateSecret.description.mapToModel()
                    ).mapToApi()
                )
            }

            delete(deleteSecretByRepositoryIdAndName) {
                requirePermission(RepositoryPermission.WRITE_SECRETS)

                val repositoryId = call.requireIdParameter("repositoryId")
                val secretName = call.requireParameter("secretName")

                secretService.deleteSecretByRepositoryAndName(repositoryId, secretName)

                call.respond(HttpStatusCode.NoContent)
            }
        }

        post(postSecretForRepository) {
            requirePermission(RepositoryPermission.WRITE_SECRETS)

            val repositoryId = call.requireIdParameter("repositoryId")
            val createSecret = call.receive<CreateSecret>()

            call.respond(
                HttpStatusCode.Created,
                secretService.createSecret(
                    createSecret.name,
                    createSecret.value,
                    createSecret.description,
                    null,
                    null,
                    repositoryId
                ).mapToApi()
            )
        }
    }

    route("groups") {
        // Instead of identifying arbitrary groups with a groupId, there are only 3 groups with fixed
        // groupId "readers", "writers" or "admins".
        route("{groupId}") {
            put(putUserToRepositoryGroup) {
                requirePermission(RepositoryPermission.WRITE)

                val user = call.receive<Username>()
                val repositoryId = call.requireIdParameter("repositoryId")
                val groupId = call.requireParameter("groupId")

                repositoryService.addUserToGroup(user.username, repositoryId, groupId)
                call.respond(HttpStatusCode.NoContent)
            }

            delete(deleteUserFromRepositoryGroup) {
                requirePermission(RepositoryPermission.WRITE)

                val user = call.receive<Username>()
                val repositoryId = call.requireIdParameter("repositoryId")
                val groupId = call.requireParameter("groupId")

                repositoryService.removeUserFromGroup(user.username, repositoryId, groupId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
