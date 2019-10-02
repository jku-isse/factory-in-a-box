import { Order } from '../order';
import { Component, OnInit } from '@angular/core';
import { OrderService } from '../order.service';
import { Router, ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-order-history',
  templateUrl: './order-history.component.html',
  styleUrls: ['./order-history.component.css']
})
export class OrderHistoryComponent implements OnInit {
  displayedColumns: string[] = ['status', 'machine'];
  orders: Map<string, Order> = new Map<string, Order>();
  orderId: string;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private orderService: OrderService) { }

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      this.orderId = params.get('id');
    });
    this.orderService.getOrderHistory(this.orderId)
      .subscribe(data => {
        data.forEach(element => {
          this.orders.set(element.machineId + element.eventType + element.timestamp, element);
        });
      }, error => console.log(error));
  }

  list() {
    this.router.navigate(['orders']);
  }

  getOrdersAsArray() {
    console.log('Result', Array.from(this.orders.values()));
    return Array.from(this.orders.values());
  }

}
