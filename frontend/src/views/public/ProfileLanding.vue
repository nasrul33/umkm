<template>
  <main class="min-h-screen bg-white font-body">
    <!-- SRS-A-01: hero section identitas usaha -->
    <section class="px-6 py-24 text-center bg-brand-primary text-white">
      <img v-if="profile.logoUrl" :src="profile.logoUrl" alt="" class="mx-auto mb-6 h-16" />
      <h1 class="font-display text-4xl md:text-5xl tracking-tight">{{ profile.namaUsaha }}</h1>
      <p class="mt-4 text-lg text-white/80 max-w-xl mx-auto">{{ profile.tagline }}</p>
      <a :href="waLink" class="inline-block mt-8 px-6 py-3 rounded-full bg-brand-secondary text-white
                                 hover:opacity-90 transition">
        Hubungi via WhatsApp
      </a>
    </section>

    <!-- SRS-A-01: tentang kami -->
    <section class="px-6 py-16 max-w-3xl mx-auto text-center">
      <h2 class="font-display text-2xl mb-4">Tentang Kami</h2>
      <p class="text-gray-600 leading-relaxed">{{ profile.ceritaUsaha }}</p>
    </section>

    <!-- SRS-A-03: katalog produk (showcase, bukan transaksional) -->
    <section class="px-6 py-16 bg-gray-50">
      <h2 class="font-display text-2xl text-center mb-10">Produk & Layanan</h2>
      <div class="grid grid-cols-1 md:grid-cols-3 gap-8 max-w-5xl mx-auto">
        <div v-for="p in products" :key="p.id" class="bg-white rounded-lg shadow-sm overflow-hidden
                                                        transition hover:shadow-md">
          <img :src="p.fotoUrl" :alt="p.nama" class="w-full h-48 object-cover" />
          <div class="p-4">
            <h3 class="font-medium">{{ p.nama }}</h3>
            <p v-if="p.tampilkanHarga" class="text-brand-secondary mt-1">
              Rp {{ p.hargaDitampilkan?.toLocaleString('id-ID') }}
            </p>
          </div>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import client from "../../api/client";

// SRS-A-01..03: data disajikan dari business_profile/product_showcase — endpoint publik,
// tanpa autentikasi (lihat SecurityConfig backend: /public/** permitAll).
const profile = ref({});
const products = ref([]);

const waLink = computed(() => {
  const nomor = profile.value.whatsappNumber || "";
  const teks = encodeURIComponent(`Halo, saya tertarik dengan ${profile.value.namaUsaha || "usaha Anda"}`);
  return `https://wa.me/${nomor}?text=${teks}`;
});

onMounted(async () => {
  const { data } = await client.get("/public/api/profile");
  profile.value = data.profile;
  products.value = data.products;
});
</script>
