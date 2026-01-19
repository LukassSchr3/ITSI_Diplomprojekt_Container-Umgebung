import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../service/auth.service';

@Component({
  selector: 'app-login',
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  private authService = inject(AuthService);

  protected email = signal<string>('');
  protected password = signal<string>('');
  protected errorMessage = signal<string>('');

  onSubmit() {
    const success = this.authService.login(this.email(), this.password());
    if (!success) {
      this.errorMessage.set('Falsche E-Mail oder Passwort');
    }
  }
}
