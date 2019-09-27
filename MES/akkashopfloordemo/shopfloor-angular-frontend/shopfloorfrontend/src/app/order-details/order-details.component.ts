
import { Order } from '../order';
import { Component, OnInit, Input } from '@angular/core';
import { OrderService } from '../order.service';
import { OrderListComponent } from '../order-list/order-list.component';
import { Router, ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-order-details',
  templateUrl: './order-details.component.html',
  styleUrls: ['./order-details.component.css']
})
export class OrderDetailsComponent implements OnInit {

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
    this.orderService.getOrder(this.id)
      .subscribe(data => {
        console.log('Order Details Component', data);
        this.order = data.stepStatus;
      }, error => console.log(error));
  }

  list() {
    this.router.navigate(['orders']);
  }

}
