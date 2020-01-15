import { Component, OnInit, ViewChild } from '@angular/core';
import { MachineEvent } from '../_models/events';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { Router } from '@angular/router';
import { MachineService } from '../_services/machine.service';
import { DataService } from '../_services/data.service';
import { UserService, AuthService } from '../_services';
import { User, Role } from '../_models';
import { first } from 'rxjs/operators';

@Component({
  selector: 'app-machine-list',
  templateUrl: './machine-list.component.html',
  styleUrls: ['./machine-list.component.css']
})
export class MachineListComponent implements OnInit {

  columnNames: string[] = ['machineId', 'eventType', 'state', 'message', 'history'];
  machines: Map<string, MachineEvent> = new Map<string, MachineEvent>();
  dataSource: MatTableDataSource<MachineEvent>;
  count: Map<string, number>;
  currentUser: User;

  @ViewChild(MatPaginator, {static: true}) paginator: MatPaginator;
  @ViewChild(MatSort, {static: true}) sort: MatSort;

  constructor(
    private machineService: MachineService,
    private router: Router,
    private data: DataService,
    private authenticationService: AuthService,
    private userService: UserService
  ) {
    this.currentUser = this.authenticationService.currentUserValue;
  }

  ngOnInit() {
    this.subscribe();
    this.reloadData();
    this.data.currentMachineCount.subscribe(count => this.count = count);
  }

  private subscribe() {
    this.machineService.getMachineUpdates().subscribe(
      sseEvent => {
        const json = JSON.parse(sseEvent.data);
        // console.log('sse', json);
        this.machines.set(json.machineId, json);
        this.dataSource = new MatTableDataSource(Array.from(this.machines.values()));
        if (this.dataSource) {
          this.dataSource.paginator = this.paginator;
          this.dataSource.sort = this.sort;
        }
      },
      err => { console.log('Error receiving SSE', err); },
      () => console.log('SSE stream completed')
    );
  }

  private reloadData() {
    this.machineService.getMachineList().subscribe(data => {
      data.forEach(element => {
        // console.log('element', element);
        this.machines.set(element.machineId, element);
        // console.log('element', this.machines);
        this.dataSource = new MatTableDataSource(Array.from(this.machines.values()));
      });
      if (this.dataSource) {
        this.dataSource.paginator = this.paginator;
        this.dataSource.sort = this.sort;
      }
    }, error => console.log(error));
  }

  machineHistory(id: string) {
    this.router.navigate(['machineHistory', id]);
  }

  applyFilter(filterValue: string) {
    this.dataSource.filter = filterValue.trim().toLowerCase();
    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  get isAdmin() {
    return this.currentUser && this.currentUser.role === Role.Admin;
  }

  get displayedColumns() {
    if (this.isAdmin) {
      const adminColumns: string[] = Object.assign([], this.columnNames);
      adminColumns.push('reset');
      adminColumns.push('stop');
      return adminColumns;
    } else {
      return this.columnNames;
    }
  }

  adminAction(orderId: string) {
    this.userService.makeAction(orderId).pipe(first()).subscribe(data => {
      console.log('data');
  });
  }

}
