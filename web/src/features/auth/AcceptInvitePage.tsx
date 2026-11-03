import { useState, type FormEvent } from 'react';
import { useForm } from 'react-hook-form';
import { Link } from 'react-router';
import type { Schemas } from '@/data/types';
import { errorMessages, fieldMessage, messageFor } from '@/shared/errors';
import { applyServerErrors, errorCode } from '@/shared/forms/serverErrors';
import { formatDate } from '@/shared/format';
import { vi } from '@/shared/i18n/vi';
import { Button, buttonClass, ErrorBanner, TextField } from '@/shared/ui';
import { useAccountApi } from './accountApi';
import { AuthLayout, CompactArt } from './AuthLayout';
import { passwordRules, requiredRule } from './rules';

type Preview = Schemas['InvitationPreviewResponse'];
type Step = 'code' | 'form' | 'done';

interface AcceptForm {
  fullName: string;
  phoneNumber: string;
  password: string;
}

const FIELDS = ['fullName', 'phoneNumber', 'password'] as const;
const LABELS = { fullName: vi.form.fullName, phoneNumber: vi.form.phone, password: vi.auth.password };

/**
 * Prototype "Nhận lời mời" (SCREENS §1.1): the email holds only a 43-character code, so the code is pasted here; the
 * invitation is shown, then the invited person sets a name and password. Afterwards a DRIVER is told to open the app.
 */
export function AcceptInvitePage() {
  const accountApi = useAccountApi();
  const [step, setStep] = useState<Step>('code');
  const [code, setCode] = useState('');
  const [codeError, setCodeError] = useState<string | null>(null);
  const [preview, setPreview] = useState<Preview | null>(null);
  const [failure, setFailure] = useState<unknown>(null);
  const [checking, setChecking] = useState(false);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<AcceptForm>({ defaultValues: { fullName: '', phoneNumber: '', password: '' } });

  const cleanCode = code.replace(/\s+/g, '');

  const onCheck = async (event: FormEvent) => {
    event.preventDefault();
    setFailure(null);
    if (!cleanCode) {
      setCodeError(fieldMessage('INVITATION_CODE_REQUIRED', vi.invite.code));
      return;
    }
    setChecking(true);
    try {
      setPreview(await accountApi.previewInvitation(cleanCode));
      setCodeError(null);
      setStep('form');
    } catch (error) {
      if (errorCode(error) === 'INVITATION_INVALID') setCodeError(vi.invite.invalid);
      else setFailure(error);
    } finally {
      setChecking(false);
    }
  };

  const onAccept = handleSubmit(async (form) => {
    setFailure(null);
    try {
      await accountApi.acceptInvitation({
        code: cleanCode,
        fullName: form.fullName.trim(),
        password: form.password,
        phoneNumber: form.phoneNumber.trim() || undefined,
      });
      setStep('done');
    } catch (error) {
      const codeOfError = errorCode(error);
      if (codeOfError === 'INVITATION_INVALID') {
        setCodeError(vi.invite.invalid);
        setStep('code');
      } else if (codeOfError === 'INVALID_PHONE_NUMBER') {
        setError('phoneNumber', { type: 'server', message: errorMessages.INVALID_PHONE_NUMBER }, { shouldFocus: true });
      } else if (!applyServerErrors(error, setError, FIELDS, LABELS)) {
        setFailure(error);
      }
    }
  });

  const isDriver = preview?.role === 'DRIVER';
  const storeName = preview?.storeName ?? '';

  return (
    <AuthLayout
      panel="compact"
      logoToLogin
      art={<CompactArt picture="invite" />}
      pitch={<p className="pw-auth-note">{vi.invite.brandLead}</p>}
    >
      <form
        className="pw-auth-card pw-auth-card-lg"
        aria-labelledby="i-title"
        onSubmit={step === 'code' ? (event) => void onCheck(event) : (event) => void onAccept(event)}
        noValidate
      >
        <div className="pw-auth-card-head">
          <h1 id="i-title">{vi.invite.titles[step]}</h1>
        </div>
        {failure ? (
          <ErrorBanner title={messageFor(failure)}>
            {errorCode(failure) === 'EMAIL_EXISTED' ? <Link to="/login">{vi.form.toLogin}</Link> : null}
          </ErrorBanner>
        ) : null}

        {step === 'code' ? (
          <>
            <TextField
              label={vi.invite.code}
              multiline
              required
              spellCheck={false}
              autoComplete="off"
              placeholder={vi.invite.codePlaceholder}
              hint={codeError ? undefined : vi.invite.codeHint}
              error={codeError ?? undefined}
              value={code}
              onChange={(event) => {
                setCode(event.target.value);
                setCodeError(null);
              }}
            />
            <Button type="submit" variant="primary" size="lg" block busy={checking}>
              {vi.invite.check}
            </Button>
          </>
        ) : null}

        {step === 'form' && preview ? (
          <>
            <dl className="pw-auth-facts">
              <dt>{vi.invite.store}</dt>
              <dd>{preview.storeName}</dd>
              <dt>{vi.invite.email}</dt>
              <dd>{preview.email}</dd>
              <dt>{vi.invite.role}</dt>
              <dd>{preview.role ? (vi.roles[preview.role] ?? preview.role) : ''}</dd>
              <dt>{vi.invite.expires}</dt>
              <dd>{formatDate(preview.expiresAt)}</dd>
            </dl>
            <TextField
              label={vi.form.fullName}
              required
              maxLength={100}
              autoComplete="name"
              error={errors.fullName?.message}
              {...register('fullName', requiredRule('FULL_NAME_REQUIRED', vi.form.fullName))}
            />
            <TextField
              label={vi.form.phone}
              type="tel"
              autoComplete="tel"
              maxLength={20}
              placeholder="0905 123 456"
              hint={vi.form.phoneHint}
              error={errors.phoneNumber?.message}
              {...register('phoneNumber')}
            />
            <TextField
              label={vi.auth.password}
              type="password"
              required
              maxLength={72}
              autoComplete="new-password"
              hint={vi.form.passwordHint}
              error={errors.password?.message}
              {...register('password', passwordRules(vi.auth.password))}
            />
            <Button type="submit" variant="primary" size="lg" block busy={isSubmitting}>
              {vi.invite.submit}
            </Button>
          </>
        ) : null}

        {step === 'done' ? (
          <>
            <div className="pw-auth-done" role="status">
              <strong>{vi.invite.joined(storeName)}</strong>
              <span>{isDriver ? vi.invite.driverNext : vi.invite.webNext}</span>
            </div>
            {isDriver ? null : (
              <Link className={buttonClass('primary', 'lg', true)} to="/login">
                {vi.invite.webLogin}
              </Link>
            )}
          </>
        ) : null}

        {step !== 'done' ? (
          <p className="pw-auth-center">
            {vi.form.haveAccount} <Link to="/login">{vi.form.toLogin}</Link>
          </p>
        ) : null}
      </form>
    </AuthLayout>
  );
}
