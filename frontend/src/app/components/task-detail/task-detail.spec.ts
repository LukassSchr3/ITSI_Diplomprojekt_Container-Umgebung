import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';

import { TaskDetail } from './task-detail';

describe('TaskDetail', () => {
  let component: TaskDetail;
  let fixture: ComponentFixture<TaskDetail>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TaskDetail],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ taskId: '1' }) } },
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
