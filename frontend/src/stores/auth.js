import { defineStore } from "pinia";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    token: localStorage.getItem("siaumkm_token") || null,
    role: null // 'OWNER' | 'STAFF' — NFR-10
  }),
  getters: {
    isAuthenticated: (state) => !!state.token
  },
  actions: {
    setSession(token, role) {
      this.token = token;
      this.role = role;
      localStorage.setItem("siaumkm_token", token);
    },
    logout() {
      this.token = null;
      this.role = null;
      localStorage.removeItem("siaumkm_token");
    }
  }
});
