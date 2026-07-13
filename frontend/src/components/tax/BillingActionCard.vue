<template>
  <div class="card p-6">
    <div class="flex flex-wrap items-start justify-between gap-3 mb-4">
      <div>
        <h2 class="font-display text-lg">Kode Billing</h2>
        <p class="text-xs text-slate-500 mt-0.5">Untuk penyetoran {{ formatRupiah(pajakTerhitung) }}</p>
      </div>
      <StatusBadge v-if="billing" :status="billing.status" />
    </div>

    <div v-if="!billing">
      <button class="btn-primary" :disabled="memproses" @click="buat">
        {{ memproses ? "Memproses..." : "Buat Kode Billing" }}
      </button>
    </div>

    <template v-else>
      <!-- ISSUED: kode + salin -->
      <div v-if="billing.status === 'ISSUED'" class="flex flex-wrap items-center gap-3">
        <p class="font-mono text-2xl tracking-widest">{{ billing.kodeBilling }}</p>
        <button class="btn-outline" @click="salin">{{ tersalin ? "Tersalin ✓" : "Salin" }}</button>
      </div>

      <!-- REQUESTED -->
      <div v-else-if="billing.status === 'REQUESTED'">
        <button class="btn-outline" :disabled="memproses" @click="buat">Muat Ulang Status</button>
      </div>

      <!-- PENDING_MANUAL / FAILED: jalur manual -->
      <div v-else class="space-y-3">
        <p class="text-sm text-slate-600">
          {{ billing.status === 'FAILED'
            ? 'Permintaan ditolak penyedia layanan. Periksa data, coba lagi, atau catat kode billing yang dibuat manual.'
            : 'Layanan pembuatan kode billing sedang tidak tersedia. Anda dapat membuat kode billing melalui DJP Online lalu mencatatnya di sini.' }}
        </p>
        <div class="flex flex-wrap items-center gap-2">
          <button v-if="billing.status === 'FAILED'" class="btn-outline" :disabled="memproses" @click="buat">
            Coba Lagi
          </button>
          <input v-model="kodeManual" maxlength="15" inputmode="numeric" placeholder="Kode billing 15 digit"
                 class="border border-slate-300 rounded-full px-4 py-2 text-sm font-mono tracking-wider w-56" />
          <button class="btn-primary" :disabled="!kodeValid || memproses" @click="simpanManual">Simpan Kode</button>
        </div>
        <p v-if="kodeManual && !kodeValid" class="text-xs text-red-700">Kode billing harus tepat 15 digit angka.</p>
      </div>

      <p v-if="galat" class="mt-3 text-sm rounded-lg bg-red-50 text-red-800 ring-1 ring-red-100 px-3 py-2">
        {{ galat }}
      </p>
    </template>
  </div>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import client from "../../api/client";
import { formatRupiah } from "../../utils/money";
import StatusBadge from "../ui/StatusBadge.vue";

const props = defineProps({
  taxCalculationLogId: { type: String, required: true },
  pajakTerhitung: { type: [Number, String], required: true }
});

const billing = ref(null);
const memproses = ref(false);
const kodeManual = ref("");
const tersalin = ref(false);
const galat = ref("");

const kodeValid = computed(() => /^[0-9]{15}$/.test(kodeManual.value));

// hasil kalkulasi baru = konteks billing baru
watch(() => props.taxCalculationLogId, () => { billing.value = null; galat.value = ""; });

async function buat() {
  memproses.value = true;
  galat.value = "";
  try {
    const { data } = await client.post(`/app/tax/billing/${props.taxCalculationLogId}`);
    billing.value = data;
  } catch (e) {
    galat.value = e.response?.data?.message ?? "Permintaan kode billing gagal diproses.";
  } finally {
    memproses.value = false;
  }
}

async function simpanManual() {
  memproses.value = true;
  galat.value = "";
  try {
    const { data } = await client.put(`/app/tax/billing/${billing.value.id}/manual`, {
      kodeBilling: kodeManual.value
    });
    billing.value = data;
  } catch (e) {
    galat.value = e.response?.data?.message ?? "Kode tidak dapat disimpan.";
  } finally {
    memproses.value = false;
  }
}

async function salin() {
  await navigator.clipboard.writeText(billing.value.kodeBilling);
  tersalin.value = true;
  setTimeout(() => (tersalin.value = false), 2000);
}
</script>
