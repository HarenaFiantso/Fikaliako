import {
  Accessibility,
  Banknote,
  BedDouble,
  Bike,
  Clock,
  Coffee,
  CreditCard,
  Flower,
  GraduationCap,
  IceCreamCone,
  type LucideIcon,
  Mountain,
  ParkingCircle,
  Sandwich,
  ShoppingBasket,
  Smartphone,
  Snowflake,
  Store,
  Sun,
  Truck,
  Users,
  Utensils,
  Wifi,
  Wine,
} from 'lucide-react';

import type {
  Amenities,
  EstablishmentDetail,
  EstablishmentSummary,
  EstablishmentType,
  OpeningInterval,
} from '@fikaliako/api-client';

export const ESTABLISHMENT_TYPES = [
  'restaurant',
  'gargotte',
  'cafe',
  'snack',
  'food_truck',
  'street_vendor',
  'pastry_shop',
  'bar_restaurant',
  'hotel_restaurant',
] as const satisfies readonly EstablishmentType[];

export const TYPE_ICONS: Record<EstablishmentType, LucideIcon> = {
  restaurant: Utensils,
  gargotte: Store,
  cafe: Coffee,
  snack: Sandwich,
  food_truck: Truck,
  street_vendor: ShoppingBasket,
  pastry_shop: IceCreamCone,
  bar_restaurant: Wine,
  hotel_restaurant: BedDouble,
};

export const AMENITY_META: readonly { key: keyof Amenities; icon: LucideIcon }[] = [
  { key: 'delivery', icon: Bike },
  { key: 'parking', icon: ParkingCircle },
  { key: 'wifi', icon: Wifi },
  { key: 'wheelchair_access', icon: Accessibility },
  { key: 'air_conditioning', icon: Snowflake },
  { key: 'terrace', icon: Sun },
  { key: 'family_friendly', icon: Users },
  { key: 'romantic', icon: Flower },
  { key: 'student_friendly', icon: GraduationCap },
  { key: 'scenic_view', icon: Mountain },
  { key: 'open_24h', icon: Clock },
];

export function paymentIcon(code: string): LucideIcon {
  if (code === 'cash') return Banknote;
  if (code === 'carte') return CreditCard;
  return Smartphone;
}

export const REVIEW_CRITERIA = [
  'avg_quality',
  'avg_price',
  'avg_cleanliness',
  'avg_speed',
  'avg_welcome',
] as const;

export type ReviewCriterion = (typeof REVIEW_CRITERIA)[number];

/** Day 0 = Monday (project book ch. 6.1), unlike JS `getDay()` where 0 = Sunday. */
export const WEEKDAY_KEYS = [
  'monday',
  'tuesday',
  'wednesday',
  'thursday',
  'friday',
  'saturday',
  'sunday',
] as const;

const SHORT_WEEKDAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

/**
 * Today's index in Antananarivo, where the establishments are. Opening hours
 * are evaluated in `Indian/Antananarivo` server-side (project book ch. 4.5), so
 * the highlighted row follows the same clock rather than the visitor's.
 */
export function antananarivoDayIndex(now: Date = new Date()): number {
  const weekday = new Intl.DateTimeFormat('en-US', {
    timeZone: 'Indian/Antananarivo',
    weekday: 'short',
  }).format(now);
  return Math.max(0, SHORT_WEEKDAYS.indexOf(weekday));
}

export function intervalsOfDay(intervals: OpeningInterval[], day: number): OpeningInterval[] {
  return intervals
    .filter((interval) => interval.day_of_week === day)
    .sort((a, b) => a.opens_at.localeCompare(b.opens_at));
}

export type OpenState = 'open' | 'closed-now' | 'closed' | 'pending';

export function openStateOf(detail: EstablishmentDetail): OpenState {
  if (detail.status === 'closed') return 'closed';
  if (detail.status === 'pending') return 'pending';
  return detail.open_now ? 'open' : 'closed-now';
}

export function toSummary(detail: EstablishmentDetail): EstablishmentSummary {
  return {
    id: detail.id,
    slug: detail.slug,
    name: detail.name,
    type: detail.type,
    position: detail.position,
    verified: detail.verified,
    status: detail.status,
    rating_count: detail.rating.count,
    ...(detail.avg_price_ar != null && { avg_price_ar: detail.avg_price_ar }),
    ...(detail.rating.avg_global != null && { rating_avg: detail.rating.avg_global }),
  };
}
