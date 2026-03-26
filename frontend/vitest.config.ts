import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    coverage: {
      provider: 'istanbul',
      reporter: ['text', 'text-summary', 'json-summary'],
      include: [
        'src/app/service/*.ts',
        'src/app/services/*.ts',
        'src/app/guards/*.ts',
      ],
      exclude: [
        'src/app/**/*.spec.ts',
        '**/*.d.ts',
        'src/app/service/novnc.ts',
      ],
    },
  },
});
