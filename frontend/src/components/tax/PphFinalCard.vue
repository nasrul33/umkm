<template>
  <div class="card p-6">
    <h2 class="font-display text-lg mb-1">PPh Final Masa Bulanan</h2>
    <p class="text-xs text-slate-500 mb-4">Kalkulasi PPh Final 0,5% atas omzet masa pajak</p>

    <div class="flex flex-wrap items-center gap-2 mb-5">
      <select v-model.number="bulan" class="border border-slate-300 rounded-full px-4 py-2 text-sm bg-white">
        <option v-for="(nama, i) in NAMA_BULAN" :key="i" :value="i + 1">{{ nama }}</option>
      </select>
      <select v-model.number="tahun" class="border border-slate-300 rounded-full px-4 py-2 text-sm bg-white">
        <option v-for="t in daftarTahun" :key="t" :value="t">{{ t }}</option>
      </select>
      <button class="btn-primary" :disabled="menghitung" @click="hitung">
        {{ menghitung ? "Menghitung..." : "Hitung" }}
      </button>
    </div>

    <p v-if="galat" class="text-sm rounded-lg bg-red-50 text-red-800 ring-1 ring-red-100 px-3 py-2 mb-4">
      {{ galat }}
    </p>

    <div v-if="!hasil && !galat" class="text-sm text-slate-500">
      Pilih bulan lalu tekan Hitung untuk melihat hasil kalkulasi PPh Final masa tersebut.
    </div>

    <template v-if="hasil">
      <div class="card-accent p-5 mb-4">
        <p class="text-sm text-slate-500">PPh Final Terhitung — masa {{ NAMA_BULAN[hasil.bulan - 1] }} {{ hasil.tahun }}</p>
        <p class="angka-hero mt-1">{{ formatRupiah(hasil.pajakTerhitung) }}</p>
      </div>

      <dl class="grid sm:grid-cols-3 gap-4 text-sm mb-4">
        <div>
          <dt class="text-slate-500">Omzet Bruto Bulan Ini</dt>
          <dd class="angka mt-0.5">{{ formatRupiah(hasil.omzetBruto) }}</dd>
        </div>
        <div>
          <dt class="text-slate-500">Omzet Kumulatif Tahun Berjalan</dt>
          <dd class="angka mt-0.5">{{ formatRupiah(hasil.omzetKumulatifTahunan) }}</dd>
        </div>
        <div>
          <dt class="text-slate-500">Omzet Kena Pajak</dt>
          <dd class="angka mt-0.5">{{ formatRupiah(hasil.omzetKenaPajak) }}</dd>
        </div>
      </dl>

      <p class="text-sm">Batas waktu setor: <span class="font-medium">{{ hasil.jatuhTempoSetor }}</span></p>
      <p class="text-xs text-slate-400 mt-2">Dihitung berdasarkan {{ hasil.regulasiAcuan }}.</p>
    </template>
  </div>
</template>

<script setup>
import { ref } from "vue";
import client from "../../api/client";
import { formatRupiah } from "../../utils/money";

const emit = defineEmits(["calculated"]);

const NAMA_BULAN = ["Januari", "Februari", "Maret", "April", "Mei", "Juni",
  "Juli", "Agustus", "September", "Oktober", "November", "Desember"];

const kini = new Date();
// default: masa bulan LALU (masa berjalan belum final)
const masaLalu = new Date(kini.getFullYear(), kini.getMonth() - 1, 1);
const bulan = ref(masaLalu.getMonth() + 1);
const tahun = ref(masaLalu.getFullYear());
const daftarTahun = Array.from({ length: kini.getFullYear() - 2022 + 1 }, (_, i) => kini.getFullYear() - i);

const hasil = ref(null);
const menghitung = ref(false);
const galat = ref("");

async function hitung() {
  menghitung.value = true;
  galat.value = "";
  try {
    const { data } = await client.post("/app/tax/pph-final/recalculate", null, {
      params: { tahun: tahun.value, bulan: bulan.value }
    });
    hasil.value = data;
    emit("calculated", data);
  } catch (e) {
    hasil.value = null;
    galat.value = e.response?.data?.message
      ?? "Kalkulasi tidak dapat dilakukan untuk masa ini. Periksa kelengkapan data identitas usaha.";
  } finally {
    menghitung.value = false;
  }
}
</script>
