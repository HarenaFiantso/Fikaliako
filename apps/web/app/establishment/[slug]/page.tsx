import type { ReactNode } from 'react';

import type { Metadata } from 'next';
import { getTranslations } from 'next-intl/server';
import Link from 'next/link';
import { notFound } from 'next/navigation';

import {
  ArrowLeft,
  BadgeCheck,
  ExternalLink,
  Globe,
  MapPin,
  MessageCircle,
  Phone,
} from 'lucide-react';

import type { EstablishmentDetail, ReviewPage } from '@fikaliako/api-client';

import { Button } from '@/components/ui/button';

import { EstablishmentReviews } from '@/components/establishments/establishment-reviews';
import { FavoriteButton } from '@/components/establishments/favorite-button';
import { OpenStateBadge } from '@/components/establishments/open-state-badge';
import { OpeningHours } from '@/components/establishments/opening-hours';
import { RatingBreakdown } from '@/components/establishments/rating-breakdown';
import { StarRow } from '@/components/establishments/star-row';
import { SiteHeader } from '@/components/site-header';

import { serverApi } from '@/lib/api/server-client';
import {
  AMENITY_META,
  openStateOf,
  paymentIcon,
  toSummary,
  TYPE_ICONS,
} from '@/lib/establishments';
import { formatAriary } from '@/lib/format';

const REVIEWS_PAGE_SIZE = 10;

async function fetchEstablishment(slug: string): Promise<EstablishmentDetail | null> {
  const { data, response } = await serverApi.GET('/v1/establishments/{idOrSlug}', {
    params: { path: { idOrSlug: slug } },
  });
  return response.ok && data ? data : null;
}

/** The first page of reviews, rendered server-side so it is indexable too. */
async function fetchFirstReviews(establishmentId: string): Promise<ReviewPage | undefined> {
  try {
    const { data, response } = await serverApi.GET('/v1/establishments/{establishmentId}/reviews', {
      params: { path: { establishmentId }, query: { limit: REVIEWS_PAGE_SIZE } },
    });
    return response.ok && data ? data : undefined;
  } catch {
    // The page is still worth rendering without them; the client will retry.
    return undefined;
  }
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ slug: string }>;
}): Promise<Metadata> {
  const { slug } = await params;
  const detail = await fetchEstablishment(slug);
  if (!detail) return {};

  const t = await getTranslations('establishment');
  const where = [detail.district, detail.city].filter(Boolean).join(', ');
  const description = t('meta-description', {
    name: detail.name,
    type: t(`types.${detail.type}`),
    where,
  });

  return {
    title: `${detail.name} — ${where}`,
    description,
    alternates: { canonical: `/establishment/${detail.slug}` },
    openGraph: { title: detail.name, description, type: 'website' },
  };
}

