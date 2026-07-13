<template>
  <div class="card p-6">
    <div class="flex flex-wrap items-start justify-between gap-3 mb-4">
      <div>
        <h2 class="font-display text-lg">Ambang Omzet Gabungan</h2>
        <p class="text-xs text-slate-500 mt-0.5">Pasal 58 — penggabungan omzet lingkaran keluarga</p>
      </div>
      <StatusBadge v-if="hasil" :status="hasil.status" />
    </div>

    <div v-if="memuat" class="h-24 rounded-xl bg-slate-100 animate-pulse"></div>

    <div v-else-if="!hasil" class="text-sm text-slate-500">
      Belum ada perhitungan tersimpan.
      <button class="btn-outline ml-2" :disabled="menghitung" @click="perbarui">Hitung Sekarang</button>
    </div>

    <template v-else>
      <div class="flex items-end justify-between mb-2">
        <p class="angka-hero">{{ formatRupiah(hasil.omzetGabungan) }}</p>
        <p v-if="hasil.ambangAtas" class="text-sm text-slate-500">
          dari batas {{ formatRupiah(hasil.ambangAtas) }} ({{ persen }}%)
        </p>
      </div>
      <div v-if="hasil.ambangAtas" class="h-2.5 rounded-full bg-slate-100 overflow-hidden mb-4">
        <div class="h-full rounded-full transition-all duration-500"
             :class="warnaBar" :style="{ width: Math.min(persen, 100) + '%' }"></div>
      </div>

      <dl class="grid grid-cols-2 gap-4 text-sm mb-4">
        <div>
          <dt class="text-slate-500">Omzet usaha ini</dt>
          <dd class="angka mt-0.5">{{ formatRupiah(hasil.omzetEntitasUtama) }}</dd>
        </div>
        <div>
          <dt class="text-slate-500">Omzet entitas terkait</dt>
          <dd class="angka mt-0.5">{{ formatRupiah(hasil.omzetEntitasTerkait) }}</dd>
        </div>
      </dl>

      <p class="text-sm text-slate-600 leading-relaxed">{{ hasil.keterangan }}</p>

      <p v-if="hasil.dataEntitasTerkaitLengkap === false"
         class="mt-3 text-xs rounded-lg bg-blue-50 text-blue-800 ring-1 ring-blue-100 px-3 py-2">
        Data omzet entitas terkait belum lengkap. Angka gabungan dapat berubah setelah data dilengkapi.
      </p>

      <div class="flex items-center justify-between mt-4">
        <p v-if="hasil.regulasiAcuan" class="text-xs text-slate-400">Rujukan: {{ hasil.regulasiAcuan }}</p>
        <button class="btn-outline" :disabled="menghitung" @click="perbarui">
          {{ menghitung ? "Menghitung..." : "Perbarui Perhitungan" }}
        </button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import client from "../../api/client";
import { formatRupiah } from "../../utils/money";
import StatusBadge from "../ui/StatusBadge.vue";

const hasil = ref(null);
const memuat = ref(true);
const menghitung = ref(false);

const persen = computed(() =>
  hasil.value?.ambangAtas
    ? Math.round((Number(hasil.value.omzetGabungan) / Number(hasil.value.ambangAtas)) * 100)
    : 0
);

const warnaBar = computed(() => {
  if (!hasil.value) return "bg-slate-300";
  return {
    DI_BAWAH_BATAS: "bg-emerald-500",
    MENDEKATI_BATAS: "bg-amber-500",
    MELEBIHI_BATAS: "bg-red-500"
  }[hasil.value.status] ?? "bg-slate-300";
});

async function muatTerakhir() {
  memuat.value = true;
  try {
    const { data } = await client.get("/app/tax/omzet-aggregation/latest");
    hasil.value = data;
  } catch {
    hasil.value = null; // 404 = belum pernah dihitung
  } finally {
    memuat.value = false;
  }
}

async function perbarui() {
  menghitung.value = true;
  try {
    const { data } = await client.post("/app/tax/omzet-aggregation/recalculate");
    hasil.value = data;
  } finally {
    menghitung.value = false;
  }
}

onMounted(muatTerakhir);
</script>
