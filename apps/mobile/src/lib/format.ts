export function formatAriary(amount: number): string {
  const grouped = String(amount).replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
  return `${grouped} Ar`;
}
