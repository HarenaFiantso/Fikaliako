import { useEffect, useState } from 'react';

import { useLocalSearchParams } from 'expo-router';
import { StyleSheet, View } from 'react-native';

import { AlertBanner } from '@/components/ui/alert-banner';
import { Button } from '@/components/ui/button';
import { FormScreen } from '@/components/ui/form-screen';
import { OtpInput } from '@/components/ui/otp-input';

import { ThemedText } from '@/components/themed-text';

import { formatPhone, normalizePhone } from '@/lib/auth/phone';
import { useSession } from '@/lib/auth/session-store';
import { useCelebration } from '@/lib/celebration-store';

import { Spacing } from '@/constants/theme';

const RESEND_DELAY_SECONDS = 30;

export default function VerifyPhoneScreen() {
  const { phone = '' } = useLocalSearchParams<{ phone?: string }>();
  const verifyPhone = useSession((state) => state.verifyPhone);
  const resendOtp = useSession((state) => state.resendOtp);

  const [code, setCode] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [verifying, setVerifying] = useState(false);
  const [resending, setResending] = useState(false);
  const [countdown, setCountdown] = useState(RESEND_DELAY_SECONDS);

  const displayPhone = formatPhone(normalizePhone(phone) ?? phone);

  useEffect(() => {
    if (countdown <= 0) return;
    const timer = setTimeout(() => setCountdown((value) => value - 1), 1000);
    return () => clearTimeout(timer);
  }, [countdown]);

  const verify = async (value: string) => {
    if (value.length !== 6 || verifying) return;
    setVerifying(true);
    setError(null);
    setNotice(null);
    try {
      await verifyPhone({ phone, code: value });
      useCelebration.getState().celebrate('Your account is ready — time to eat well.');
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not verify the number');
      setCode('');
      setVerifying(false);
    }
  };

  const handleChange = (value: string) => {
    setCode(value);
    if (value.length === 6) void verify(value);
  };

  const resend = async () => {
    setResending(true);
    setError(null);
    setNotice(null);
    try {
      await resendOtp(phone);
      setNotice('A new code is on its way.');
      setCountdown(RESEND_DELAY_SECONDS);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not send the code');
    } finally {
      setResending(false);
    }
  };

  return (
    <FormScreen
      title="Check your messages"
      subtitle={`Enter the 6-digit code sent by SMS to ${displayPhone}.`}
    >
      {error && <AlertBanner kind="error" message={error} />}
      {notice && !error && <AlertBanner kind="success" message={notice} />}

      <OtpInput value={code} onChange={handleChange} error={error ?? undefined} autoFocus />

      <Button
        title="Verify"
        loading={verifying}
        disabled={code.length !== 6}
        onPress={() => void verify(code)}
      />

      <View style={styles.resendRow}>
        <ThemedText type="small" themeColor="textSecondary">
          Nothing received?
        </ThemedText>
        <Button
          title={countdown > 0 ? `Resend code in ${countdown}s` : 'Resend code'}
          variant="ghost"
          disabled={countdown > 0}
          loading={resending}
          onPress={() => void resend()}
        />
      </View>
    </FormScreen>
  );
}

const styles = StyleSheet.create({
  resendRow: {
    alignItems: 'center',
    gap: Spacing.one,
  },
});
