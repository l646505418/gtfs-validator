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
package org.mobilitydata.gtfsvalidator.validator;

import static org.mobilitydata.gtfsvalidator.notice.SeverityLevel.ERROR;

import com.google.common.collect.Multimaps;
import java.util.List;
import javax.inject.Inject;
import org.mobilitydata.gtfsvalidator.annotation.GtfsValidationNotice;
import org.mobilitydata.gtfsvalidator.annotation.GtfsValidationNotice.FileRefs;
import org.mobilitydata.gtfsvalidator.annotation.GtfsValidationNotice.UrlRef;
import org.mobilitydata.gtfsvalidator.annotation.GtfsValidator;
import org.mobilitydata.gtfsvalidator.notice.NoticeContainer;
import org.mobilitydata.gtfsvalidator.notice.SeverityLevel;
import org.mobilitydata.gtfsvalidator.notice.ValidationNotice;
import org.mobilitydata.gtfsvalidator.table.GtfsStopTime;
import org.mobilitydata.gtfsvalidator.table.GtfsStopTimeSchema;
import org.mobilitydata.gtfsvalidator.table.GtfsStopTimeTableContainer;
import org.mobilitydata.gtfsvalidator.type.GtfsTime;

/**
 * Validates departure_time and arrival_time fields in "stop_times.txt".
 *
 * <p>Generated notices:
 *
 * <ul>
 *   <li>{@link StopTimeWithOnlyArrivalOrDepartureTimeNotice} - a single departure_time or
 *       arrival_time is defined for a row (both or none are expected)
 *   <li>{@link StopTimeWithArrivalBeforePreviousDepartureTimeNotice} - prev(arrival_time) &lt;
 *       curr(departure_time)
 * </ul>
 */
@GtfsValidator
public class StopTimeArrivalAndDepartureTimeValidator extends FileValidator {

  private final GtfsStopTimeTableContainer table;

  @Inject
  StopTimeArrivalAndDepartureTimeValidator(GtfsStopTimeTableContainer table) {
    this.table = table;
  }

  @Override
  public void validate(NoticeContainer noticeContainer) {
    for (List<GtfsStopTime> stopTimeList : Multimaps.asMap(table.byTripIdMap()).values()) {
      int previousDepartureRow = -1;
      for (int i = 0; i < stopTimeList.size(); ++i) {
        GtfsStopTime stopTime = stopTimeList.get(i);
        final boolean hasDeparture = stopTime.hasDepartureTime();
        final boolean hasArrival = stopTime.hasArrivalTime();
        if (hasArrival != hasDeparture) {
          noticeContainer.addValidationNotice(
              new StopTimeWithOnlyArrivalOrDepartureTimeNotice(
                  stopTime.csvRowNumber(),
                  stopTime.tripId(),
                  stopTime.stopSequence(),
                  hasArrival
                      ? GtfsStopTime.ARRIVAL_TIME_FIELD_NAME
                      : GtfsStopTime.DEPARTURE_TIME_FIELD_NAME));
        }
        if (hasArrival
            && previousDepartureRow != -1
            && stopTime
                .arrivalTime()
                .isBefore(stopTimeList.get(previousDepartureRow).departureTime())) {
          noticeContainer.addValidationNotice(
              new StopTimeWithArrivalBeforePreviousDepartureTimeNotice(
                  stopTime.csvRowNumber(),
                  stopTimeList.get(previousDepartureRow).csvRowNumber(),
                  stopTime.tripId(),
                  stopTime.arrivalTime(),
                  stopTimeList.get(previousDepartureRow).departureTime()));
        }
        if (hasDeparture) {
          previousDepartureRow = i;
        }
      }
    }
  }

  /**
   * Two {@code GtfsTime} are out of order
   *
   * <p>Severity: {@code SeverityLevel.ERROR}
   */
  @GtfsValidationNotice(
      severity = ERROR,
      files = @FileRefs(GtfsStopTimeSchema.class),
      urls = {
        @UrlRef(
            label = "Original Python validator implementation",
            url = "https://github.com/google/transitfeed")
      })
  static class StopTimeWithArrivalBeforePreviousDepartureTimeNotice extends ValidationNotice {

    /** The row number of the faulty record. */
    private final int csvRowNumber;

    /** The row of the previous stop time. */
    private final long prevCsvRowNumber;

    /** The trip_id associated to the faulty record. */
    private final String tripId;

    /** Arrival time at the faulty record. */
    private final GtfsTime arrivalTime;

    /** Departure time at the previous stop time. */
    private final GtfsTime departureTime;

    StopTimeWithArrivalBeforePreviousDepartureTimeNotice(
        int csvRowNumber,
        long prevCsvRowNumber,
        String tripId,
        GtfsTime arrivalTime,
        GtfsTime departureTime) {
      super(SeverityLevel.ERROR);
      this.csvRowNumber = csvRowNumber;
      this.prevCsvRowNumber = prevCsvRowNumber;
      this.tripId = tripId;
      this.departureTime = departureTime;
      this.arrivalTime = arrivalTime;
    }
  }

  /**
   * Missing `stop_times.arrival_time` or `stop_times.departure_time`
   *
   * <p>Severity: {@code SeverityLevel.ERROR}
   */
  @GtfsValidationNotice(severity = ERROR, files = @FileRefs(GtfsStopTimeSchema.class))
  static class StopTimeWithOnlyArrivalOrDepartureTimeNotice extends ValidationNotice {

    /** The row number of the faulty record. */
    private final int csvRowNumber;

    /** The trip_id associated to the faulty record. */
    private final String tripId;

    /** The sequence of the faulty stop. */
    private final int stopSequence;

    /** Either `arrival_time` or `departure_time` */
    private final String specifiedField;

    StopTimeWithOnlyArrivalOrDepartureTimeNotice(
        int csvRowNumber, String tripId, int stopSequence, String specifiedField) {
      super(SeverityLevel.ERROR);
      this.csvRowNumber = csvRowNumber;
      this.tripId = tripId;
      this.stopSequence = stopSequence;
      this.specifiedField = specifiedField;
    }
  }
}
