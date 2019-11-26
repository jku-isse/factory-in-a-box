import { Component, OnInit } from '@angular/core';
import { OrderService } from '../order.service';
import { Router, ActivatedRoute } from '@angular/router';
import { OrderEvent } from '../events';

@Component({
  selector: 'app-order-history',
  templateUrl: './order-history.component.html',
  styleUrls: ['./order-history.component.css']
})
export class OrderHistoryComponent implements OnInit {
  displayedColumns: string[] = ['status', 'machine', 'message', 'time'];
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
    this.orderService.getOrderUpdates().subscribe(
      sseEvent => {
        const json = JSON.parse(sseEvent.data);
        if (json.orderId === this.orderId) {
          json.timestamp = this.parseTimestamp(json.timestamp);
          this.orders.set(json.machineId + json.eventType + json.timestamp, json);
          this.setLatest(json.timestamp);
        }
      },
      err => { console.log('Error receiving SSE in History', err); },
      () => console.log('SSE stream completed')
    );
    this.orderService.getOrderHistory(this.orderId)
      .subscribe(data => {
        data.forEach(element => {
          element.prettyTimestamp = this.parseTimestamp(element.timestamp);
          this.orders.set(element.machineId + element.eventType + element.timestamp, element);
          this.setLatest(element.timestamp);
          // console.log('History data', element);
        });
      }, error => console.log(error));
  }

  list() {
    this.router.navigate(['orders']);
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

}