export default async function EstablishmentPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;

  const detail = await fetchEstablishment(slug);
  if (!detail) notFound();

  const t = await getTranslations('establishment');
  const reviews = await fetchFirstReviews(detail.id);

  const TypeIcon = TYPE_ICONS[detail.type];
  const amenities = AMENITY_META.filter((amenity) => detail.amenities[amenity.key]);

  const links = [
    detail.phone && { icon: Phone, label: t('contact.call'), href: `tel:${detail.phone}` },
    detail.whatsapp && {
      icon: MessageCircle,
      label: t('contact.whatsapp'),
      href: `https://wa.me/${detail.whatsapp.replace(/\D/g, '')}`,
    },
    detail.facebook_url && {
      icon: ExternalLink,
      label: t('contact.facebook'),
      href: detail.facebook_url,
    },
    detail.website && { icon: Globe, label: t('contact.website'), href: detail.website },
  ].filter(Boolean) as { icon: typeof Phone; label: string; href: string }[];

  const area = [detail.district, detail.city].filter(Boolean).join(' · ');

  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />

      <main className="mx-auto w-full max-w-3xl flex-1 px-4 pb-16 sm:px-6">
        <div className="flex items-center justify-between py-4">
          <Button variant="ghost" size="sm" asChild>
            <Link href="/explore">
              <ArrowLeft />
              {t('back-to-explore')}
            </Link>
          </Button>
          <FavoriteButton establishment={toSummary(detail)} />
        </div>

        <header className="from-accent to-background flex flex-col items-center gap-2 rounded-3xl bg-gradient-to-b px-6 py-10 text-center">
          <span className="bg-primary text-primary-foreground mb-1 flex size-18 items-center justify-center rounded-[22px]">
            <TypeIcon className="size-8" />
          </span>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-extrabold tracking-tight">{detail.name}</h1>
            {detail.verified ? (
              <BadgeCheck
                aria-label={t('verified')}
                className="size-5.5 shrink-0 text-emerald-600 dark:text-emerald-400"
              />
            ) : null}
          </div>
          <p className="text-muted-foreground text-sm">
            {t(`types.${detail.type}`)}
            {detail.avg_price_ar != null ? ` · ~${formatAriary(detail.avg_price_ar)}` : ''}
            {detail.district ? ` · ${detail.district}` : ''}
          </p>
          <div className="mt-1 flex flex-wrap items-center justify-center gap-3">
            {detail.rating.count > 0 && detail.rating.avg_global != null ? (
              <span className="flex items-center gap-1.5 text-sm">
                <StarRow value={detail.rating.avg_global} />
                <span className="font-semibold">{detail.rating.avg_global.toFixed(1)}</span>
                <span className="text-muted-foreground">({detail.rating.count})</span>
              </span>
            ) : null}
            <OpenStateBadge state={openStateOf(detail)} />
          </div>
        </header>

        <div className="space-y-8 pt-8">
          {links.length > 0 ? (
            <Section title={t('sections.contact')}>
              <div className="flex flex-wrap gap-2">
                {links.map((link) => (
                  <a
                    key={link.label}
                    href={link.href}
                    target={link.href.startsWith('http') ? '_blank' : undefined}
                    rel={link.href.startsWith('http') ? 'noreferrer' : undefined}
                    className="bg-secondary hover:bg-accent flex min-h-11 items-center gap-1.5 rounded-full px-4 text-sm font-semibold transition-colors"
                  >
                    <link.icon className="text-primary size-4" />
                    {link.label}
                  </a>
                ))}
              </div>
            </Section>
          ) : null}

          <Section title={t('sections.location')}>
            <div className="flex items-start gap-2 text-sm">
              <MapPin className="text-primary mt-0.5 size-4.5 shrink-0" />
              <div className="space-y-0.5">
                {detail.address ? <p>{detail.address}</p> : null}
                <p className="text-muted-foreground">{area}</p>
                <a
                  href={`https://www.openstreetmap.org/?mlat=${detail.position.lat}&mlon=${detail.position.lng}#map=18/${detail.position.lat}/${detail.position.lng}`}
                  target="_blank"
                  rel="noreferrer"
                  className="text-primary inline-flex items-center gap-1 font-semibold hover:underline"
                >
                  {t('view-on-map')}
                  <ExternalLink className="size-3.5" />
                </a>
              </div>
            </div>
          </Section>

          {detail.opening_hours.length > 0 ? (
            <Section title={t('sections.opening-hours')}>
              <OpeningHours intervals={detail.opening_hours} />
            </Section>
          ) : null}

          {amenities.length > 0 ? (
            <Section title={t('sections.good-to-know')}>
              <ul className="flex flex-wrap gap-2">
                {amenities.map((amenity) => (
                  <li
                    key={amenity.key}
                    className="bg-secondary flex min-h-9 items-center gap-1.5 rounded-full px-3.5 text-sm"
                  >
                    <amenity.icon className="text-primary size-4" />
                    {t(`amenities.${amenity.key}`)}
                  </li>
                ))}
              </ul>
            </Section>
          ) : null}

          {detail.cuisines.length > 0 || detail.payment_methods.length > 0 ? (
            <Section title={t('sections.cuisine-payment')}>
              <ul className="flex flex-wrap gap-2">
                {detail.cuisines.map((cuisine) => (
                  <li
                    key={cuisine.code}
                    className="bg-accent text-accent-foreground flex min-h-9 items-center rounded-full px-3.5 text-sm"
                  >
                    {cuisine.label_fr}
                  </li>
                ))}
                {detail.payment_methods.map((method) => {
                  const PaymentIcon = paymentIcon(method.code);
                  return (
                    <li
                      key={method.code}
                      className="bg-secondary flex min-h-9 items-center gap-1.5 rounded-full px-3.5 text-sm"
                    >
                      <PaymentIcon className="text-primary size-4" />
                      {method.label_fr}
                    </li>
                  );
                })}
              </ul>
            </Section>
          ) : null}

          <Section title={t('sections.reviews')}>
            <RatingBreakdown rating={detail.rating} />
            <EstablishmentReviews establishmentId={detail.id} initialPage={reviews} />
          </Section>
        </div>
      </main>
    </div>
  );
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="space-y-3">
      <h2 className="text-muted-foreground text-xs font-semibold tracking-[0.12em] uppercase">
        {title}
      </h2>
      {children}
    </section>
  );
}
