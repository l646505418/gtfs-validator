/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mobilitydata.gtfsvalidator.table;

import java.time.ZoneId;
import org.mobilitydata.gtfsvalidator.annotation.*;

@GtfsTable("stops.txt")
@Required
public interface GtfsStopSchema extends GtfsEntity {
  @FieldType(FieldTypeEnum.ID)
  @PrimaryKey
  @Required
  String stopId();

  String stopCode();

  @MixedCase
  @ConditionallyRequired
  String stopName();

  String ttsStopName();

  @MixedCase
  String stopDesc();

  @FieldType(FieldTypeEnum.LATITUDE)
  @ConditionallyRequired
  double stopLat();

  @FieldType(FieldTypeEnum.LONGITUDE)
  @ConditionallyRequired
  double stopLon();

  @FieldType(FieldTypeEnum.ID)
  @Index
  @ConditionallyRequired
  String zoneId();

  @FieldType(FieldTypeEnum.URL)
  String stopUrl();

  GtfsLocationType locationType();

  @FieldType(FieldTypeEnum.ID)
  @Index
  @ConditionallyRequired
  @ForeignKey(table = "stops.txt", field = "stop_id")
  String parentStation();

  @FieldType(FieldTypeEnum.TIMEZONE)
  ZoneId stopTimezone();

  GtfsWheelchairBoarding wheelchairBoarding();

  @FieldType(FieldTypeEnum.ID)
  @ForeignKey(table = "levels.txt", field = "level_id")
  String levelId();

  String platformCode();
}
