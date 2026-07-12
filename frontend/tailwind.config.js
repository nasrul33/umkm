/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{vue,js}"],
  theme: {
    extend: {
      // SRS-A-06: warna dikonsumsi dari brand_theme sebagai CSS variable runtime,
      // BUKAN warna brand hardcoded — lihat premium-ux-designer agent.
      colors: {
        brand: {
          primary: "var(--brand-primary, #1F2A44)",
          secondary: "var(--brand-secondary, #8A6D3B)"
        }
      },
      fontFamily: {
        display: ["var(--font-display, 'Playfair Display')", "serif"],
        body: ["var(--font-body, 'Inter')", "sans-serif"]
      }
    }
  },
  plugins: []
};
