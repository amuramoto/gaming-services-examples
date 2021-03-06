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
package com.google.maps.gaming.zoinkies.models;

import com.google.maps.gaming.zoinkies.ITEMS;
import java.util.ArrayList;
import java.util.List;

/**
 * Types of reference data
 * - Location Objects
 * - Collectibles
 * - Weapons
 * - Armors
 * - Characters
 */
public class ReferenceData {

  /**
   * Lists to all references for this game. Every game object has a reference in this list.
   * The list is reconciled between client and server.
   */
  private List<ReferenceItem> references;

  /**
   * Getter for references
   * @return
   */
  public List<ReferenceItem> getReferences() {
    return references;
  }

  /**
   * Setter for references
   * @param references
   */
  public void setReferences(List<ReferenceItem> references) {
    this.references = references;
  }

  public ReferenceData() {
    references = new ArrayList<>();
  }

  /**
   * Returns the reference item identified by the given id
   * @param id the unique identifier for this item
   * @return a ReferenceItem
   */
  public ReferenceItem getReferenceItem(ITEMS id) {
    if (id == null) {
      return null;
    }
    for(ReferenceItem ri:getReferences()) {
      if (ri.getItemId().equals(id)) {
        return ri;
      }
    }
    return null;
  }

  /**
   * Outputs all reference data
   * @return
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (ReferenceItem ri : references) {
      sb.append(ri.toString());
      sb.append("\n");
    }
    return sb.toString();
  }
}
