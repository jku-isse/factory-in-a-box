import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { OrderDetailsComponent } from './order-details/order-details.component';
import { OrderHistoryComponent } from './order-history/order-history.component';
import { OrderListComponent } from './order-list/order-list.component';

const routes: Routes = [
  { path: '', redirectTo: 'orders', pathMatch: 'full'},
  { path: 'orders', component: OrderListComponent },
  { path: 'orderStatus/:id', component: OrderDetailsComponent },
  { path: 'orderHistory/:id', component: OrderHistoryComponent }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
