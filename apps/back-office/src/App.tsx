import { Admin } from 'react-admin';

import { darkTheme, lightTheme } from './theme';

export function App() {
  return <Admin title="Fikaliako Back-office" theme={lightTheme} darkTheme={darkTheme} />;
}
