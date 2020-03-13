import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class OrderService {

  constructor(private _zone: NgZone, private http: HttpClient) { }

  getOrder(id: string): Observable<any> {
    return this.http.get(`${environment.apiUrl}/order/${id}`);
  }

  getOrderHistory(id: string): Observable<any> {
    return this.http.get(`${environment.apiUrl}/orderHistory/${id}`);
  }

  getOrderList(): Observable<any> {
    return this.http.get(`${environment.apiUrl}/orders`);
  }

  getOrderUpdates(): Observable<any> {
    return new Observable(observer =>  {
      const eventSource = this.getOrderEventStream();
      eventSource.onmessage = event => {
        this._zone.run(() => {
          observer.next(event);
        });
      };

      eventSource.onerror = error => {
        this._zone.run(() => {
          console.log('SSE error ', eventSource);
          observer.error(error);
          eventSource.close();
        });
      };

      return () => {
        eventSource.close();
      };

    });
  }

  getProcessUpdates(orderId: string): Observable<any> {
    return new Observable(observer =>  {
      const eventSource = this.getProcessEventStream(orderId);
      eventSource.onmessage = event => {
        this._zone.run(() => {
          observer.next(event);
        });
      };
      eventSource.onerror = error => {
        this._zone.run(() => {
          observer.error(error);
          eventSource.close();
        });
      };

      return () => {
        eventSource.close();
      };

    });
  }

  getOrderEventStream(): EventSource {
    return new EventSource(`${environment.apiUrl}/orderevents`);
  }

  getProcessEventStream(orderId: string): EventSource {
    return new EventSource(`${environment.apiUrl}/processevents/${orderId}`);
  }

}
