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

import { ShieldQuestion } from 'lucide-react';

import { useRunsServiceGetApiV1RunsByRunIdStatistics } from '@/api/queries';
import { JobStatus, VulnerabilityRating } from '@/api/requests';
import { LoadingIndicator } from '@/components/loading-indicator';
import { StatisticsCard } from '@/components/statistics-card';
import { ToastError } from '@/components/toast-error';
import {
  getStatusFontColor,
  getVulnerabilityRatingBackgroundColor,
} from '@/helpers/get-status-class';
import { isJobFinished, jobStatusTexts } from '@/helpers/job-helpers';
import { toast } from '@/lib/toast';

type VulnerabilitiesStatisticsCardProps = {
  jobIncluded?: boolean;
  status: JobStatus | undefined;
  runId: number;
};

export const VulnerabilitiesStatisticsCard = ({
  jobIncluded,
  status,
  runId,
}: VulnerabilitiesStatisticsCardProps) => {
  const { data, isPending, isError, error } =
    useRunsServiceGetApiV1RunsByRunIdStatistics(
      {
        runId: runId,
      },
      undefined,
      {
        enabled: isJobFinished(status),
      }
    );

  if (isPending) {
    return (
      <StatisticsCard
        title='Vulnerabilities'
        icon={() => (
          <ShieldQuestion className={`h-4 w-4 ${getStatusFontColor(status)}`} />
        )}
        value={<LoadingIndicator />}
        className='hover:bg-muted/50 h-full'
      />
    );
  }

  if (isError) {
    toast.error('Unable to load data', {
      description: <ToastError error={error} />,
      duration: Infinity,
      cancel: {
        label: 'Dismiss',
        onClick: () => {},
      },
    });
    return;
  }

  const total = data.vulnerabilitiesCount;
  const counts = data.vulnerabilitiesCountByRating;
  const { value, description } = jobStatusTexts(status, jobIncluded, total);

  return (
    <StatisticsCard
      title='Vulnerabilities'
      icon={() => (
        <ShieldQuestion className={`h-4 w-4 ${getStatusFontColor(status)}`} />
      )}
      value={value}
      description={description}
      counts={
        counts
          ? Object.entries(counts).map(([rating, count]) => ({
              key: rating,
              count: count,
              color: getVulnerabilityRatingBackgroundColor(
                rating as VulnerabilityRating
              ),
            }))
          : []
      }
      className='hover:bg-muted/50 h-full'
    />
  );
};
