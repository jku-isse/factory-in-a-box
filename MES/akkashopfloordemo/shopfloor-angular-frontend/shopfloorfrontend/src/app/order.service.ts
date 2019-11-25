import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class OrderService {

  private baseUrl = 'http://localhost:8080/';

  constructor(private _zone: NgZone, private http: HttpClient) { }

  getOrder(id: string): Observable<any> {
    return this.http.get(`${this.baseUrl}order/${id}`);
  }

  getOrderHistory(id: string): Observable<any> {
    return this.http.get(`${this.baseUrl}orderHistory/${id}`);
  }

  getOrderList(): Observable<any> {
    return this.http.get(`${this.baseUrl}orders`);
  }

  getOrderUpdates(): Observable<any> {
    return Observable.create(observer =>  {
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

    });
  }

  getProcessUpdates(orderId: string): Observable<any> {
    return Observable.create(observer =>  {
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

    });
  }

  getOrderEventStream(): EventSource {
    return new EventSource(`${this.baseUrl}orderevents`);
  }

  getProcessEventStream(orderId: string): EventSource {
    return new EventSource(`${this.baseUrl}processevents/${orderId}`);
  }

}
