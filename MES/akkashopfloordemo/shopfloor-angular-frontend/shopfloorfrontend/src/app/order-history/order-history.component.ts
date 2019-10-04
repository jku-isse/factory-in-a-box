import { Component, OnInit } from '@angular/core';
import { OrderService } from '../order.service';
import { Router, ActivatedRoute } from '@angular/router';
import { OrderEvent } from '../orderevent';

@Component({
  selector: 'app-order-history',
  templateUrl: './order-history.component.html',
  styleUrls: ['./order-history.component.css']
})
export class OrderHistoryComponent implements OnInit {
  displayedColumns: string[] = ['status', 'machine', 'time'];
  orders: Map<string, OrderEvent> = new Map<string, OrderEvent>();
  orderId: string;
  latest = '';

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
          if (this.latest === '' || Date.parse(this.latest.substring(0, 28)) <= Date.parse(element.timestamp.substring(0, 28))) {
            this.latest = element.timestamp;
          }
        });
      }, error => console.log(error));
  }

  list() {
    this.router.navigate(['orders']);
  }

  getOrdersAsArray() {
    return Array.from(this.orders.values());
  }

}
