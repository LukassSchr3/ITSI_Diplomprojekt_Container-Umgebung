import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../service/auth.service';

@Component({
  selector: 'app-login',
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  protected email = signal<string>('');
  protected password = signal<string>('');
  protected errorMessage = signal<string>('');
  protected isLoading = signal(false);

  async onSubmit() {
    this.errorMessage.set('');
    this.isLoading.set(true);
    try {
      console.log(this.email())
      console.log(this.password())
      const success = await this.authService.login(this.email(), this.password());
      if (success) {
        await this.router.navigate(['/dashboard']);
      } else {
        this.errorMessage.set('Falsche E-Mail oder Passwort');
      }
    } catch {
      this.errorMessage.set('Verbindung zum Server fehlgeschlagen');
    } finally {
      this.isLoading.set(false);
    }
  }
}
