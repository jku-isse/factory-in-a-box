import { OrderService } from '../order.service';
import { OrderEvent } from '../events';
import { Component, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { DataService } from '../data.service';
import { User, Role } from '../_models';
import { AuthService, UserService } from '../_services';
import { first } from 'rxjs/operators';


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
    private userService: UserService
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
          // console.log('element', this.orders);
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

  adminAction(orderId: string) {
    this.userService.makeAction(orderId).pipe(first()).subscribe(data => {
      console.log('data');
  });
  }
}
