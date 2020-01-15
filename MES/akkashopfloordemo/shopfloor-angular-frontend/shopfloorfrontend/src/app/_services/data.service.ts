import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class DataService {

  private countSource = new BehaviorSubject(new Map<string, number>());
  currentCount = this.countSource.asObservable();

  private machineCountSource = new BehaviorSubject(new Map<string, number>());
  currentMachineCount = this.machineCountSource.asObservable();

  constructor() { }

  changeCount(oldCount: Map<string, number>, order: string, count: number) {
    oldCount.set(order, count);
    this.countSource.next(oldCount);
  }

  changeMachineCount(oldMachineCount: Map<string, number>, machine: string, count: number) {
    oldMachineCount.set(machine, count);
    this.machineCountSource.next(oldMachineCount);
  }
}
