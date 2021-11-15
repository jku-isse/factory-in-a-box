import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { OrderDetailsComponent } from './order-details/order-details.component';
import { OrderHistoryComponent } from './order-history/order-history.component';
import { OrderListComponent } from './order-list/order-list.component';
import { MachineListComponent } from './machine-list/machine-list.component';
import { MachineHistoryComponent } from './machine-history/machine-history.component';
import { LoginComponent } from './login/login.component';
import { AuthGuard } from './_helpers/auth.guard';

const routes: Routes = [
  { path: '', redirectTo: 'orders', pathMatch: 'full', canActivate: [AuthGuard] },
  { path: 'login', component: LoginComponent },
  { path: 'orders', component: OrderListComponent, canActivate: [AuthGuard] },
  { path: 'orderStatus/:id', component: OrderDetailsComponent, canActivate: [AuthGuard] },
  { path: 'orderHistory/:id', component: OrderHistoryComponent, canActivate: [AuthGuard] },
  { path: 'machines', component: MachineListComponent, canActivate: [AuthGuard] },
  { path: 'machineHistory/:id', component: MachineHistoryComponent, canActivate: [AuthGuard] },
  { path: '**', redirectTo: 'orders' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
