/*
 * Copyright (C) 2025 The ORT Server Authors (See <https://github.com/eclipse-apoapsis/ort-server/blob/main/NOTICE>)
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
import { useNavigate } from '@tanstack/react-router';
import { useState } from 'react';
import { Bar, BarChart, CartesianGrid, XAxis } from 'recharts';

import { useRepositoriesServiceGetApiV1RepositoriesByRepositoryIdRuns } from '@/api/queries';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
  ChartConfig,
  ChartContainer,
  ChartLegend,
  ChartLegendContent,
  ChartTooltip,
  ChartTooltipContent,
} from '@/components/ui/chart';
import { Label } from '@/components/ui/label';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { config } from '@/config';
import {
  calculateDuration,
  convertDurationToHms,
} from '@/helpers/get-run-duration';
import { ALL_ITEMS } from '@/lib/constants';
import { toast } from '@/lib/toast';
import { LoadingIndicator } from '../loading-indicator';
import { ToastError } from '../toast-error';

const chartConfig = {
  analyzer: {
    label: 'Analyzer',
    color: 'hsl(var(--chart-analyzer))',
  },
  advisor: {
    label: 'Advisor',
    color: 'hsl(var(--chart-advisor))',
  },
  scanner: {
    label: 'Scanner',
    color: 'hsl(var(--chart-scanner))',
  },
  evaluator: {
    label: 'Evaluator',
    color: 'hsl(var(--chart-evaluator))',
  },
  reporter: {
    label: 'Reporter',
    color: 'hsl(var(--chart-reporter))',
  },
} satisfies ChartConfig;

type JobDurationsProps = {
  repoId: string;
  pageIndex: number;
  pageSize: number;
};

const pollInterval = config.pollInterval;

export const JobDurations = ({
  repoId,
  pageIndex,
  pageSize,
}: JobDurationsProps) => {
  const [fetchMode, setFetchMode] = useState<'VISIBLE_RUNS' | 'ALL_RUNS'>(
    'VISIBLE_RUNS'
  );
  const navigate = useNavigate();

  const {
    data: runs,
    error: runsError,
    isPending: runsIsPending,
    isError: runsIsError,
  } = useRepositoriesServiceGetApiV1RepositoriesByRepositoryIdRuns(
    {
      repositoryId: Number.parseInt(repoId),
      limit: fetchMode === 'VISIBLE_RUNS' ? pageSize : ALL_ITEMS,
      offset: fetchMode === 'VISIBLE_RUNS' ? pageIndex * pageSize : undefined,
      sort: '-index',
    },
    undefined,
    {
      refetchInterval: pollInterval,
    }
  );

  const chartData = runs?.data?.map((run) => {
    const analyzerDuration =
      run.jobs.analyzer?.createdAt && run.jobs.analyzer?.finishedAt
        ? calculateDuration(
            run.jobs.analyzer.createdAt,
            run.jobs.analyzer.finishedAt
          ).durationMs
        : null;

    const advisorDuration =
      run.jobs.advisor?.createdAt && run.jobs.advisor?.finishedAt
        ? calculateDuration(
            run.jobs.advisor.createdAt,
            run.jobs.advisor.finishedAt
          ).durationMs
        : null;

    const scannerDuration =
      run.jobs.scanner?.createdAt && run.jobs.scanner?.finishedAt
        ? calculateDuration(
            run.jobs.scanner.createdAt,
            run.jobs.scanner.finishedAt
          ).durationMs
        : null;

    const evaluatorDuration =
      run.jobs.evaluator?.createdAt && run.jobs.evaluator?.finishedAt
        ? calculateDuration(
            run.jobs.evaluator.createdAt,
            run.jobs.evaluator.finishedAt
          ).durationMs
        : null;

    const reporterDuration =
      run.jobs.reporter?.createdAt && run.jobs.reporter?.finishedAt
        ? calculateDuration(
            run.jobs.reporter.createdAt,
            run.jobs.reporter.finishedAt
          ).durationMs
        : null;

    return {
      runId: run.index,
      analyzer: analyzerDuration,
      advisor: advisorDuration,
      scanner: scannerDuration,
      evaluator: evaluatorDuration,
      reporter: reporterDuration,
    };
  });

  if (runsIsPending) {
    return <LoadingIndicator />;
  }

  if (runsIsError) {
    toast.error('Unable to load data', {
      description: <ToastError error={runsError} />,
      duration: Infinity,
      cancel: {
        label: 'Dismiss',
        onClick: () => {},
      },
    });
    return;
  }

  if (runs.pagination.totalCount === 0) {
    return null;
  }

  const handleBarClick = (runIndex: string) => {
    const run = runs.data?.find((run) => run.index === Number(runIndex));
    if (run)
      navigate({
        to: '/organizations/$orgId/products/$productId/repositories/$repoId/runs/$runIndex',
        params: {
          orgId: run.organizationId.toString(),
          productId: run.productId.toString(),
          repoId: run.repositoryId.toString(),
          runIndex: run.index.toString(),
        },
      });
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className='flex items-center justify-between'>
          Durations
          <div className='flex items-center space-x-2'>
            <div className='text-sm font-normal'>Show durations for</div>
            <RadioGroup
              value={fetchMode}
              onValueChange={(value) =>
                setFetchMode(value as 'VISIBLE_RUNS' | 'ALL_RUNS')
              }
              className='flex gap-2'
            >
              <div className='flex items-center space-x-2'>
                <RadioGroupItem value='VISIBLE_RUNS' id='visible' />
                <Label htmlFor='visible'>current view</Label>
              </div>
              <div className='flex items-center space-x-2'>
                <RadioGroupItem value='ALL_RUNS' id='all' />
                <Label htmlFor='all'>all runs</Label>
              </div>
            </RadioGroup>
          </div>
        </CardTitle>
      </CardHeader>
      <CardContent>
        <ChartContainer
          config={chartConfig}
          className='h-[200px] min-h-[200px] w-full'
        >
          <BarChart
            accessibilityLayer
            data={chartData}
            onClick={(data) => {
              handleBarClick(data.activeLabel as string);
            }}
          >
            <CartesianGrid vertical={false} />
            <XAxis
              dataKey='runId'
              tickLine={false}
              tickMargin={10}
              axisLine={false}
              reversed
            />
            <ChartTooltip
              content={
                <ChartTooltipContent
                  hideLabel
                  className='w-[180px]'
                  formatter={(value, name) => (
                    <>
                      <div className='flex w-full items-baseline justify-between'>
                        <div className='flex items-baseline gap-2'>
                          <div
                            className='size-2.5 shrink-0 rounded-[2px]'
                            style={{
                              backgroundColor: `var(--color-${name})`,
                            }}
                          />
                          <div>
                            {chartConfig[name as keyof typeof chartConfig]
                              ?.label || name}
                          </div>
                        </div>
                        <div className='text-muted-foreground font-mono text-xs'>
                          {convertDurationToHms(Number(value))}
                        </div>
                      </div>
                    </>
                  )}
                  footer={
                    <div className='text-muted-foreground text-xs'>
                      Click bar to go to run
                    </div>
                  }
                />
              }
            />
            <ChartLegend content={<ChartLegendContent />} />
            <Bar
              dataKey='analyzer'
              stackId='a'
              fill='var(--color-analyzer)'
              className='cursor-pointer'
            />
            <Bar
              dataKey='advisor'
              stackId='a'
              fill='var(--color-advisor)'
              className='cursor-pointer'
            />
            <Bar
              dataKey='scanner'
              stackId='a'
              fill='var(--color-scanner)'
              className='cursor-pointer'
            />
            <Bar
              dataKey='evaluator'
              stackId='a'
              fill='var(--color-evaluator)'
              className='cursor-pointer'
            />
            <Bar
              dataKey='reporter'
              stackId='a'
              fill='var(--color-reporter)'
              className='cursor-pointer'
            />
          </BarChart>
        </ChartContainer>
      </CardContent>
    </Card>
  );
};
