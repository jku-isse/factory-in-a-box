
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
  displayedColumns: string[] = ['jobid', 'jobstatus', 'caps'];
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
        // console.log('SSE', json);
        if (typeof this.order.jobStatus === 'undefined') {
          this.order.jobStatus = json.stepStatus;
        } else {
          for (const key in json.stepStatus) {
            if (json.stepStatus.hasOwnProperty(key)) {
              this.order.jobStatus[key] = json.stepStatus[key];
            }
          }
        }
        if (typeof this.order.capabilities === 'undefined') {
          this.order.capabilities = json.capabilities;
        } else {
          for (const key in json.capabilities) {
            if (json.capabilities.hasOwnProperty(key)) {
              this.order.capabilities[key] = json.capabilities[key];
            }
          }
        }
      },
      err => { console.log('Error receiving SSE in Details', err); },
      () => console.log('SSE stream completed')
    );
    this.orderService.getOrder(this.id)
      .subscribe(data => {
        // console.log('Update', data);
        if (typeof this.order.jobStatus === 'undefined') {
          this.order.jobStatus = data.stepStatus;
        } else {
          for (const key in data.stepStatus) {
            if (data.stepStatus.hasOwnProperty(key)) {
              this.order.jobStatus.set(key, data.stepStatus[key]);
            }
          }
        }
        if (typeof this.order.capabilities === 'undefined') {
          this.order.capabilities = data.capabilities;
        } else {
          for (const key in data.capabilities) {
            if (data.capabilities.hasOwnProperty(key)) {
              this.order.capabilities.set(key, data.capabilities[key]);
            }
          }
        }
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
      const c: object[] = [];
      for (const key of Object.keys(this.order.jobStatus)) {
        let caps: string;
        if (typeof this.order.capabilities !== 'undefined') {
          caps = this.order.capabilities[key];
        }
        c.push({id: key, status: this.order.jobStatus[key], caps});
      }
      return c;
    }
  }

}
