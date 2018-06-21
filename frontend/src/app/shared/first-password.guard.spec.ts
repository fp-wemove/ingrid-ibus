/*-
 * **************************************************-
 * ingrid-ibus-frontend
 * ==================================================
 * Copyright (C) 2014 - 2018 wemove digital solutions GmbH
 * ==================================================
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 * 
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl5
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * **************************************************#
 */
import { TestBed, async, inject } from '@angular/core/testing';

import { FirstPasswordGuard } from './first-password.guard';

describe('FirstPasswordGuard', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [FirstPasswordGuard]
    });
  });

  it('should ...', inject([FirstPasswordGuard], (guard: FirstPasswordGuard) => {
    expect(guard).toBeTruthy();
  }));
});
