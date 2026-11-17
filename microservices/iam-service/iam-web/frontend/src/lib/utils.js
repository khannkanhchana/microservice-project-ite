import { clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

/** shadcn/ui class helper: conditional classes, with later Tailwind classes overriding earlier ones. */
export function cn(...inputs) {
  return twMerge(clsx(inputs))
}
