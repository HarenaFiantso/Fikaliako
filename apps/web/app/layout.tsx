import { Metadata } from 'next';
import { Raleway } from 'next/font/google';

import './globals.css';

const raleway = Raleway({
  subsets: ['latin'],
  display: 'swap',
  variable: '--font-raleway',
});

export const metadata: Metadata = {
  title: {
    default: 'Fikaliako',
    template: '%s · Fikaliako',
  },
  description: 'Plateforme de découverte culinaire géolocalisée à Madagascar',
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="fr" suppressHydrationWarning>
      <body className={`${raleway.variable} font-sans antialiased`}>{children}</body>
    </html>
  );
}
