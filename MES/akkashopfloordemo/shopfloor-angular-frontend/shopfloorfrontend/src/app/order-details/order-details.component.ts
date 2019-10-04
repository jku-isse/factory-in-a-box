
import { Order } from '../order';
import { Component, OnInit, Input } from '@angular/core';
import { OrderService } from '../order.service';
import { Router, ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-order-details',
  templateUrl: './order-details.component.html',
  styleUrls: ['./order-details.component.css']
})
export class OrderDetailsComponent implements OnInit {
  displayedColumns: string[] = ['jobid', 'jobstatus'];
  id: string;
  order: Order;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private orderService: OrderService) { }

  ngOnInit() {
    this.order = new Order();
    this.route.paramMap.subscribe(params => {
      this.id = params.get('id');
    });
    this.orderService.getProcessUpdates(this.id).subscribe(
      sseEvent => {
        const json = JSON.parse(sseEvent.data);
        this.order.jobStatus = json.stepStatus;
      },
      err => { console.log('Error receiving SSE in Details', err); },
      () => console.log('SSE stream completed')
    );
    this.orderService.getOrder(this.id)
      .subscribe(data => {
        this.order.jobStatus = data.stepStatus;
        this.order.orderId = data.orderId;
      }, error => console.log(error));
  }

  list() {
    this.router.navigate(['orders']);
  }

  getJobsAsArray() {
    if (typeof this.order.jobStatus === 'undefined') {
      return [];
    } else {
      let c: object[] = [];
      for (const key of Object.keys(this.order.jobStatus)) {
        c.push({id: key, status: this.order.jobStatus[key]});
      }
      return c;
    }
  }

}
