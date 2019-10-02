import {OrderDetailsComponent} from '../order-details/order-details.component';
import { Observable } from "rxjs";
import { OrderService } from "../order.service";
import { OrderEvent } from "../orderevent";
import { Component, OnInit } from "@angular/core";
import { Router } from '@angular/router';
import {Sort} from '@angular/material/sort';

@Component({
  selector: 'app-order-list',
  templateUrl: './order-list.component.html',
  styleUrls: ['./order-list.component.css']
})
export class OrderListComponent implements OnInit {
  displayedColumns: string[] = ['order', 'status', 'machine', 'process-button', 'history-button'];
  sortedData: OrderEvent[];
  orders: Map<string, OrderEvent> = new Map<string, OrderEvent>();

  constructor(private orderService: OrderService, private router: Router) { }

  ngOnInit() {
    this.orderService.getOrderUpdates().subscribe(
      sseEvent => {
        const json = JSON.parse(sseEvent.data);
        // console.log('Received SSE', json);
        this.orders.set(json.orderId, json);
        this.sortedData = Array.from(this.orders.values());
      },
      err => { console.log('Error receiving SSE', err); },
      () => console.log('SSE stream completed')
    );
    this.reloadData();
  }

  reloadData() {
    this.orderService.getOrderList()
      .subscribe(data => {
        data.forEach(element => {
          console.log('Order:', element);
          this.orders.set(element.orderId, element);
          this.sortedData = Array.from(this.orders.values());
        });
      }, error => console.log(error));
  }

  orderDetails(id: string) {
    this.router.navigate(['orderStatus', id]);
  }

  orderHistory(id: string) {
    this.router.navigate(['orderHistory', id]);
  }

  sortData(sort: Sort) {
    const data = Array.from(this.orders.values()).slice();
    if (!sort.active || sort.direction === '') {
      this.sortedData = data;
      return;
    }

    this.sortedData = data.sort((a, b) => {
      const isAsc = sort.direction === 'asc';
      switch (sort.active) {
        case 'orderId': return compare(a.orderId, b.orderId, isAsc);
        case 'eventType': return compare(a.eventType, b.eventType, isAsc);
        case 'machineId': return compare(a.machineId, b.machineId, isAsc);
        case 'timestamp': return compare(a.timestamp, b.timestamp, isAsc);
        default: return 0;
      }
    });
  }
}

function compare(a: number | string, b: number | string, isAsc: boolean) {
  return (a < b ? -1 : 1) * (isAsc ? 1 : -1);
}
