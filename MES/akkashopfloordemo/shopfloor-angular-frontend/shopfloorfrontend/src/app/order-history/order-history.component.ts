import { Component, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { OrderService } from '../_services/order.service';
import { Router, ActivatedRoute } from '@angular/router';
import { OrderEvent } from '../_models/events';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { DataService } from '../_services/data.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-order-history',
  templateUrl: './order-history.component.html',
  styleUrls: ['./order-history.component.css']
})
export class OrderHistoryComponent implements OnInit, OnDestroy {
  displayedColumns: string[] = ['status', 'machine', 'message', 'time'];
  orders: Map<string, OrderEvent> = new Map<string, OrderEvent>();
  orderId: string;
  latest = '';
  dataSource: MatTableDataSource<OrderEvent>;
  count: Map<string, number>;
  subscriptions: Subscription[] = [];

  @ViewChild(MatPaginator, { static: true }) paginator: MatPaginator;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private orderService: OrderService,
    private data: DataService) { }

  ngOnInit() {
    const sub1 = this.route.paramMap.subscribe(params => {
      this.orderId = params.get('id');
    });
    const sub2 = this.orderService.getOrderUpdates().subscribe(
      sseEvent => {
        const json = JSON.parse(sseEvent.data);
        if (json.orderId === this.orderId) {
          json.prettyTimestamp = this.parseTimestamp(json.timestamp);
          this.orders.set(json.machineId + json.eventType + json.timestamp, json);
          this.setLatest(json.timestamp);
          this.dataSource = new MatTableDataSource(this.getOrdersAsArray());
          if (this.dataSource) {
            this.dataSource.paginator = this.paginator;
          }
        }
        this.newCount();
      },
      err => { console.log('Error receiving SSE in History', err); },
      () => console.log('SSE stream completed')
    );
    const sub3 = this.orderService.getOrderHistory(this.orderId)
      .subscribe(data => {
        data.forEach(element => {
          element.prettyTimestamp = this.parseTimestamp(element.timestamp);
          this.orders.set(element.machineId + element.eventType + element.timestamp, element);
          this.setLatest(element.timestamp);
          this.dataSource = new MatTableDataSource(this.getOrdersAsArray());
        });
        if (this.dataSource) {
          this.dataSource.paginator = this.paginator;
        }
        this.newCount();
      }, error => console.log(error));
    const sub4 = this.data.currentCount.subscribe(count => this.count = count);
    this.subscriptions.push(sub1);
    this.subscriptions.push(sub2);
    this.subscriptions.push(sub3);
    this.subscriptions.push(sub4);
  }

  ngOnDestroy() {
    this.subscriptions.forEach((subscription) => subscription.unsubscribe());
  }

  list() {
    this.router.navigate(['orders']);
  }

  status(id: string) {
    if (id) {
      this.router.navigate(['orderStatus', id]);
    }
  }

  getOrdersAsArray() {
    // console.log('Debug flag', this.orders.values());
    return Array.from(this.orders.values());
  }

  parseTimestamp(timestamp: string): string {
    const d: Date = new Date(Date.parse(timestamp.substring(0, timestamp.indexOf('+'))));
    return d.toLocaleTimeString() + `.${d.getMilliseconds()}`;
  }

  setLatest(timestamp: string) {
    if (this.latest === '' ||
      Date.parse(this.latest.substring(0, this.latest.indexOf('+'))) <= Date.parse(timestamp.substring(0, timestamp.indexOf('+')))) {
      this.latest = timestamp;
    }
  }

  applyFilter(filterValue: string) {
    console.log(this.dataSource);
    this.dataSource.filter = filterValue.trim().toLowerCase();
    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  newCount() {
    this.data.changeCount(new Map<string, number>(this.count), this.orderId, this.orders.size);
  }

  decode(s: string): string {
    return decodeURIComponent(s);
  }

}
