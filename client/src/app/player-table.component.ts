import {HttpClient} from '@angular/common/http';
import {Component, ViewChild, AfterViewInit} from '@angular/core';
import {MatPaginator} from '@angular/material/paginator';
import {MatSort} from '@angular/material/sort';
import {merge, Observable, of as observableOf} from 'rxjs';
import {catchError, map, startWith, switchMap} from 'rxjs/operators';

/**
 * @title Table retrieving data through HTTP
 */
@Component({
  selector: 'player-table',
  styleUrls: ['player-table.component.css'],
  templateUrl: 'player-table.component.html',
})
export class PlayerTableComponent implements AfterViewInit {
  displayedColumns: string[] = ['rank', 'id', 'nickname', 'score'];
  playerApi: PlayerApi | null;
  filteredAndPagedIssues: Observable<Player[]>;

  resultsLength = 0;
  isLoadingResults = true;

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  constructor(private _httpClient: HttpClient) {}

  ngAfterViewInit() {
    this.playerApi = new PlayerApi(this._httpClient);

    this.filteredAndPagedIssues = merge(this.sort.sortChange, this.paginator.page)
      .pipe(
        startWith({}),
        switchMap(() => {
          this.isLoadingResults = true;
          return this.playerApi!.getPlayer(this.paginator.pageIndex);
        }),
        map(data => {
          // Flip flag to show that loading has finished.
          this.isLoadingResults = false;
          this.resultsLength = data.count;
          return data.players;
        }),
        catchError(() => {
          this.isLoadingResults = false;
          return observableOf([]);
        })
      );
  }

  resetPaging(): void {
    this.paginator.pageIndex = 0;
  }
}

export interface PlayerResult {
  players: Player[];
  count: number;
}

export interface Player {
  _id: string;
  nickname: string;
  score: number;
  rank: number;
}

export class PlayerApi {
  constructor(private _httpClient: HttpClient) {}

  getPlayer(page: number): Observable<PlayerResult> {
    const href = '/tournament-players';
    const requestUrl =
        `${href}?q=repo:angular/components&page=${page + 1}`;
    return this._httpClient.get<PlayerResult>(requestUrl);
  }
}


/**  Copyright 2021 Google LLC. All Rights Reserved.
    Use of this source code is governed by an MIT-style license that
    can be found in the LICENSE file at http://angular.io/license */
