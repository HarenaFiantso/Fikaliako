'use client';

import { useMemo, useState } from 'react';

import { useTranslations } from 'next-intl';
import Link from 'next/link';

import { zodResolver } from '@hookform/resolvers/zod';
import { Check, LocateFixed, MapPin, Send } from 'lucide-react';
import { useForm, useWatch } from 'react-hook-form';
import { toast } from 'sonner';

import type { EstablishmentProposal } from '@fikaliako/api-client';

import { FormAlert } from '@/components/auth/form-alert';

import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';

import { useProposeEstablishment } from '@/hooks/use-propose-establishment';

import { ESTABLISHMENT_TYPES, TYPE_ICONS } from '@/lib/establishments';
import { cn } from '@/lib/utils';
import { createProposalSchema, type ProposalValues } from '@/lib/validation/proposal-schema';

export function ProposeForm() {
  const t = useTranslations('propose');
  const tErrors = useTranslations('propose.errors');
  const tTypes = useTranslations('establishment.types');
  const propose = useProposeEstablishment();

  const [formError, setFormError] = useState<string | null>(null);
  const [locationError, setLocationError] = useState<string | null>(null);
  const [locating, setLocating] = useState(false);
  const [accuracy, setAccuracy] = useState<number | null>(null);
  const [sent, setSent] = useState(false);

  const schema = useMemo(() => createProposalSchema(tErrors), [tErrors]);
  const form = useForm({
    resolver: zodResolver(schema),
    defaultValues: {
      name: '',
      type: 'gargotte' as const,
      lat: '',
      lng: '',
      city: 'Antananarivo',
      district: '',
      address: '',
      phone: '',
      avgPrice: '',
      comment: '',
    },
  });

  const [lat, lng] = useWatch({ control: form.control, name: ['lat', 'lng'] });
  const pinned = lat !== '' && lng !== '';

  function locate() {
    setLocationError(null);

    if (typeof navigator === 'undefined' || !navigator.geolocation) {
      setLocationError(t('position.unsupported'));
      return;
    }

    setLocating(true);
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        form.setValue('lat', coords.latitude.toFixed(6), { shouldValidate: true });
        form.setValue('lng', coords.longitude.toFixed(6), { shouldValidate: true });
        setAccuracy(coords.accuracy);
        setLocating(false);
      },
      () => {
        setLocationError(t('position.denied'));
        setLocating(false);
      },
      { timeout: 10_000 }
    );
  }

  function onSubmit(values: ProposalValues) {
    setFormError(null);

    const body: EstablishmentProposal = {
      name: values.name,
      type: values.type,
      position: { lat: Number(values.lat), lng: Number(values.lng) },
      city: values.city,
      ...(values.district && { district: values.district }),
      ...(values.address && { address: values.address }),
      ...(values.phone && { phone: values.phone }),
      ...(values.avgPrice && { avg_price_ar: Number(values.avgPrice) }),
      ...(values.comment && { comment: values.comment }),
    };

    propose.mutate(body, {
      onSuccess: () => {
        toast.success(t('success-toast'));
        setSent(true);
      },
      onError: () => setFormError(t('errors.submit-failed')),
    });
  }

  if (sent) {
    return (
      <Card className="mx-auto max-w-md text-center">
        <CardContent className="flex flex-col items-center gap-4 py-4">
          <span className="flex size-12 items-center justify-center rounded-full bg-emerald-500/10 text-emerald-700 dark:text-emerald-400">
            <Check className="size-6" />
          </span>
          <div className="space-y-1">
            <p className="text-lg font-bold">{t('sent-title')}</p>
            <p className="text-muted-foreground text-sm">{t('sent-body')}</p>
          </div>
          <div className="flex flex-wrap justify-center gap-2">
            <Button size="sm" asChild>
              <Link href="/explore">{t('back-to-explore')}</Link>
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                form.reset();
                setAccuracy(null);
                setSent(false);
              }}
            >
              {t('add-another')}
            </Button>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      <div className="space-y-1">
        <h1 className="text-2xl font-extrabold tracking-tight">{t('title')}</h1>
        <p className="text-muted-foreground text-sm">{t('subtitle')}</p>
      </div>

      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSubmit)} noValidate>
          <FormAlert message={formError} />

          <div className="grid gap-6">
            <FormField
              control={form.control}
              name="type"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t('fields.type')}</FormLabel>
                  <FormControl>
                    <div className="flex flex-wrap gap-2">
                      {ESTABLISHMENT_TYPES.map((type) => {
                        const TypeIcon = TYPE_ICONS[type];
                        const selected = field.value === type;
                        return (
                          <button
                            key={type}
                            type="button"
                            aria-pressed={selected}
                            onClick={() => field.onChange(type)}
                            className={cn(
                              'flex min-h-11 items-center gap-1.5 rounded-full px-4 text-sm transition-colors',
                              selected
                                ? 'bg-accent text-primary font-semibold'
                                : 'bg-secondary text-muted-foreground hover:bg-accent/60'
                            )}
                          >
                            <TypeIcon className="size-4" />
                            {tTypes(type)}
                          </button>
                        );
                      })}
                    </div>
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="name"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t('fields.name')}</FormLabel>
                  <FormControl>
                    <Input placeholder={t('placeholders.name')} {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="space-y-3">
              <FormLabel>{t('fields.position')}</FormLabel>
              <div className="bg-card flex items-center gap-3 rounded-2xl border p-3">
                <MapPin
                  className={cn(
                    'size-5.5 shrink-0',
                    pinned ? 'text-primary' : 'text-muted-foreground'
                  )}
                />
                <div className="min-w-0 flex-1 text-sm">
                  {pinned ? (
                    <>
                      <p className="font-semibold">{t('position.pinned')}</p>
                      <p className="text-muted-foreground">
                        {lat}, {lng}
                        {accuracy != null ? ` · ±${Math.round(accuracy)} m` : ''}
                      </p>
                    </>
                  ) : (
                    <p className="text-muted-foreground">{t('position.hint')}</p>
                  )}
                </div>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  disabled={locating}
                  onClick={locate}
                >
                  <LocateFixed />
                  {locating ? t('position.locating') : t('position.use-my-location')}
                </Button>
              </div>

              {locationError ? <p className="text-destructive text-sm">{locationError}</p> : null}

              <div className="grid gap-4 sm:grid-cols-2">
                <FormField
                  control={form.control}
                  name="lat"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>{t('fields.lat')}</FormLabel>
                      <FormControl>
                        <Input inputMode="decimal" placeholder="-18.909200" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="lng"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>{t('fields.lng')}</FormLabel>
                      <FormControl>
                        <Input inputMode="decimal" placeholder="47.521000" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>
              <FormDescription>{t('position.description')}</FormDescription>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <FormField
                control={form.control}
                name="district"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t('fields.district')}</FormLabel>
                    <FormControl>
                      <Input placeholder={t('placeholders.district')} {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="city"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t('fields.city')}</FormLabel>
                    <FormControl>
                      <Input placeholder="Antananarivo" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <FormField
              control={form.control}
              name="address"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t('fields.address')}</FormLabel>
                  <FormControl>
                    <Input placeholder={t('placeholders.address')} {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="grid gap-4 sm:grid-cols-2">
              <FormField
                control={form.control}
                name="phone"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t('fields.phone')}</FormLabel>
                    <FormControl>
                      <Input type="tel" inputMode="tel" placeholder="034 12 345 67" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="avgPrice"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t('fields.avg-price')}</FormLabel>
                    <FormControl>
                      <Input inputMode="numeric" placeholder="3000" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <FormField
              control={form.control}
              name="comment"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t('fields.comment')}</FormLabel>
                  <FormControl>
                    <Textarea
                      rows={4}
                      maxLength={500}
                      placeholder={t('placeholders.comment')}
                      {...field}
                    />
                  </FormControl>
                  <FormDescription>{t('fields.comment-description')}</FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />

            <Button type="submit" disabled={propose.isPending} className="w-full sm:w-auto">
              <Send />
              {propose.isPending ? t('sending') : t('submit')}
            </Button>
          </div>
        </form>
      </Form>
    </div>
  );
}
