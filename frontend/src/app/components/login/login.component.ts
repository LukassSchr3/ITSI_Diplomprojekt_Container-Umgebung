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
  protected isSubmitting = signal<boolean>(false);

  async onSubmit() {
    if (this.isSubmitting()) return;
    this.errorMessage.set('');
    this.isSubmitting.set(true);

    const success = await this.authService.login(this.email(), this.password());
    this.isSubmitting.set(false);

    if (success) {
      this.router.navigate(['/dashboard']);
    } else {
      this.errorMessage.set('Falsche E-Mail oder Passwort');
    }
  }
}
