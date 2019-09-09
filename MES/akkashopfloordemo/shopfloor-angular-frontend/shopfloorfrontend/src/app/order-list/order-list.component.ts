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
  orderIds: Observable<string[]>;

  constructor(private orderService: OrderService, private router: Router) { }

  ngOnInit() {
    this.reloadData();
  }

  reloadData() {
    this.orderIds = this.orderService.getOrderList();
  }

  orderDetails(id: string) {
    this.router.navigate(['orderStatus', id]);
  }
}
