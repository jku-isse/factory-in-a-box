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
  orders: Observable<object[]>;

  constructor(private orderService: OrderService, private router: Router) { }

  ngOnInit() {
    this.reloadData();
  }

  reloadData() {
    this.orderService.getOrderList()
      .subscribe(data => {
        this.orders = data;
      }, error => console.log(error));
  }

  orderDetails(id: string) {
    this.router.navigate(['orderStatus', id]);
  }
}
