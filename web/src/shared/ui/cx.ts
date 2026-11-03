export function cx(...parts: unknown[]): string {
  return parts.filter((part): part is string => typeof part === 'string' && part !== '').join(' ');
}
