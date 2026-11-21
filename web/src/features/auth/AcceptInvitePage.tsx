import { useEffect, useRef, useState, type FormEvent } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useSearchParams } from 'react-router';
import type { Schemas } from '@/data/types';
import { fieldMessage, messageFor } from '@/shared/errors';
import { applyServerErrors, errorCode } from '@/shared/forms/serverErrors';
import { formatDate } from '@/shared/format';
import { vi } from '@/shared/i18n/vi';
import {
  Button,
  buttonClass,
  CheckCircleIcon,
  ErrorBanner,
  KeyIcon,
  LockIcon,
  TextField,
  UserIcon,
  UserPlusIcon,
} from '@/shared/ui';
import { useAccountApi } from './accountApi';
import { AuthCardHead, AuthLayout, MainArt, MapArt } from './AuthLayout';
import { changeCard } from './changeCard';
import { passwordRules, requiredRule } from './rules';

type Preview = Schemas['InvitationPreviewResponse'];
type Step = 'code' | 'form' | 'done';

interface AcceptForm {
  fullName: string;
  password: string;
}

// No phone number here (M06, D7): the driver adds it in the app ("Thông tin của tôi"), anyone else on the web profile.
const FIELDS = ['fullName', 'password'] as const;
const LABELS = { fullName: vi.form.fullName, password: vi.auth.password };
const STEP_ICONS = [KeyIcon, UserPlusIcon, CheckCircleIcon];

/**
 * Prototype "Nhận lời mời" (SCREENS §1.1): the invitation email links here with the code (`?code=`), filled in and
 * checked at once; the code can also be pasted. The invitation is shown, then the invited person sets a name and
 * password. Afterwards a DRIVER is told to open the app. Same picture, card and controls as "Đăng nhập" (AuthStage,
 * SCREENS.md §1.2).
 */
