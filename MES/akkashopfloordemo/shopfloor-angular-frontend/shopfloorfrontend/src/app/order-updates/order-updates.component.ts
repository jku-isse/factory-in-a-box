import { OrderListComponent } from '../order-list/order-list.component';
import { Observable } from "rxjs";
import { OrderService } from "../order.service";
import { Order } from "../order";
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-order-updates',
  templateUrl: './order-updates.component.html',
  styleUrls: ['./order-updates.component.css']
})
export class OrderUpdatesComponent implements OnInit {

  orders: Map<string, Order> = new Map<string, Order>();

  constructor(
    private router: Router,
    private orderService: OrderService) { }

  ngOnInit() {
    // this.orders = new Map<string, Order>();
    this.orderService.getOrderUpdates().subscribe(
      sseEvent => {
        console.log('Received SSE', sseEvent);
        const json = JSON.parse(sseEvent.data);
        const orderId: string = json.orderId;
        const orderStatus: Order = json.status;
        this.orders.set(orderId, orderStatus);
      },
      err => { console.log('Error receiving SSE', err); },
      () => console.log('SSE stream completed')
    );
  }

}
