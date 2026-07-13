<template>
  <div>
    <div class="mb-6">
      <h1 class="font-display text-2xl">Pajak</h1>
      <p class="text-sm text-slate-500 mt-0.5">Kalkulasi otomatis berdasarkan data transaksi yang tercatat</p>
    </div>

    <div class="space-y-6">
      <OmzetThresholdCard />
      <PphFinalCard @calculated="hasilMasa = $event" />
      <BillingActionCard v-if="hasilMasa?.taxCalculationLogId"
                         :tax-calculation-log-id="hasilMasa.taxCalculationLogId"
                         :pajak-terhitung="hasilMasa.pajakTerhitung" />
    </div>

    <!-- Pernyataan faktual — bukan saran (BR-B5-11 / Aturan Emas #5) -->
    <p class="text-xs text-slate-400 mt-8">
      Seluruh angka pada halaman ini adalah hasil kalkulasi otomatis dari data transaksi
      yang tercatat, berdasarkan peraturan yang dirujuk. Halaman ini bukan nasihat pajak.
    </p>
  </div>
</template>

<script setup>
import { ref } from "vue";
import OmzetThresholdCard from "../../components/tax/OmzetThresholdCard.vue";
import PphFinalCard from "../../components/tax/PphFinalCard.vue";
import BillingActionCard from "../../components/tax/BillingActionCard.vue";

const hasilMasa = ref(null);
</script>
