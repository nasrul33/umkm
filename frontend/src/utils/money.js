// Format uang tampilan — komputasi uang TIDAK pernah terjadi di frontend.
const fmt = new Intl.NumberFormat("id-ID", {
  style: "currency", currency: "IDR",
  minimumFractionDigits: 0, maximumFractionDigits: 2
});

// Gaya akuntansi: negatif dalam kurung, bukan minus.
export function formatRupiah(nilai) {
  const n = Number(nilai ?? 0);
  if (n === 0) return fmt.format(0); // normalisasi -0 -> "Rp 0"
  return n < 0 ? `(${fmt.format(Math.abs(n))})` : fmt.format(n);
}

// Konvensi backend: saldo = debit - kredit. Akun bersaldo normal KREDIT
// (kewajiban, modal, pendapatan) dibalik tandanya untuk display.
export function displaySaldo(tipe, saldo) {
  return ["KEWAJIBAN", "MODAL", "PENDAPATAN"].includes(tipe) ? -Number(saldo) : Number(saldo);
}
