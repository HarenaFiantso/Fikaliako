'use client';

import * as React from 'react';

import { OTPInput, OTPInputContext } from 'input-otp';
import { Minus } from 'lucide-react';

import { cn } from '@/lib/utils';

function InputOTP({
  className,
  containerClassName,
  ...props
}: React.ComponentProps<typeof OTPInput> & {
  containerClassName?: string;
}) {
  return (
    <OTPInput
      data-slot="input-otp"
      containerClassName={cn('flex items-center gap-2 has-disabled:opacity-50', containerClassName)}
      className={cn('disabled:cursor-not-allowed', className)}
      {...props}
    />
  );
}

function InputOTPGroup({ className, ...props }: React.ComponentProps<'div'>) {
  return (
    <div
      data-slot="input-otp-group"
      className={cn('flex items-center gap-2', className)}
      {...props}
    />
  );
}

function InputOTPSlot({
  index,
  className,
  ...props
}: React.ComponentProps<'div'> & {
  index: number;
}) {
  const inputOTPContext = React.useContext(OTPInputContext);
  const { char, hasFakeCaret, isActive } = inputOTPContext?.slots[index] ?? {};

  return (
    <div
      data-slot="input-otp-slot"
      data-active={isActive}
      className={cn(
        'border-input relative flex size-12 items-center justify-center rounded-lg border text-lg font-semibold shadow-xs transition-all duration-150 outline-none',
        'data-[active=true]:border-ring data-[active=true]:ring-ring/30 data-[active=true]:z-10 data-[active=true]:ring-[3px]',
        'aria-invalid:border-destructive data-[active=true]:aria-invalid:ring-destructive/20',
        className
      )}
      {...props}
    >
      {char}
      {hasFakeCaret && (
        <div className="pointer-events-none absolute inset-0 flex items-center justify-center">
          <div className="animate-caret-blink bg-foreground h-5 w-px duration-1000" />
        </div>
      )}
    </div>
  );
}

function InputOTPSeparator({ ...props }: React.ComponentProps<'div'>) {
  return (
    <div data-slot="input-otp-separator" role="separator" {...props}>
      <Minus className="text-muted-foreground size-4" />
    </div>
  );
}

export { InputOTP, InputOTPGroup, InputOTPSlot, InputOTPSeparator };
