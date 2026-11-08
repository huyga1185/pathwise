import type { FieldValues, Path, UseFormSetError } from 'react-hook-form';
import { ApiError } from '@/data/apiError';
import { fieldErrors } from '@/shared/errors';

/**
 * Puts the field errors of a 400 VALIDATION_ERROR under the form's fields (ARCHITECTURE §3.2: under the field, not in a
 * toast). `fields` are the form's own names, the same as the request's.
 * @returns whether at least one error was placed on a field; otherwise the caller shows the error at the top of the form
 */
export function applyServerErrors<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
  fields: readonly Path<T>[],
  labels: Record<string, string>,
): boolean {
  if (!(error instanceof ApiError) || error.code !== 'VALIDATION_ERROR') return false;
  let placed = false;
  for (const [field, message] of Object.entries(fieldErrors(error, labels))) {
    if ((fields as readonly string[]).includes(field)) {
      setError(field as Path<T>, { type: 'server', message }, { shouldFocus: !placed });
      placed = true;
    }
  }
  return placed;
}

/** The code of an API error, or null for anything else (network errors included). */
export function errorCode(error: unknown): string | null {
  return error instanceof ApiError ? error.code : null;
}
