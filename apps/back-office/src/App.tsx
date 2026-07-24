import { Admin } from 'react-admin';

import { LoginPage } from './auth/LoginPage';
import { authProvider } from './auth/provider';
import { darkTheme, lightTheme } from './theme';

export function App() {
  return (
    <Admin
      title="Fikaliako Back-office"
      authProvider={authProvider}
      loginPage={LoginPage}
      requireAuth
      theme={lightTheme}
      darkTheme={darkTheme}
    />
  );
}
