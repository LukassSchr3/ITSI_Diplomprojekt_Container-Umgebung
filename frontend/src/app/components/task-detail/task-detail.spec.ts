import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { Component } from '@angular/core';

import { TaskDetail } from './task-detail';

@Component({ template: '' })
class DummyComponent {}

describe('TaskDetail', () => {
  let component: TaskDetail;
  let fixture: ComponentFixture<TaskDetail>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TaskDetail],
      providers: [
        provideRouter([{ path: 'dashboard', component: DummyComponent }]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: '1' }) } },
        },
      ],
    })
    .compileComponents();

    fixture = TestBed.createComponent(TaskDetail);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
