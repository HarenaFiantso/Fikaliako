import RestaurantMenuRoundedIcon from '@mui/icons-material/RestaurantMenuRounded';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useState, type FormEvent } from 'react';
import { useLogin } from 'react-admin';

export function LoginPage() {
  const login = useLogin();
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login({ phone: phone.trim(), password });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Sign-in failed.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: 'background.default',
        p: 2,
      }}
    >
      <Card
        sx={{
          width: '100%',
          maxWidth: 400,
          border: 1,
          borderColor: 'divider',
          boxShadow: '0 20px 40px -24px rgba(183, 75, 33, 0.35)',
        }}
      >
        <CardContent sx={{ p: 4 }}>
          <Stack spacing={3}>
            <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
              <Box
                sx={{
                  width: 44,
                  height: 44,
                  borderRadius: 2.5,
                  bgcolor: 'primary.main',
                  color: 'primary.contrastText',
                  display: 'grid',
                  placeItems: 'center',
                }}
              >
                <RestaurantMenuRoundedIcon fontSize="small" />
              </Box>
              <Box>
                <Typography variant="h6" sx={{ fontWeight: 800, lineHeight: 1.2 }}>
                  Fikaliako
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Moderation back-office
                </Typography>
              </Box>
            </Stack>

            <Typography variant="body2" color="text.secondary">
              Sign in with your moderator or admin account.
            </Typography>

            {error ? <Alert severity="error">{error}</Alert> : null}

            <Stack component="form" spacing={2} onSubmit={handleSubmit}>
              <TextField
                label="Phone number"
                type="tel"
                placeholder="+261 34 00 000 01"
                value={phone}
                onChange={(event) => setPhone(event.target.value)}
                required
                autoFocus
                autoComplete="username"
                fullWidth
              />
              <TextField
                label="Password"
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                required
                autoComplete="current-password"
                fullWidth
              />
              <Button
                type="submit"
                variant="contained"
                size="large"
                disabled={submitting}
                sx={{ height: 44 }}
              >
                {submitting ? 'Signing in…' : 'Sign in'}
              </Button>
            </Stack>
          </Stack>
        </CardContent>
      </Card>
    </Box>
  );
}