export function AcceptInvitePage() {
  const accountApi = useAccountApi();
  const [params] = useSearchParams();
  const linkCode = (params.get('code') ?? '').replace(/\s+/g, '');
  const [step, setStep] = useState<Step>('code');
  const [code, setCode] = useState(linkCode);
  const [codeError, setCodeError] = useState<string | null>(null);
  const [preview, setPreview] = useState<Preview | null>(null);
  const [failure, setFailure] = useState<unknown>(null);
  const [checking, setChecking] = useState(false);
  const {
    register,
    handleSubmit,
    setError,
    trigger,
    formState: { errors, isSubmitting },
  } = useForm<AcceptForm>({
    // HIG Text fields: "when creating a user name or password, validation needs to happen before people switch to
    // another field" (the password checks itself while typed, below); the other fields when people leave them.
    mode: 'onTouched',
    defaultValues: { fullName: '', password: '' },
  });

  // The card changes its content in place (code → form → done): move focus to the new title, so VoiceOver and other
  // screen readers report the change (HIG VoiceOver: "Inform VoiceOver when visible content or layout changes occur")
  // and the keyboard does not stay on a button that is gone.
  const heading = useRef<HTMLHeadingElement>(null);
  const shownStep = useRef(step);
  useEffect(() => {
    if (shownStep.current === step) return;
    shownStep.current = step;
    heading.current?.focus();
  }, [step]);

  const cleanCode = code.replace(/\s+/g, '');

  const check = async (value: string) => {
    setFailure(null);
    if (!value) {
      setCodeError(fieldMessage('INVITATION_CODE_REQUIRED', vi.invite.code));
      return;
    }
    setChecking(true);
    try {
      const found = await accountApi.previewInvitation(value);
      changeCard(() => {
        setPreview(found);
        setCodeError(null);
        setStep('form');
      });
    } catch (error) {
      if (errorCode(error) === 'INVITATION_INVALID') setCodeError(vi.invite.invalid);
      else setFailure(error);
    } finally {
      setChecking(false);
    }
  };
  const onCheck = (event: FormEvent) => {
    event.preventDefault();
    void check(cleanCode);
  };
  // Opened from the link of the email: check the code once, no need to press "Tiếp tục".
  const checkedLink = useRef(false);
  useEffect(() => {
    if (!linkCode || checkedLink.current) return;
    checkedLink.current = true;
    void check(linkCode);
    // eslint-disable-next-line react-hooks/exhaustive-deps -- once, for the code of the link
  }, [linkCode]);

  const onAccept = handleSubmit(async (form) => {
    setFailure(null);
    try {
      await accountApi.acceptInvitation({
        code: cleanCode,
        fullName: form.fullName.trim(),
        password: form.password,
      });
      changeCard(() => setStep('done'));
    } catch (error) {
      const codeOfError = errorCode(error);
      if (codeOfError === 'INVITATION_INVALID') {
        changeCard(() => {
          setCodeError(vi.invite.invalid);
          setStep('code');
        });
      } else if (!applyServerErrors(error, setError, FIELDS, LABELS)) {
        setFailure(error);
      }
    }
  });

  const isDriver = preview?.role === 'DRIVER';
  const storeName = preview?.storeName ?? '';

  return (
    <AuthLayout
      panel="wide"
      logoToLogin
      art={<MapArt />}
      mainArt={<MainArt variant="register" />}
      pitch={
        <div className="pw-auth-pitch pw-auth-pitch-steps">
          <div className="pw-auth-pitch-text">
            <p className="pw-auth-headline">
              {vi.invite.brandHeadline[0]}
              <span>{vi.invite.brandHeadline[1]}</span>
              {vi.invite.brandHeadline[2]}
            </p>
            <p className="pw-auth-lead">{vi.invite.brandLead}</p>
          </div>
          <ol className="pw-auth-steps-inline">
            {vi.invite.steps.map((item, index) => {
              const StepIcon = STEP_ICONS[index] ?? CheckCircleIcon;
              return (
                <li key={item}>
                  <StepIcon />
                  <span>{item}</span>
                </li>
              );
            })}
          </ol>
        </div>
      }
    >
      <form
        className="pw-auth-card"
        aria-labelledby="i-title"
        onSubmit={step === 'code' ? (event) => void onCheck(event) : (event) => void onAccept(event)}
        noValidate
      >
        <AuthCardHead id="i-title" title={vi.invite.titles[step]} accent={false} headingRef={heading} />
        {failure ? (
          <ErrorBanner title={messageFor(failure)}>
            {errorCode(failure) === 'EMAIL_EXISTED' ? (
              <Link to="/login" viewTransition>
                {vi.form.toLogin}
              </Link>
            ) : null}
          </ErrorBanner>
        ) : null}

        {step === 'code' ? (
          <>
            <TextField
              label={vi.invite.code}
              clearable
              required
              size="lg"
              spellCheck={false}
              autoCapitalize="off"
              startIcon={<KeyIcon />}
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
              <dd>{vi.roles[preview.role] ?? preview.role}</dd>
              <dt>{vi.invite.expires}</dt>
              <dd>{formatDate(preview.expiresAt)}</dd>
            </dl>
            <TextField
              label={vi.form.fullName}
              clearable
              required
              size="lg"
              maxLength={100}
              startIcon={<UserIcon />}
              autoComplete="name"
              error={errors.fullName?.message}
              {...register('fullName', requiredRule('FULL_NAME_REQUIRED', vi.form.fullName))}
            />
            <TextField
              label={vi.auth.password}
              type="password"
              required
              size="lg"
              maxLength={72}
              autoComplete="new-password"
              hint={vi.form.passwordHint}
              startIcon={<LockIcon />}
              error={errors.password?.message}
              {...register('password', {
                ...passwordRules(vi.auth.password),
                onChange: () => void trigger('password'),
              })}
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
              <Link className={buttonClass('primary', 'lg', true)} to="/login" viewTransition>
                {vi.invite.webLogin}
              </Link>
            )}
          </>
        ) : null}

        {step !== 'done' ? (
          <p className="pw-auth-center">
            {vi.form.haveAccount}{' '}
            <Link to="/login" viewTransition>
              {vi.form.toLogin}
            </Link>
          </p>
        ) : null}
      </form>
    </AuthLayout>
  );
}
