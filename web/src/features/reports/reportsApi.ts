import { useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';
import { useApi } from '@/data/ApiContext';
import { unwrap, unwrapFile } from '@/data/http';
import { queryKeys } from '@/data/queryKeys';
import type { Schemas } from '@/data/types';

export type StoreReport = Schemas['StoreReportResponse'];
export type ReportDriver = Schemas['ReportDriver'];
export type ReportFailureReason = Schemas['ReportFailureReason'];

/** Longest period of a report (P-68). */
export const MAX_DAYS = 31;

/** `GET /reports/summary` of the store, by plan date, both days included (API_SPEC §7a, T63). */
export function useReport(from: string, to: string, enabled = true) {
  const api = useApi();
  return useQuery({
    queryKey: queryKeys.report({ from, to }),
    queryFn: () => unwrap(api.GET('/api/v1/reports/summary', { params: { query: { from, to } } })),
    enabled,
    placeholderData: (previous) => previous,
  });
}

/** `GET /reports/stops.csv` as a file to save. */
export function useReportApi() {
  const api = useApi();
  return useMemo(
    () => ({
      stopsCsv: (from: string, to: string) =>
        unwrapFile(
          api.GET('/api/v1/reports/stops.csv', { params: { query: { from, to } }, parseAs: 'blob' }),
          `pathwise-${from.replaceAll('-', '')}-${to.replaceAll('-', '')}.csv`,
        ),
    }),
    [api],
  );
}
