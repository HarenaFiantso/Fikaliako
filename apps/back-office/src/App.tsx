import FactCheckRoundedIcon from '@mui/icons-material/FactCheckRounded';
import { Admin, Resource } from 'react-admin';

import { LoginPage } from './auth/LoginPage';
import { authProvider } from './auth/provider';
import { ContributionList } from './moderation/ContributionList';
import { ContributionShow } from './moderation/ContributionShow';
import { dataProvider } from './moderation/data-provider';
import { type ContributionRecord, proposalOf } from './moderation/types';
import { darkTheme, lightTheme } from './theme';

export function App() {
  return (
    <Admin
      title="Fikaliako Back-office"
      authProvider={authProvider}
      dataProvider={dataProvider}
      loginPage={LoginPage}
      requireAuth
      theme={lightTheme}
      darkTheme={darkTheme}
    >
      <Resource
        name="contributions"
        options={{ label: 'Moderation queue' }}
        icon={FactCheckRoundedIcon}
        list={ContributionList}
        show={ContributionShow}
        recordRepresentation={(record: ContributionRecord) =>
          proposalOf(record).name ?? String(record.id)
        }
      />
    </Admin>
  );
}
