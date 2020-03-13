import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class MachineService {

  constructor(private _zone: NgZone, private http: HttpClient) { }

  getMachineList(): Observable<any> {
    return this.http.get(`${environment.apiUrl}/machines`);
  }

  getMachineHistory(id: string): Observable<any> {
    return this.http.get(`${environment.apiUrl}/machineHistory/${id}`);
  }

  getMachineUpdates(): Observable<any> {
    return new Observable(observer =>  {
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

      return () => {
        eventSource.close();
      };

    });
  }

  private getMachineEventStream(): EventSource {
    return new EventSource(`${environment.apiUrl}/machineEvents`);
  }

}
