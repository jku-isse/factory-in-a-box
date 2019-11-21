import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class MachineService {

  private baseUrl = 'http://localhost:8080/';

  constructor(private _zone: NgZone, private http: HttpClient) { }

  getMachineList(): Observable<any> {
    return this.http.get(`${this.baseUrl}machines`);
  }

  getMachineHistory(id: string): Observable<any> {
    return this.http.get(`${this.baseUrl}machineHistory/${id}`);
  }

  getMachineUpdates(): Observable<any> {
    return Observable.create(observer =>  {
      const eventSource = this.getMachineEventStream();
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

  private getMachineEventStream(): EventSource {
    return new EventSource(`${this.baseUrl}machineEvents`);
  }

}
