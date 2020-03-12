import { Component, OnInit, ViewChild, OnDestroy } from '@angular/core';
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
import { MatDialog } from '@angular/material/dialog';
import { DialogConfirmComponent } from '../dialog-confirm/dialog-confirm.component';
import { DialogData, ActionRequest } from '../_models/dialog-data';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-machine-list',
  templateUrl: './machine-list.component.html',
  styleUrls: ['./machine-list.component.css']
})
export class MachineListComponent implements OnInit, OnDestroy {

  columnNames: string[] = ['machineId', 'eventType', 'state', 'message', 'history'];
  machines: Map<string, MachineEvent> = new Map<string, MachineEvent>();
  dataSource: MatTableDataSource<MachineEvent>;
  count: Map<string, number>;
  currentUser: User;
  subscriptions: Subscription[] = [];

  @ViewChild(MatPaginator, {static: true}) paginator: MatPaginator;
  @ViewChild(MatSort, {static: true}) sort: MatSort;

  constructor(
    private machineService: MachineService,
    private router: Router,
    private data: DataService,
    private authenticationService: AuthService,
    private userService: UserService,
    public dialog: MatDialog,
    private _snackBar: MatSnackBar
  ) {
    this.currentUser = this.authenticationService.currentUserValue;
  }

  ngOnInit() {
    this.subscribe();
    this.reloadData();
    const sub = this.data.currentMachineCount.subscribe(count => this.count = count);
    this.subscriptions.push(sub);
  }

  ngOnDestroy() {
    this.subscriptions.forEach((subscription) => subscription.unsubscribe());
  }

  private subscribe() {
    const sub = this.machineService.getMachineUpdates().subscribe(
      sseEvent => {
        const json = JSON.parse(sseEvent.data);
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
    this.subscriptions.push(sub);
  }

  private reloadData() {
    const sub = this.machineService.getMachineList().subscribe(data => {
      data.forEach(element => {
        this.machines.set(element.machineId, element);
        this.dataSource = new MatTableDataSource(Array.from(this.machines.values()));
      });
      if (this.dataSource) {
        this.dataSource.paginator = this.paginator;
        this.dataSource.sort = this.sort;
      }
    }, error => console.log(error));
    this.subscriptions.push(sub);
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

  adminAction(machineId: string, action: string) {
    const msg: DialogData = new ActionRequest(action, machineId);
    const sub = this.userService.action(msg)
      .subscribe(
        resp => {
          if (resp.status < 400) {
            this.openSnackBar(action + ' initiated!');
          } else {
            this.openSnackBar('Unauthorized');
          }
        },
        error => {
          this.openSnackBar('Error: ' + error);
        }
      );
    this.subscriptions.push(sub);
  }

  openDialog(machineId: string, action: string): void {
    const dialogRef = this.dialog.open(DialogConfirmComponent, {
      width: '300px',
      data: {action, id: machineId}
    });

    const sub = dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.adminAction(machineId, action);
      }
    });

    this.subscriptions.push(sub);
  }

  openSnackBar(message: string) {
    this._snackBar.open(message, 'OK', {
      duration: 5000,
    });
  }

  decode(s: string): string {
    return decodeURIComponent(s);
  }

}
