export function formatAriary(amount: number): string {
  return `${new Intl.NumberFormat('fr-FR').format(amount)} Ar`;
}

export function humanizeType(type: string | undefined): string {
  return type ? type.replace(/_/g, ' ') : '—';
}
