import {OrderDetailsComponent} from '../order-details/order-details.component';
import { Observable } from "rxjs";
import { OrderService } from "../order.service";
import { Order } from "../order";
import { Component, OnInit } from "@angular/core";
import { Router } from '@angular/router';

@Component({
  selector: 'app-order-list',
  templateUrl: './order-list.component.html',
  styleUrls: ['./order-list.component.css']
})
export class OrderListComponent implements OnInit {
  displayedColumns: string[] = ['order', 'status', 'machine', 'button'];
  // orders: Observable<object[]>;
  orders: Map<string, Order> = new Map<string, Order>();

  constructor(private orderService: OrderService, private router: Router) { }

  ngOnInit() {
    this.orderService.getOrderUpdates().subscribe(
      sseEvent => {
        const json = JSON.parse(sseEvent.data);
        // console.log('Received SSE', json);
        this.orders.set(json.orderId, json);
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
          // console.log('Order:', element);
          this.orders.set(element.orderId, element);
        });
      }, error => console.log(error));
  }

  orderDetails(id: string) {
    this.router.navigate(['orderStatus', id]);
  }

  getOrdersAsArray() {
    return Array.from(this.orders.values());
  }
}
