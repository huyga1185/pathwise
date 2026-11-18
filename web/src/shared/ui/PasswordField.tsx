import { useState } from 'react';
import { vi } from '@/shared/i18n/vi';
import { EyeIcon, EyeOffIcon } from './icons';
import { TextField, type TextFieldProps } from './TextField';

/**
 * A password field with the eye button at its trailing end that shows or hides the password, the same as on the
 * sign-in page (a secure field hides private data — HIG Text fields: "Use secure text fields to hide private data";
 * the button lets people check what they typed). At the field's trailing end (HIG Text fields: "the trailing end to
 * offer additional features"; APPLE_CHECKLIST T-8); a familiar icon, its name for assistive technologies and on hover,
 * and whether it is on (I-1, I-2, aria-pressed); its shape concentric with the field (I-3, ui.css).
 */
export function PasswordField(props: Omit<TextFieldProps, 'type' | 'endAction' | 'clearable'>) {
  const [shown, setShown] = useState(false);
  return (
    <TextField
      {...props}
      type={shown ? 'text' : 'password'}
      endAction={
        <button
          type="button"
          className="pw-icon-button"
          aria-label={shown ? vi.auth.hidePassword : vi.auth.showPassword}
          // Its name on hover too (APPLE_CHECKLIST I-2; HIG Buttons: macOS shows a tooltip).
          title={shown ? vi.auth.hidePassword : vi.auth.showPassword}
          aria-pressed={shown}
          onClick={() => setShown((was) => !was)}
        >
          {shown ? <EyeOffIcon /> : <EyeIcon />}
        </button>
      }
    />
  );
}
