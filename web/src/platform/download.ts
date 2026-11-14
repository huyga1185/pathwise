/**
 * Saves a file the app received (W12: import template, report CSV). Files come with the access token in a header, so
 * a plain link cannot download them: the blob is given to the browser through a temporary object URL.
 */
export function saveFile(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.rel = 'noopener';
  document.body.appendChild(link);
  link.click();
  link.remove();
  // Some browsers start the download after the click returns.
  setTimeout(() => URL.revokeObjectURL(url), 0);
}
