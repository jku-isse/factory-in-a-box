import { OrderService } from '../_services/order.service';
import { OrderEvent } from '../_models/events';
import { Component, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { DataService } from '../_services/data.service';
import { User, Role } from '../_models';
import { AuthService, UserService } from '../_services';
import { first } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { DialogConfirmComponent } from '../dialog-confirm/dialog-confirm.component';
import { ActionRequest, DialogData } from '../_models/dialog-data';
import { MatSnackBar } from '@angular/material/snack-bar';


@Component({
  selector: 'app-order-list',
  templateUrl: './order-list.component.html',
  styleUrls: ['./order-list.component.css']
})
export class OrderListComponent implements OnInit {

  columnNames: string[] = ['orderId', 'eventType', 'machineId', 'message', 'process-button', 'history-button'];
  orders: Map<string, OrderEvent> = new Map<string, OrderEvent>();
  dataSource: MatTableDataSource<OrderEvent>;
  count: Map<string, number>;
  currentUser: User;

  @ViewChild(MatPaginator, { static: true }) paginator: MatPaginator;
  @ViewChild(MatSort, { static: true }) sort: MatSort;

  constructor(
    private orderService: OrderService,
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
    this.orderService.getOrderUpdates().subscribe(
      sseEvent => {
        const json = JSON.parse(sseEvent.data);
        this.orders.set(json.orderId, json);
        this.dataSource = new MatTableDataSource(Array.from(this.orders.values()));
        if (this.dataSource) {
          this.dataSource.paginator = this.paginator;
          this.dataSource.sort = this.sort;
        }
      },
      err => { console.log('Error receiving SSE', err); },
      () => console.log('SSE stream completed')
    );
    this.reloadData();
    this.data.currentCount.subscribe(count => this.count = count);
  }

  reloadData() {
    this.orderService.getOrderList()
      .subscribe(data => {
        data.forEach(element => {
          this.orders.set(element.orderId, element);
          this.dataSource = new MatTableDataSource(Array.from(this.orders.values()));
        });
        if (this.dataSource) {
          this.dataSource.paginator = this.paginator;
          this.dataSource.sort = this.sort;
        }
      }, error => console.log(error));
  }

  orderDetails(id: string) {
    this.router.navigate(['orderStatus', id]);
  }

  orderHistory(id: string) {
    this.router.navigate(['orderHistory', id]);
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
      adminColumns.push('adminAction');
      return adminColumns;
    } else {
      return this.columnNames;
    }
  }

  deleteOrder(orderId: string) {
    const msg: DialogData = new ActionRequest('delete', orderId);
    this.userService.action(msg)
      .subscribe(
        resp => {
          if (resp.status < 400) {
            this.openSnackBar('Deleting of order initiated!');
          } else {
            this.openSnackBar('Unauthorized');
          }
        },
        error => {
          this.openSnackBar('Error: ' + error);
        }
      );
  }

  openDialog(orderId: string): void {
    const dialogRef = this.dialog.open(DialogConfirmComponent, {
      width: '300px',
      data: {action: 'delete', id: orderId}
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.deleteOrder(orderId);
      }
    });
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
