import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { MachineListComponent } from './machine-list.component';

describe('MachineListComponent', () => {
  let component: MachineListComponent;
  let fixture: ComponentFixture<MachineListComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ MachineListComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MachineListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
