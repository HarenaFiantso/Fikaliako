'use client';

import { useTranslations } from 'next-intl';

import { ChevronDown, LogOut } from 'lucide-react';
import { toast } from 'sonner';

import type { UserProfile } from '@fikaliako/api-client';

import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';

import { useLogout } from '@/hooks/use-auth-mutations';

import { formatPhoneForDisplay } from '@/lib/validation/normalize-phone';

export function UserMenu({ user }: { user: UserProfile }) {
  const t = useTranslations('header');
  const logout = useLogout();

  function handleLogout() {
    logout.mutate(undefined, {
      onSettled: () => toast.success(t('logout-success')),
    });
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="sm" className="gap-2 pl-1.5">
          <span className="bg-primary/15 text-primary flex size-7 items-center justify-center rounded-full text-xs font-bold uppercase">
            {user.display_name.charAt(0)}
          </span>
          <span className="max-w-28 truncate text-sm font-semibold">{user.display_name}</span>
          <ChevronDown className="text-muted-foreground size-3.5" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="min-w-[12rem]">
        <DropdownMenuLabel>
          <p className="text-sm">{user.display_name}</p>
          <p className="text-muted-foreground text-xs font-normal">
            {formatPhoneForDisplay(user.phone)}
          </p>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem variant="destructive" onSelect={handleLogout}>
          <LogOut />
          {t('logout')}
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
