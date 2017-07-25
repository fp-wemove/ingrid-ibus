import { IndexDetail } from './+index-detail/index-detail.component';
import { IndexItem } from './index-item/index-item.component';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Rx';
import 'rxjs/add/operator/map';
import { environment } from '../../environments/environment';
import { SearchHit } from '../+search/SearchHit';
import { SearchHits } from '../+search/SearchHits';
import { HttpClient } from '@angular/common/http';

@Injectable()
export class IndexService {

  constructor(private http: HttpClient) {
  }

  getIndices(): Observable<IndexItem[]> {
    return this.http.get(environment.apiUrl + '/indices');
  }

  getIndexDetail(id: string, type: string): Observable<IndexDetail> {
    return this.http.get(environment.apiUrl + '/indices/' + id + '?type=' + type);
  }

  update(detail: IndexDetail) {
    // TODO: implement
  }

  deleteIndex(id: string): Observable<number> {
    return this.http.delete(environment.apiUrl + '/indices/' + id, {observe: 'response'})
      .map(response => response.status);
  }

  setActive(id: string, active: boolean) {
    let command = active ? 'activate' : 'deactivate';
    return this.http.put(environment.apiUrl + '/indices/' + encodeURIComponent(id) + '/' + command, null);
  }

  index(id: string) {
    return this.http.put(environment.apiUrl + '/indices/' + id + '/index', null, {observe: 'response'})
      .map(response => response.status);
  }

  search(query: string): Observable<SearchHits> {
    return this.http.get(environment.apiUrl + '/search?query=' + query);
  }

  getSearchDetail(indexId: string, hitId: string): Observable<SearchHit> {
    return this.http.get(environment.apiUrl + '/indices/' + indexId + '/' + hitId);
  }
}
