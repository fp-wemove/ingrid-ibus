import { RouterTestingModule } from '@angular/router/testing';
/* tslint:disable:no-unused-variable */
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DebugElement, LOCALE_ID } from '@angular/core';

import { IndexItem, IndexItemComponent } from './index-item.component';
import { ConfirmationPopoverModule } from 'angular-confirmation-popover';
import { IndexService } from '../index.service';
import { IndexService as IndexServiceMock } from '../index-mock.service';

let ITEM_EXAMPLE: IndexItem = {
  id: '123',
  name: 'myId',
  longName: 'myName',
  lastIndexed: '2014-03-12T13:37:27+00:00',
  types: []
};

describe('IndexItemComponent', () => {
  let component: IndexItemComponent;
  let debug: DebugElement;
  let fixture: ComponentFixture<IndexItemComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        ConfirmationPopoverModule.forRoot()
      ],
      declarations: [IndexItemComponent],
      providers: [
        {provide: IndexService, use: IndexServiceMock},
        {provide: LOCALE_ID, useValue: 'de'}
      ]
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(IndexItemComponent);
    component = fixture.componentInstance;
    debug = fixture.debugElement;
    component.data = ITEM_EXAMPLE;
    fixture.detectChanges();
  });

  it('should create a panel with index information', () => {
    expect(component).toBeTruthy();

    expect(debug.query(By.css('.panel .index-id')).nativeElement.textContent).toBe('myId');
    expect(debug.query(By.css('.panel .index-name')).nativeElement.textContent).toBe('myName');
    expect(debug.query(By.css('.panel .index-last-indexed')).nativeElement.textContent).toBe('12. März 2014, 14:37:27');
  });
});
