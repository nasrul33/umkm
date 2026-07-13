import { defineStore } from "pinia";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    token: localStorage.getItem("siaumkm_token") || null,
    // Role dipersist utk UX-gating menu; sumber kebenaran otorisasi tetap
    // backend (403) — NFR-10.
    role: localStorage.getItem("siaumkm_role") || null
  }),
  getters: {
    isAuthenticated: (state) => !!state.token,
    isOwner: (state) => state.role === "OWNER"
  },
  actions: {
    setSession(token, role) {
      this.token = token;
      this.role = role;
      localStorage.setItem("siaumkm_token", token);
      localStorage.setItem("siaumkm_role", role);
    },
    logout() {
      this.token = null;
      this.role = null;
      localStorage.removeItem("siaumkm_token");
      localStorage.removeItem("siaumkm_role");
    }
  }
});
