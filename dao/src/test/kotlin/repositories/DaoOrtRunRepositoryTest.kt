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

package org.ossreviewtoolkit.server.dao.test.repositories

import io.kotest.core.test.TestCase
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

import org.ossreviewtoolkit.server.dao.connect
import org.ossreviewtoolkit.server.dao.repositories.DaoOrtRunRepository
import org.ossreviewtoolkit.server.model.AnalyzerJobConfiguration
import org.ossreviewtoolkit.server.model.JobConfigurations
import org.ossreviewtoolkit.server.model.OrtRun
import org.ossreviewtoolkit.server.model.OrtRunStatus
import org.ossreviewtoolkit.server.model.util.OptionalValue
import org.ossreviewtoolkit.server.utils.test.DatabaseTest

class DaoOrtRunRepositoryTest : DatabaseTest() {
    private lateinit var fixtures: Fixtures
    private lateinit var ortRunRepository: DaoOrtRunRepository

    private var repositoryId = -1L

    private val jobConfigurations = JobConfigurations(
        analyzer = AnalyzerJobConfiguration(
            allowDynamicVersions = true
        )
    )

    override suspend fun beforeTest(testCase: TestCase) {
        dataSource.connect()

        fixtures = Fixtures()
        repositoryId = fixtures.repository.id

        ortRunRepository = DaoOrtRunRepository()
    }

    init {
        test("create should create an entry in the database") {
            val revision = "revision"

            val createdOrtRun = ortRunRepository.create(repositoryId, revision, jobConfigurations)

            val dbEntry = ortRunRepository.get(createdOrtRun.id)

            dbEntry.shouldNotBeNull()
            dbEntry shouldBe OrtRun(
                id = createdOrtRun.id,
                index = createdOrtRun.id,
                repositoryId = repositoryId,
                revision = revision,
                createdAt = createdOrtRun.createdAt,
                jobs = jobConfigurations,
                status = OrtRunStatus.CREATED
            )
        }

        test("create should create sequential indexes for different repositories") {
            val otherRepository = fixtures.createRepository(url = "https://example.com/repo2.git")

            ortRunRepository.create(repositoryId, "revision", jobConfigurations).index shouldBe 1
            ortRunRepository.create(otherRepository.id, "revision", jobConfigurations).index shouldBe 1
            ortRunRepository.create(otherRepository.id, "revision", jobConfigurations).index shouldBe 2
            ortRunRepository.create(repositoryId, "revision", jobConfigurations).index shouldBe 2
        }

        test("getByIndex should return the correct run") {
            val ortRun = ortRunRepository.create(repositoryId, "revision", jobConfigurations)

            ortRunRepository.getByIndex(repositoryId, ortRun.index) shouldBe ortRun
        }

        test("listForRepositories should return all runs for a repository") {
            val ortRun1 = ortRunRepository.create(repositoryId, "revision1", jobConfigurations)
            val ortRun2 = ortRunRepository.create(repositoryId, "revision2", jobConfigurations)

            ortRunRepository.listForRepository(repositoryId) shouldBe listOf(ortRun1, ortRun2)
        }

        test("update should update an entry in the database") {
            val ortRun = ortRunRepository.create(repositoryId, "revision", jobConfigurations)

            val updateStatus = OptionalValue.Present(OrtRunStatus.ACTIVE)

            val updateResult = ortRunRepository.update(ortRun.id, updateStatus)

            updateResult shouldBe ortRun.copy(status = updateStatus.value)
            ortRunRepository.get(ortRun.id) shouldBe ortRun.copy(status = updateStatus.value)
        }

        test("delete should delete the database entry") {
            val ortRun = ortRunRepository.create(repositoryId, "revision", jobConfigurations)

            ortRunRepository.delete(ortRun.id)

            ortRunRepository.listForRepository(repositoryId) shouldBe emptyList()
        }
    }
}
