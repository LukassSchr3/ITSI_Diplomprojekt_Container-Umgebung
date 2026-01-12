import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Image } from '../interfaces/Image';

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  images: Image[] = [
    { ID: BigInt(1), Name: 'Ubuntu 22.04', URL: 'ubuntu:22.04' },
    { ID: BigInt(2), Name: 'Nginx Latest', URL: 'nginx:latest' },
    { ID: BigInt(3), Name: 'Node.js 20', URL: 'node:20' },
  ];

  constructor(private router: Router) {}

  onImageClick(image: Image): void {
    console.log('Navigating to image:', image);
    this.router.navigate(['/image', image.ID.toString()]);
  }
}
