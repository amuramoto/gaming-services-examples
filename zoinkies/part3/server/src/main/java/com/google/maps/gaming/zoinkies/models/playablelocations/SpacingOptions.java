/**
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.maps.gaming.zoinkies.models.playablelocations;

/**
 * POJO classes to map the json request / response to the playable locations REST API.
 * @see https://developers.google.com/maps/documentation/gaming/reference/playable_locations/rest
 *
 */
public class SpacingOptions {
  /**
   * The minimum spacing between two locations in meters
   */
  private int min_spacing_meters;

  /**
   * Getter for min_spacing_meters
   * @return
   */
  public int getMin_spacing_meters() {
    return min_spacing_meters;
  }

  /**
   * Setter for min_spacing_meters
   * @param min_spacing_meters
   */
  public void setMin_spacing_meters(int min_spacing_meters) {
    this.min_spacing_meters = min_spacing_meters;
  }

  /**
   * The point type
   */
  private PointType pointType;

  /**
   * Getter for pointType
   * @return
   */
  public PointType getPointType() {
    return pointType;
  }

  /**
   * Setter for pointType
   * @param pointType
   */
  public void setPointType(
      PointType pointType) {
    this.pointType = pointType;
  }
}
