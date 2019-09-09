import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { OrderDetailsComponent } from './order-details/order-details.component';
import { OrderListComponent } from './order-list/order-list.component';
import { OrderUpdatesComponent} from './order-updates/order-updates.component';

const routes: Routes = [
  { path: '', redirectTo: 'orders', pathMatch: 'full'},
  { path: 'orders', component: OrderListComponent },
  { path: 'updates', component: OrderUpdatesComponent },
  { path: 'orderStatus/:id', component: OrderDetailsComponent }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
