import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { OrderDetailsComponent } from './order-details/order-details.component';
import { OrderHistoryComponent } from './order-history/order-history.component';
import { OrderListComponent } from './order-list/order-list.component';
import { MachineListComponent } from './machine-list/machine-list.component';
import { MachineHistoryComponent } from './machine-history/machine-history.component';

const routes: Routes = [
  { path: '', redirectTo: 'orders', pathMatch: 'full'},
  { path: 'orders', component: OrderListComponent },
  { path: 'orderStatus/:id', component: OrderDetailsComponent },
  { path: 'orderHistory/:id', component: OrderHistoryComponent },
  { path: 'machines', component: MachineListComponent },
  { path: 'machineHistory/:id', component: MachineHistoryComponent }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
