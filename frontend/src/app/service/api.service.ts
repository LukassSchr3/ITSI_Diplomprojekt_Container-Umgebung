import axios from 'axios';
import { Injectable } from '@angular/core';

const apiClient = axios.create({
  baseURL: 'http://localhost:5050',
  headers: { 'Content-Type': 'application/json' }
});

// JWT-Token aus sessionStorage an jeden Request anhängen
apiClient.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('auth_token');
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  return config;
});

export default apiClient;

@Injectable({ providedIn: 'root' })
export class ApiService {}

